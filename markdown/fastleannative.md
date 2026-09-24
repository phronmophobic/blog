

Posted: September 23rd, 2026
<!-- Updated -->


# Rationale

Working with native libraries and off-heap memory has historically been awkward and cumbersome from Clojure. Recently, there has been an increasing number of powerful tools for working within and alongside native libraries written in c, c++, rust, and more. These tools enable Clojure developers to improve startup time, lower CPU and memory usage, create standalone binaries, call native libraries, manipulate off-heap memory, reach new platforms, and create native libraries for embedding Clojure in non JVM applications.

This reference serves as a companion guide to a workshop I'm giving at the Clojure Conj: [Fast, Lean, Native Clojure](https://2026.clojure-conj.org/workshops). The tools and technologies for letting Clojure developers leverage the native ecosystem are improving quickly. The goal for this document is to cover the fundamental concepts behind these tools so that readers can apply them regardless of which technologies they end up choosing.

{{table-of-contents/}}
 

# The C ABI

C doesn't have an ABI. Technically. However, that technicality doesn't matter in practice. In practice, the C ABI is _the_ most common way  for different languages and runtimes to talk to each other or to the underlying operating system.

Calling Rust from C++? Use the C ABI.
Calling C++ from Python? Use the C ABI.
Calling Java from Ruby? Use the C ABI.

As there is no official C ABI, there is also no official definition. For our purposes, we will view the C ABI as the contract for calling functions that accept and return C datatypes.

## C ABI datatypes

The main datatypes are:
- _Bool/bool (typically 1 byte in size)
- integral numbers (eg. char, short, int, long, long long)
- floating point numbers (eg. float, double, long double)
- enum (typically shares the same size as int)
- pointer
- struct
- array
- union

A library that exposes a C ABI compatible interface will define an API of functions. Each function will have a name that follows c naming conventions. The function will accept arguments specified by c data types and will return a value with a specified c datatype.

## Example function, `cos`

As an example, here is the definition of `cos` in c:

```c
double cos(double x);
```

This function is called `cos`. It takes a single double as an argument and returns a double.

## Structs

Structs are like quirky maps. Here's an example struct in c ([source](https://github.com/tree-sitter/tree-sitter/blob/659cda7c7f86ebe31cc825dc5da59e9add172dc7/lib/include/tree_sitter/api.h#L77)):

```
typedef struct TSPoint {
  uint32_t row;
  uint32_t column;
} TSPoint;
```

Structs have fields. Each field has a type and a name. Field types can be c data types like int, float, pointer, and even other structs. In addition to names and types, the order and type of fields also specifies how the structure will be laid out in memory. This is important for reading and writing data that is provided or received from a native library.

### Pass by Reference vs. Pass by Value 

When working with structs, you may hear phrases like "passed by reference" or "passed by value". Using this parlance, a reference is just a pointer to the memory of a struct. In practice, that means that reads or writes to a struct by reference (ie. a pointer to a struct) will all be operating on the same shared struct.

When a struct is "passed by value", that means the memory of the entire struct is copied. Changes to the copy will not affect the original. However, it is worth emphasizing an important caveat. Passing a struct by value is not a deep copy. That means if the struct has a field which is a pointer to another struct, then the copy of the struct will also have a field which points to the same struct as its copy.

Let's look at an example:

```c
// structexample.c
#include <stdio.h>

typedef struct NumStruct {
    int num;
} NumStruct;

typedef struct StructWithStruct {
    NumStruct* numStruct;
    int num;
} StructWithStruct;

void incByValue(StructWithStruct s){
    s.num++;
    s.numStruct->num++;
    
}

int main(int argc, char** argv){

    NumStruct ns;
    ns.num = 42;

    StructWithStruct a;
    a.num = 42;
    a.numStruct = &ns;

    incByValue(a);

    printf("nums are %d, %d, %d\n", a.num, a.numStruct->num, ns.num);
    

}
```
This will print the following:
```sh
$ gcc structexample.c -o structexample
$ ./structexample
nums are 42, 43, 43
```

As you can see, the value of `a.num` was not altered by `incByValue`, but `a.numStruct->num` was altered. If it doesn't make sense initially, that's ok. If you're not familiar with `c`, this can be a very difficult concept to grok. However, understanding the difference between "pass by reference" and "pass by value" is very important for writing correct code.

_Note: passing by reference and passing by value also have different performance characteristics, which are out of scope for this reference._

## Strings

The c ABI does not have a proper string type. However, most native libraries typically use one of the two following strategies.

1. "c strings" - pointer to null terminated byte encoding
2. a struct with a data pointer, a length, and possibly an encoding

### C Strings (null terminated)

C strings are a common way to represent strings in native libraries. C strings are just a pointer to some bytes followed by a null terminator. A null terminator is just the zero byte value. This representation has fallen out of favor since errors can lead to memory corruption and security exploits. Additionally, calculating the length of a string is O(n), which can be inefficient for many use cases.

The size of a c string is the length of the string plus one (for the null byte). The encoding for the string is often implicit. Usually, ascii is supported and utf-8 is tolerated.

### Representing strings with structs

Due to the problems with c strings, many c libraries use a struct to represent string types. Unfortunately, since there is no standard string representation, each library needs to reinvent the string representation. Most string representations have a pointer to the string data and the length. 

Some libraries provide utility functions that work with their string type. Some don't.
Some libraries allow for multiple string encodings. Some don't.
Some libraries use separate types for different string encodings. Some don't.

As an example, here is the definition for the string representation found in [webgpu.h](https://github.com/webgpu-native/webgpu-headers/blob/673658bc2bd70ec39fc55ebe6bb0173cf6d0a603/webgpu.h#L193).

```c
/**
 * Nullable value defining a pointer+length view into a UTF-8 encoded string.
 *
 * Values passed into the API may use the special length value @ref WGPU_STRLEN
 * to indicate a null-terminated string.
 * Non-null values passed out of the API (for example as callback arguments)
 * always provide an explicit length and **may or may not be null-terminated**.
 *
 * Some inputs to the API accept null values. Those which do not accept null
 * values "default" to the empty string when null values are passed.
 *
 * Values are encoded as follows:
 * - `{NULL, WGPU_STRLEN}`: the null value.
 * - `{non_null_pointer, WGPU_STRLEN}`: a null-terminated string view.
 * - `{any, 0}`: the empty string.
 * - `{NULL, non_zero_length}`: not allowed (null dereference).
 * - `{non_null_pointer, non_zero_length}`: an explictly-sized string view with
 *   size `non_zero_length` (in bytes).
 *
 * For info on how this is used in various places, see \ref Strings.
 */
typedef struct WGPUStringView {
    WGPU_NULLABLE char const * data;
    size_t length;
} WGPUStringView WGPU_STRUCTURE_ATTRIBUTE;
```

As you can see, it has a pointer to the contents of the string and a field that can (but might not) specify the length of the string. Notice all the special rules required to represent a string that only apply to this specific library.

## Callbacks

Some native functions accept callbacks, aka function pointers, or upcalls. This allows native functions to invoke functions in the higher level language. Just to give a flavor why this is useful, let's take a look at a few examples.

### glfw example

Here is an example of a callback that can be passed to glfw to receive [key events](https://www.glfw.org/docs/latest/input_guide.html#input_key).


```c
// callback type definition
typedef void(* GLFWkeyfun) (GLFWwindow *window, int key, int scancode, int action, int mods);


// example usage
void key_callback(GLFWwindow* window, int key, int scancode, int action, int mods)
{
    if (key == GLFW_KEY_E && action == GLFW_PRESS)
        activate_airship();
}

void init(GLFWwindow* window){
    ...
    glfwSetKeyCallback(window, key_callback);
    ...
}
```

### qsort example
A simple example is qsort using [babashka.ffi](https://github.com/babashka/ffi):

From the babashka.ffi docs:
```clojure

(with-open [arena (ffi/confined-arena)]
  (let [comparator
        (ffi/callback
         arena
         (fn [left-pointer right-pointer]
           (compare (ffi/read (ffi/reinterpret left-pointer 4) :int)
                    (ffi/read (ffi/reinterpret right-pointer 4) :int)))
         [:pointer :pointer]
         :int)]
    (qsort values 5 4 comparator)))
```

Here, the example is calling `qsort`, which will sort an array. The last argument to `qsort` is a callback which acts as a comparator. The callback accepts two arguments, pointers to the items to compare. The callback then returns an integer less than, equal to, or greater than zero to signify if the first item is less than, equal to, or greater than the second item.




## Variadic Functions

While rare for most library APIs, C ABI functions may be variadic (ie. accept different numbers of arguments). The canonical example is `printf`. Below is the C definition for `printf` and an example of its usage:

```c

int printf(const char * restrict format, ...);

printf("I can take %d or %d or more arguments!\n", 1, 2);
```



# GraalVM Native Image

Native Image compiles Java bytecode to standalone executables and native libraries. This allows for faster startup, improved performance, smaller, self-contained deployment artifacts, and lower memory usage.

However, there are a few tradeoffs to be aware of:
- No bytecode or class generation
- No eval (due to lack of bytecode and class generation)
- No Runtime reflection without explicit configuration
- Increased compile times
- Not all libraries are native image compatible



    

<!-- 

  - creating native libraries
    https://www.graalvm.org/latest/reference-manual/native-image/native-code-interoperability/JNIInvocationAPI/
https://www.graalvm.org/sdk/javadoc/org/graalvm/nativeimage/c/function/CEntryPoint.html

  - creating executables
  - SCI
  - config
    - reflection
    - jni
    - resource

## Creating Native Libraries
-->



# The Foreign Function and Memory API

The Foreign Function and Memory API, aka FFM, previously aka Project Panama is the Java API for interacting with native libraries. It is available in JDK 22+ (there are preview releases prior to that, which we will ignore). As described in the [docs](https://docs.oracle.com/en/java/javase/22/core/foreign-function-and-memory-api.html):

> The Foreign Function and Memory (FFM) API enables Java programs to interoperate with code and data outside the Java runtime. This API enables Java programs to call native libraries and process native data without the brittleness and danger of JNI. The API invokes foreign functions, code outside the JVM, and safely accesses foreign memory, memory not managed by the JVM. 

Since there are many excellent resources for learning about the FFM APIs, we will only cover a few Clojure specific topics.

## `invokeExact` Inexpressible in Clojure

Most Java example code for FFM uses [MethodHandle/.invokeExact](https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/invoke/MethodHandle.html#invokeExact(java.lang.Object...)). However, `invokeExact` has a polymorphic signature which is [currently inexpressible](https://clojure.atlassian.net/browse/CLJ-2921) using Clojure's Java interop syntax. As a workaround, `invokeWithArguments` can be substituted. Unfortunately, `invokeWithArguments` is less performant. Fortunately, the Clojure wrapper libraries for FFM use bytecode generation to allow usage from Clojure to be both fast and idiomatic.

## Clojure FFM compatible wrappers

- [dtype next](https://cnuernber.github.io/dtype-next/tech.v3.datatype.ffi.html)
- [coffi](https://github.com/IGJoshua/coffi/)
- [babashka.ffi](https://github.com/babashka/ffi)

For a comparison of ffi libraries, see this google docs [spreadsheet](https://docs.google.com/spreadsheets/u/1/d/e/2PACX-1vQAiX80h3wsbwo7qv8aAOp2TFLO6V2dJV5Ay24xihhKObDhT7HwS0nbZGUPxLjaJc9rSwoN-tNksFda/pubhtml#gid=1519410866).


# Packaging Libraries that use Native Code

A challenge when wrapping native libraries is that the native library also needs to be provided or exist on the target system. The most common methods for distributing native code are standalone executables and dynamic libraries. While it is possible to make a custom build of the Java runtime that statically links a particular native library, it is fairly uncommon compared to bundling a shared library that can be loaded at runtime. As such, we will only cover loading dynamic libraries at runtime and producing standalone executables.

## Packaging Dynamic Libraries

Dynamic libraries (aka. shared libraries) are files that contain code targeting a particular operating system and architecture. The actual implementation for loading dynamic libraries is provided by the particular operating system (eg. `dlopen`). However, the OS specific function calls are typically abstracted over by ffi libraries. The typical workflow for loading a dynamic library is:

1. Load the library
2. Lookup the address of symbol for the exported function
3. Wrap the function address as a function call by providing the types of the arguments and return value.


Here's a basic example of loading a shared library using the Java's Foreign Function and Memory API in Java.


```java
 Linker linker = Linker.nativeLinker();
 SymbolLookup stdlib = linker.defaultLookup();
 MethodHandle strlen = linker.downcallHandle(
     stdlib.find("strlen").orElseThrow(),
     FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
 );

 try (Arena arena = Arena.ofConfined()) {
     MemorySegment cString = arena.allocateFrom("Hello");
     long len = (long)strlen.invokeExact(cString); // 5
 }
```

And here it is translated to Clojure (with a little extra ceremony to accommodate the Java interop).

```clojure
(import
 '(java.lang.foreign Linker SymbolLookup FunctionDescriptor ValueLayout Arena MemorySegment)
 '(java.lang.invoke MethodHandle))

;; If the dynamic library is not already loaded
;; (System/loadLibrary "mylib")

(def linker (Linker/nativeLinker))

(defn strlen [^String s]
  (let [^MethodHandle strlen
        (Linker/.downcallHandle
         linker
         (.orElseThrow (.find (.defaultLookup linker) "strlen"))
         (FunctionDescriptor/of ValueLayout/JAVA_LONG
                                (into-array java.lang.foreign.MemoryLayout
                                            [ValueLayout/ADDRESS]))
         (into-array java.lang.foreign.Linker$Option []))]
    (with-open [^Arena arena (Arena/ofConfined)]
      (let [^MemorySegment cstr (.allocateFrom arena s)]
        (MethodHandle/.invokeWithArguments strlen (object-array [cstr]))))))

(strlen "Hello") ;=> 5

```

Let's now take a closer look at each step.

## Load the library

To load a library, it must be available on the file system{{footnote}}I think it's technically possible to load a shared library from memory, but I've never heard of this approach being applied in practice since it's usually easier to just extract a bundled library to the file system somewhere and then load it{{/footnote}}. That means if your shared library is bundled as a program resource (eg. in a jar), then you must extract the shared library to the file system somewhere. 

### Loading Libraries From The System

One common approach to loading libraries is instead of providing the shared library as part of your software, you require the user to make the library available on the library search path. This approach can work well if the library is commonly available. This also allows users to use a package manager that they are comfortable with to install and update the necessary native libraries. Using system libraries also sidesteps the issues of how to build and link any dependencies that the shared library itself needs.

Pros:
- Avoid multiple copies of the same shared library when multiple libraries have the same dependency
- No CI step for packaging native dependencies
- Give users more control over native dependencies (eg. build flags, versions)
- Smaller jar sizes

Cons:
- Not all dependencies are captured by the library maven coordinate (or equivalent)
- Potential version compatibility issues
- Requires extra steps for the user to use your library

### FFI loaders


Some ffi libraries provide helpers for packaging shared libraries in jars and extracting them so they can be loaded (eg. javacpp, JNA), but some do not. If your ffi library doesn't help package native dependencies, you can also rely on a separate ffi library just for its loading utilities. For example, [javacpp-presets](https://github.com/bytedeco/javacpp-presets/) provides packages with the native dependencies for libraries like llvm and ffmpeg. These native dependencies can be a pain to package, so you can use the javacpp loader for these packages, even if you don't want to use the Java wrappers generated by javacpp.


### Custom Packaging

Since dynamic library loading is implemented by the operating system, separate binaries are required for every combination of architecture and operating system that is supported.

When packaging native dependency, I recommend having separate dependencies for:
- each operating system, architecture combination that you target
- the Clojure source for the native wrapper without any native dependencies
- an additional coordinate with the Clojure source and binaries for all target systems

This allows consumers of your library to decide if they want to optimize for convenience or bundle size. It also allows the user to build native dependencies themselves.

In practice, it is possible to build shared libraries targeting Mac OSX and Windows that can work across a wide variety of systems and versions. You still need separate binaries for each architecture, but the same shared library will usually work on most systems running the same OS and architecture.

Linux, on the other hand, is not as straightforward. If you build a shared library targeting Ubuntu, it may or may not work on other distributions, even if the architecture is the same. It may not even work across versions of Ubuntu. In general, you will get the best results if you build the shared library on a very similar system to the target system. Good luck!

### Custom Loading

If your ffi library doesn't provide helpers for packaging native dependencies, you can roll your own. If you write your own packaging method, you have to decide where to extract native dependencies onto the file system. Common approaches are to either extract the libraries to a temporary folder or to a common location so that extraction doesn't need to rerun across multiple invocations of your program.

## Static

One way to bundle a native library is to statically link the library as part of the distributed executable. This can be especially attractive for builds that can produce standalone binaries (eg. native image, jank). While it is possible to build your own Java runtime distribution that bundles your app and native dependencies, it's less common. When targeting the JVM, using dynamic libraries is generally the preferred approach.

<!--

  - packaging
    - building for clojars
    - CI
    - cosmopolitanm
  - libpython-clj

-->


## Address Lookup

Typically, finding the address for a function is fairly straightforward. The symbol for a function is just the name of the function. The only interesting thing to note is that symbol names are typically global across all shared libraries. When doing symbol lookup, it is possible to scope the symbol lookup to a particular library. However, since symbols are generally global, most libraries are fairly good about prefixing all function names with a relatively unique prefix so scoped lookup isn't typically necessary. However, scoped lookup can be useful in some use cases, so it's nice to have as an option.

## Wrapping Native Functions


After loading a library, you're still left with the task of making the native function accessible to your Clojure code. How you wrap the native library is generally where most of the challenges lie.

### Specifying Types

To call a native function without crashing your program requires knowing the types that the function accepts as well as the type of the return value. You can find the necessary types by checking the function signatures in the header file(s) provided by the native library.

Before we discuss my preferred approach, I've asked around and folks have said that using LLMs to guess the correct types seems to work and they haven't noticed any obvious bugs when using LLMs to generate function type signatures. It is worth noting that specifying incorrect types for functions may not immediately crash and may instead cause silent memory corruption that doesn't manifest until some later point.

Some other alternatives code generators that are not covered here are javacpp and jextract.

#### Generating function type signatures with Clong

Rather than guessing the types for function calls, the correct types can be explicitly derived from the header files. To make this easier, I've written library called [clong](https://github.com/phronmophobic/clong) that wraps libclang to extract all the necessary information for wrapping native calls. As a bonus, it also extracts doc strings, enums, and struct layouts.

The general process of writing a native wrapper with clong usually follows these steps:
1. Parse header files into a description of the native API as data
2. Generate code from the api data



##### API as Data

For many common use cases, extracting the API as data is straightforward.

```clojure
(require '[com.phronemophobic.clong.clang :as clang])

(def api (clang/easy-api "/opt/local/include/zlib.h"))
```

The api data will contain three keys, `:functions`, `:structs`, and `:enums`. These data have all the required information to generate code wrappers.

Many native libraries will provide a single header with the full API, but some libraries have a more extensive API (eg. ffmpeg). In those cases `clang/easy-api` allows you to pass all the arguments you would normally pass to the clang cli.

Extracting the api data requires libclang. However, in most cases, the API data can be extracted at compile time or dev time so that the libclang dependencies are required by users of your library.

##### FFI code generation

There are multiple different FFI libraries. Separating the datafication of the API from the code generation allows clong to support multiple ffi libraries as targets. Additionally, many native libraries rely on conventions for correct usage. As an example, many libraries have a convention where all functions with "create" return a resource that needs to be freed. Since the API interface is available as data, generating code that correctly and consistently implements these conventions is easy.

Clong currently supports code generators for JNA and dtype-next. The dtype-next library has an extensible FFI implementation that supports multiple ffi targets including Java's FFM, GraalVM native-image, and JNA.

##### Ergonomic wrappers

Clong's code generation deals with basic datatypes that correspond to the data types found in the C ABI (eg. char, float, int, long, pointer, struct, etc). In most cases, an idiomatic Clojure API will want to provide a higher level interface and deal with more ergonomic data types (eg. strings). It's tempting to want to implement the higher level API directly in the code that wraps the native functions. However, I've found that a much saner approach is to generate code that wraps the C API using a very direct approach and building the higher level API on top of it.


# Thread Safety

The JVM memory model does a lot of work to enforce a coherent memory model for building multi-threaded applications. The JDK does provide some guarantees for off-heap memory (eg. [Arena](https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/Arena.html)). However, these guarantees do not apply to memory allocated by native libraries. Most native libraries use mutation pervasively and are not thread safe. Using native libraries safely in a multi-threaded context is currently beyond the scope of this reference. Good luck!




# Memory Management

When interacting with native libraries, memory management is a problem you will have to solve. Fortunately, there are many utilities and techniques that can help.

## Leveraging the Garbage Collector

Even though you may be calling native code that expects manual memory management, you can often leverage the garbage collector to do your dirty work and clean up resources for you.


On the JVM, you can use the [java.lang.foreign.Arena/ofAuto](https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/Arena.html#ofAuto()) to keep track of MemorySegments for you. However, the auto arena (or the other arenas) can only help with memory that you allocate.

Another technique is to use [java.lang.ref.Cleaner](https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/ref/Cleaner.html). Cleaners allow you to register a function to be called when an object becomes phantom reachable (ie. ready to be collected). This allows you to either free associated resources or decrement a reference count in a managed way. Just be sure that your cleaning function doesn't reference your object! Even if your context doesn't have access to Java's Cleaner (eg. the context isn't JVM based), there may be a similar alternative.

Warning! The garbage collector will probably not release resources promptly. In many cases, that is a good thing because it increases efficiency. Waiting for a handle to be collected to free resources may take a while. Further, the garbage collector does not necessarily know the size of the resource referenced by a handle. The handle itself may only be a few bytes, but may point to a very large object in memory.

As a library author, it's usually good practice to allow library users to explicitly free resources even if having resources managed by the garbage collector is also an option. One good way to make a resource explicitly freeable is to have the handle implement `java.lang.AutoCloseable`. Implementing AutoCloseable allows your resources to seamlessly interoperate with existing code that knows how to work with AutoCloseable objects (like `with-open`).

### Hanging onto References

If a chunk of memory managed by the garbage collector is passed to native code, you must make sure to hang on to the reference until the native code will no longer need access. Otherwise, the garbage collector may unpredictably free the resource before the native code is done using the resource. This may crash your program or worse.

### Overzealous Allocations

As long as your program is allocating memory faster than the garbage collector can clean up garbage, the JVM does a decent job of managing the total memory used by the program. The total memory used can be further constrained via system parameters. However, when allocating memory off-heap, the garbage collector usually only knows about the sizes of references or resource handles and doesn't know about the sizes of the resources that are off-heap. If you don't manually free large resources, the garbage collector may not prioritize cleaning unreachable handles to large resources. This can cause the total memory usage of the program to balloon. Even if you do clean up the resources eventually, simply freeing the resources does not typically cause the total memory used by the program to decrease. To prevent memory usage from climbing out of control, it may be necessary to take a more hands on approach to allocating and freeing resources rather than relying on the garbage collector.

### Never Free

Another approach to memory management is to never free resources. While this sounds irresponsible, all resources associated with a process are cleaned up by the operating system when a process exits. For short lived programs, never freeing memory can be both fast and efficient.

However, it should be noted that while never freeing memory may make sense for applications, it is bad practice for libraries. Library authors should almost always provide explicit mechanisms for cleaning up resources.

Never freeing memory also doesn't need to be the only approach in an application. For resources that are required for the full lifetime of a program, keeping track of references may be unnecessary work. In those cases, global storage (eg. Arena/global) can be used for references that don't need to be freed for the life of a program, while other resources are tracked.

## String Memory Management

Since the data for strings is usually dynamically allocated, each library also needs to develop a strategy for releasing memory used for strings. Some libraries use reference counting. Some libraries use ownership models. Unfortunately, this can be a bit of a pain point when working with native libraries.

# C ABI upgrade compatibility

As with any library, native libraries can grow and change. The API and data structures defined by the library API can also change over time. Some changes are backwards compatible which allow libraries that wrap an old version of the API to continue to work with new versions without changing the wrapper. Some changes to a native API can lead to crashes, bugs, security vulnerabilities, undefined behavior or worse when accessed by a wrapper targeting a different version of the API.

One reason I like to use [clong](https://github.com/phronmophobic/clong) to generate wrappers for native code is that the datafied API allows for comparison between versions. When an API changes, you can use clong to generate a data representation of the API and compare the old API with the new API. This not only speeds up the process of updating the generated wrapper code, but also provides tools to reason about the changes.

## Breaking changes in the C ABI

An incomplete list of breaking changes for native APIs:
- Removing functions
- Adding or removing arguments to functions
- Changing the data type of a function argument or return value

Removing functions and adding/removing arguments should be self-explanatory, but what it means to change the datatype of an argument or return value can be a bit tricky. The main reason is that many C apis accept or return pointers to structs. An API can make some changes to the layout of structs passed by reference without breaking the API (some changes will still be breaking).

### Struct Layout changes and compatibility

If a struct is passed by reference, it is possible to change the struct layout without breaking compatibility. However, most changes are breaking, so any changes to struct layout that are intended to be backwards compatible must be carefully considered.

#### Initialization

One precondition for changing the layout of a struct in a backwards compatible way is that there must be some mechanism for ensuring that enough space is provided for the full struct when the struct is allocated. In practice, that usually means that the API provides a function to create or initialize structs rather than allowing the caller to allocate a struct. Theoretically, an API could provide some way for the caller to know how much space to allocate and how to initialize the memory, but I don't think I've ever seen that approach in practice.

#### Adding Fields to Structs

In addition to ensuring that structs are initialized correctly, the layout of the struct must not change the offset of any struct field that should be accessed as part of using the API. In general, that usually means the only common type of change to struct layouts that is backwards compatible is adding new fields to structs. Importantly, existing fields of a struct should not be rearranged and the types for each field should remain unchanged. Theoretically, you can make other changes to layout using unions or conditionalizing on a field that specifies the type or version of a struct, but that is uncommon.

# Other Native Languages Besides C

In general, there aren't convenient mechanisms for calling functions from other native languages like Rust or C++ other than going through the C ABI. In practice, that means that calling functions from other native languages requires writing a C ABI compatible interface without language specific features (and nonsense). As an example, a C++ library that exposes a C ABI compatible API cannot expose templates, macros, C++ classes, constructors, destructors, operators, etc. The same idea applies to rust, swift, and others.

## C++

If you are interested in accessing C++ from Clojure, you may be interested in [jank](https://jank-lang.org/).

Accessing C++ from the JVM isn't free. If a C ABI interface doesn't exist, then one must be created to use C++ functions from the JVM. One tool that may help is [javacpp](https://github.com/bytedeco/javacpp). If you're lucky, then there may already be a wrapper available in [javacpp-presets](https://github.com/bytedeco/javacpp-presets/).


## Rust

Similar to C++, Rust code cannot be directly called from the JVM without a C ABI compatible wrapper. If a C ABI compatible wrapper does not already exist, then [cbindgen](https://github.com/mozilla/cbindgen) may help.

<!-- 
# Working with Off-Heap Memory

## Dtype next
### basic usage
native-buffer
make-container
pointers
tracking resources
structs
callbacks
more ergomic wrappers with coercion

# Error handling

# Mobile

# Benchmarks

numbers every programmer should know

# Web Assembly

--->
# GPUs

Despite the amount of software running on GPUs these days, GPU programming is still a bit of a mess. There are several APIs that target GPUs: OpenGL, Vulkan, CUDA, Metal, and DirectX. Each API has its strengths, weaknesses, availability, and shader language. Most options are limited to specific platforms. Cross platform options like Vulkan have a steep learning curve (see this +1,000 line example for drawing a [triangle](https://github.com/KhronosGroup/Vulkan-Samples/blob/177edebf0cd7d4f669667e49f052cfb56b17e004/samples/api/hello_triangle/hello_triangle.cpp)).

Each API has its own shader language, which can make reusing code difficult. Further, most shader languages have poor support for code reuse so many libraries that include shaders use a custom dialect that is non-portable.

There are higher level libraries that expose a higher level interface so that users don't have to write shaders. Due to the complexity of the underlying system, these higher level libraries often have to make tough choices about which languages, runtimes, and hardware they support. Especially for graphics, many higher level libraries still require writing shaders regardless.

<!-- 
## Glossary

arena
c string
downcall - calling a native function from a high level language
upcall - calling a function in a high level language from a native function
struct
C ABI - contract for calling functions that accept and return C datatypes
pass by reference
pass by value
ffi - Foreign Function Interface

## Resources


#GraalVM on clojurians slack
https://github.com/clj-easy/graal-docs
https://github.com/clj-easy/graalvm-clojure

https://thephd.dev/to-save-c-we-must-save-abi-fixing-c-function-abi
https://docs.oracle.com/en/java/javase/22/docs/api/java.base/java/lang/foreign/Linker.html#variadic-funcs

-->

# Footnotes

{{footnotes/}}

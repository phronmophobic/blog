{{table-of-contents/}}

Posted: December 7, 2025

A quine is a program that outputs its own source. It's easy to view quines as these special creatures, crafted only for the purpose of reproducing themselves. However, once you understand the trick that makes quines work, it's fairly trivial to turn _any_ program into a quine. I'm not sure if I never realized that any program could be turned into a quine because I didn't understand how quines worked or just because I've never seen quines that do anything else besides reproduce themselves. To be fair, most programs don't benefit from adding a giant chunk of code to reproduce themselves.

## How to Make a Quine

### Self Insert

To understand how to make a quine, we will first start with a simple function that takes a string (presumably some source code) and inserts a quoted version of itself in the middle.

```clojure
(require '[clojure.java.io :as io]
         '[clojure.string :as str])

(defn ^:private escape
  "Add slashes before double quotes and slashes."
  [s]
  (str/replace s #"([\"\\])" "\\\\$1" ))
(defn ^:private quoted
  "Escape and surround `s` with double quotes."
  [s]
  (str "\"" (escape s) "\""))

(defn self-insert [beg end]
  (str beg (quoted beg)
       ;; this space isn't necessary,
       ;; but will make our examples more readable
       " "
       (quoted end) end))

```

The function of interest here is `self-insert`. It takes two strings and inserts a quoted copy of the two strings in the middle. Let's take a look at an example output.

```clojure
> (println (self-insert ":hello " " :world"))
:hello ":hello " " :world" :world
```

The output for the above example isn't very interesting, but as you can see, it duplicates the input and inserts the duplicate surrounded by quotes in the middle.

### Self Insert Function Call

Let's try a more interesting example.

```clojure
> (println (self-insert "(my-function " ")"))
(my-function "(my-function " ")")
```

Since the self insertion point is in the argument position of a function, the output is a function that receives its own quoted source as arguments. Despite some very innocent looking code, you might already get the sense that we're up to no good. With this trick, we were able to produce **a program that calls a function with its own source**.

### Self Insert with Print

Since a quine is a program that outputs its own source, a straightforward (but incorrect) attempt would be to try the above trick using `print`.

```clojure
> (println (self-insert "(print " ")"))
(print "(print " ")")
```

### Self Insert Quine

So close! If the source for our program is `(print "(print " ")")`, it's output would be `(print  )`. The original program received a copy of its input, but the output of the original program does not receive a copy of its input. However, we know a trick for that...

```clojure
> (println (self-insert "(println (self-insert " "))"))
(println (self-insert "(println (self-insert " "))"))
```

We did it! We made a quine!

### Self Insert Quine with `self-insert`

Unfortunately, it feels a bit like we cheated. Clearly, the `self-insert` function is doing the majority of the work here, but its source is not included in the quine. However, you may notice that you can add source to the beginning or end of the program without anything breaking. Let's try a simple example of prepending 42 to our quine just to see what it looks like.

```clojure
> 42 (println (self-insert "42 (println (self-insert " "))"))
42 (println (self-insert "42 (println (self-insert " "))"))
```

Notice how we added 42 to both the part of the program that will be evaluated as well as the quoted copy. Also note that the quine is still a quine, even though we added a bit of junk to the beginning of our program.

Ok, now let's try making the full quine with `self-insert` included. The only change to our smaller quine is that we prepended the source necessary for `self-insert`. Just as above, we will prepend the code for `self-insert` to both the quoted and unquoted copies of the source.

```clojure
(require '[clojure.java.io :as io]
         '[clojure.string :as str])

(defn ^:private escape
  "Add slashes before double quotes and slashes."
  [s]
  (str/replace s #"([\"\\])" "\\\\$1" ))
(defn ^:private quoted
  "Escape and surround `s` with double quotes."
  [s]
  (str "\"" (escape s) "\""))

(defn self-insert [beg end]
  (str beg (quoted beg)
       ;; this space isn't necessary,
       ;; but will make our examples more readable
       " "
       (quoted end) end))

(print (self-insert "(require '[clojure.java.io :as io]
         '[clojure.string :as str])

(defn ^:private escape
  \"Add slashes before double quotes and slashes.\"
  [s]
  (str/replace s #\"([\\\"\\\\])\" \"\\\\\\\\$1\" ))
(defn ^:private quoted
  \"Escape and surround `s` with double quotes.\"
  [s]
  (str \"\\\"\" (escape s) \"\\\"\"))

(defn self-insert [beg end]
  (str beg (quoted beg)
       ;; this space isn't necessary,
       ;; but will make our examples more readable
       \" \"
       (quoted end) end))

(print (self-insert " "))
"))
```

The above is a quine with all of the interesting bits included.

### Generalized Quining

At the very beginning of the post, I said that we can turn _any_ program into a quine. Directly above, we used the fact that we can add any amount of code to the beginning or end of our program to include `self-insert` as part of quine. Likewise, we can prepend source code that does non quine related things. In fact, we can generalize the recipe for creating a quine by taking our "base quine" and prepending or appending any source we want. See the code on github, [quineize](https://github.com/phronmophobic/quineize).

### Self Reflection

Hopefully, the above explanation helps demystify the quine, if only a bit. However, seeing all the bits and even understanding the individual bits may help, but it's not always enough to understand the project as a whole. Now that we know _how_ a quine works, I'd like to spend a brief moment trying to give an intuition for _why_ our quine works. To try to understand our quine construction, let's return back to the `self-insert` we started with.

```clojure
(defn ^:private escape
  "Add slashes before double quotes and slashes."
  [s]
  (str/replace s #"([\"\\])" "\\\\$1" ))
(defn ^:private quoted
  "Escape and surround `s` with double quotes."
  [s]
  (str "\"" (escape s) "\""))

(defn self-insert [beg end]
  (str beg (quoted beg)
       ;; this space isn't necessary,
       ;; but will make our examples more readable
       " "
       (quoted end) end))
```

The first observation to note is that `self-insert` creates a full copy of the source and that the extra copy is "quoted". "Quoted" in this context means to wrap the contents in double quotes (note: this is not the same as `clojure.core/quote`, although the ideas are related). The contents are also escaped to make a proper string in case the quoted content has double quotes or backslashes. The implementation for quoting is so trivial that it's easy to overlook that the concept of quoting is quite crucial to the whole enterprise. Quoted contents do not get immediately evaluated, but are saved for future use.

In essence, `self-insert` keeps the original source which will be evaluated and inserts a new copy that can be used for making future copies. At first, it can feel like a quine is materializing out of the void, but I think it's easier to think of a quine as two copies of a program. One copy to be evaluated and one copy that can be used to print the next program's source. Each time the full program is run, it runs one copy and uses the other unevaluated to copy to make two new copies (one copy to run and another to make future copies).

### Beginning and end

Another way to think about building a quine is to divide the program into the beginning and end. The beginning can't contain itself and similarly, the end can't contain itself. The beginning could contain the end, but then the end would have to also contain the beginning, which also doesn't work.

To solve this, you can introduce a middle section that contains the beginning and end. This is exactly what `self-insert` does. Crucially, the middle section must be fully derived from the beginning and end portions of the program source.

You may be wondering why `self-insert` takes two arguments instead of one. This will be left as an exercise for the reader. You can either try to reason it out or clone [quineize](https://github.com/phronmophobic/quineize) and play with `self-insert` yourself.

## Bonus: Deriving the Y Combinator

This idea of passing a copy of a program to itself is a powerful one. To better understand the idea, we'll implement another curiosity from computer science, the y combinator. The y combinator can be used to implement recursion without explicit recursive calls. To illustrate, let's see an example using the well known recursive function, `factorial`.

```clojure

(defn factorial* [f n]
  (if (pos? n)
    (* n (f (dec n)))
    1))

(def factorial (y-combinator factorial*))

(factorial 5)
;; 120

```

In this example, `factorial*` doesn't explicitly recur, but instead, it expects an argument, `f` that can be used to make recursive calls to `factorial*`. The actual factorial function is created by passing `factorial*` to the `y-combinator`. The `y-combinator` takes `factorial*` and fills in the first argument, `f`, so that `factorial*` can make recursive calls.

### Self Call

Similar to our quine example, we're going to start with a simple function. This time, it will be called `self-call`.

```clojure
(defmacro self-call [f]
  `(~f ~f))
```

Instead of a function that inserts a copy of its source as in our quine example, we're instead going to work with a function that receives a copy of itself. Let's see a simple example.

```clojure
> (macroexpand-1 '(self-call identity))
(identity identity)
```

Not a very interesting example, but it's a start. We will slowly work our way to the y combinator, but first we're going to try a slightly simpler example, writing a version of factorial without explicit recursion.





### Building Factorial without Explicit Recursion

Goal: **write a function that receives a copy of itself and returns the factorial function.**

We'll go step by step. As we go, we're going to leave "holes" to fill in later which we will denote with "`...`".

Given our goal, we know that our function will take a function as its argument, so let's start there.

```clojure
(def factorial
  (self-call
   (fn [f]
     ...)))
```

Next, we'll have our function return a function that takes an integer.

```clojure
(def factorial
  (self-call
   (fn [f]
     (fn [n]
       ...))))
```

Now, let's fill in a bit of the implementation of factorial.

```clojure
(def factorial
  (self-call
   (fn [f]
     (fn [n]
       (if (pos? n)
         (* n (... (dec n)))
         1)))))
```

Ok, now the tricky bit. The last hole to fill in is the "recursive" call to our factorial function. Where are we going to get our factorial function? Remember, by definition, we are writing "a function that receives a copy of itself and returns the factorial function." If we believe ourselves, then we should be able to get a factorial function by calling our input, `f`, with itself. Let's try.

```clojure
(def factorial
  (self-call
   (fn [f]
     (fn [n]
       (if (pos? n)
         (* n ((f f) (dec n)))
         1)))))

(factorial 3) ;; 6
(factorial 5) ;; 120
(factorial 9) ;; 362880
```

It worked! If you're like me, the fact that this actually works seems suspicious. Let's do a little more investigation to try to understand what the heck is actually happening. First, let's briefly take a look at the macroexpanded version.

```clojure
> (macroexpand-1 '(self-call
                   (fn [f]
                     (fn [n]
                       (if (pos? n)
                         (* n ((f f) (dec n)))
                         1)))))
((fn [f]
   (fn [n]
     (if (pos? n)
       (* n ((f f) (dec n)))
       1)))
 (fn [f]
   (fn [n]
     (if (pos? n)
       (* n ((f f) (dec n)))
       1))))
```
The function is indeed getting a copy of itself. It's possible to evaluate what happens step by step to verify that it does work, but I've personally never felt like that helps me understand why it works.

### Explanation

To understand what's happening here, let's think back to our quine example. In the quine example, we had two copies of our program, one to be evaluated and an extra quoted copy that could be used for making future copies. Likewise, in this example, we have a function that will be invoked now and a copy of the same function that can be invoked later to make future copies. Each recursive call invokes one copy and passes along another function that can be invoked later.

I do think the y combinator example is harder to grok than the quine example. Don't worry if it doesn't make much sense at first glance.

### Generalized Recursion: Y Combinator

Anyway, just like we could generalize the process of making quines so that any program could be turned into a quine, we can also generalize our solution for making factorial recursive so that we can make any function recursive.

Before we write the y combinator, let's first do some local variable renaming that will make our code more readable later.

```clojure
(def factorial
  (self-call
   (fn [make-recursive]
     (fn [n]
       (if (pos? n)
         (* n ((make-recursive make-recursive) (dec n)))
         1)))))
```

The only thing we changed is that we renamed `f` to `make-recursive`. Let's also remove the specifics of factorial.


```clojure
(self-call
 (fn [make-recursive]
   (fn [& args]
     ...)))
```

The above removed the factorial specifics and we changed the arguments of the returned function to be a generic list of args. Our y combinator function will take a function as an argument, so let's add that.

```clojure
(def y-combinator
  (fn [f]
    (self-call
     (fn [make-recursive]
       (fn [& args]
         ...)))))
```

Now, we'll have the returned function call the original function and pass the recursive function as its first argument.

```clojure
(def y-combinator
  (fn [f]
    (self-call
     (fn [make-recursive]
       (fn [& args]
         (apply f (make-recursive make-recursive) args))))))
```

That's it!

```clojure

(def y-combinator
  (fn [f]
    (self-call
     (fn [make-recursive]
       (fn [& args]
         (apply f (make-recursive make-recursive) args))))))

(defn factorial* [f n]
  (if (pos? n)
    (* n (f (dec n)))
    1))

(def factorial (y-combinator factorial*))

(factorial 3) ;; 6
(factorial 5) ;; 120
(factorial 9) ;; 362880

```

You may notice that our version of the y combinator looks slightly different than the version you'll probably find in textbooks and wikipedia. The main reason is that the textbook versions are usually in a context where every function is curried. However, it's conceptually important to note that the y combinator isn't unique, there are infinitely many! As an exercise, try to write `fibonacci` using `self-call`.

## Conclusion

Both quines and the y combinator are examples of programs that receive copies of themselves. It's easy to get lost in the mechanics of how these curiosities work rather than thinking about what it means for a program to receive a copy of itself. Once you realize what it means for a program to receive a copy of itself, new possibilities can emerge!



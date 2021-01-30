{{table-of-contents/}}

_This post is the third  in a series of posts explaining the design principles behind [membrane](https://github.com/phronmophobic/membrane), a cross platform library for building fully functional user interfaces in clojure(script)._

Previous posts: [What is a User Interface?](what-is-a-user-interface.html), [Implementing a Functional UI Model](ui-model.html)


<!-- Feedback is appreciated. Discuss on [reddit](https://www.reddit.com/r/Clojure/comments/kb8mbp/implementing_a_functional_ui_model/) or file an issue on [membrane's](https://github.com/phronmophobic/membrane/issues/new) github repo. -->

# Introduction

UI components in many frameworks couple state management and event handling. If we can untangle these two responsibilities, then we can make our components easier to test, reuse, debug, and reason about. Further, having simpler UI components will enable us to build improved tooling, which we'll cover in the next post.

Our strategy remains familiar. We'll try to build our UI components using only data and pure functions. We've already shown in [Part I](what-is-a-user-interface.html) and [Part II](ui-model.html) that the view and event functions can be pure. The missing piece is a plan for implementing "stateful" components.

# The Big Idea


In this post, we'll:
* Define what we mean by "UI component"
* Propose design constraints for building UI components and explain their reasoning

The summary of the design constraints is:

1. No side effects in view or event functions
2. Receive data only through function arguments
    * corollary: No hidden/local state and don't use global state
3. Use references (represented with immutable data) in event handlers to return intents that need to refer to input state or parts thereof.

<!-- Hopefully, everyone one of these points seems obvious, but violating some or all of these principles is common in user interface code. Even worse, many of our frameworks offer little help. -->


<!-- Just like `map`, `filter`, etc. were reimplemented for every context before transducers came around, we keep implementing UI components for every state management option. This isn't even a novel idea, <https://day8.github.io/re-frame/reusable-components/#implications>. -->


# What Makes a Checkbox Tick?

Our dumb ice breaker question for today is "What is a checkbox?". It's so obvious that it's difficult to put into words. Is a checkbox a fundamental UI block? Can it be broken down into smaller pieces? If so, what are the pieces that make up a checkbox?

# Effects

Before we can talk about state, we need to talk about how effects like updating state are implemented. Let's refer back to some of the definitions from [part 1](what-is-a-user-interface.html) of this series.

**Intent**: Data representing a user intent. Examples of user intents are "delete a todo list item", "open a document", "navigate to a URL".

**Event Function**: a pure function which receives the application state and an event and returns data specifying the user's intent (eg. add a new todo item to the todo list).

**Effect**: The carrying out of an intent.

Additionally, we'll add:

**Effect Handler**: An impure function that receives **intents** and should process these intents by affecting the world.

<!-- Common mistakes when designing an  -->
<!-- 1. All effect handlers are installed globally and it's not possible to mock, override, or ignore globally installed effect handlers. -->
<!-- 2. Encouraging effect handlers to access and modify global state. -->

As a refresher, membrane represents **intents** as a vector where the first value is the intent type, and the subsequent values are the arguments of the intent.



Below is a simple example of an effect handler that can process 2 types of intents:
1. `[:inc-counter]`: increment the current count
2. `[:get-count]`: return the current count

```clojure
;; handler for [:inc-counter]
(defn inc-counter-effect [db]
  (swap! db inc))

;; handler for [:get-count]
(defn get-count-effect [db]
  @db)

;; helper function for creating the counter effect handler
(defn make-counter-effect-handler []
  (let [db (atom 0)]
    (fn [intent]
      (let [intent-type (first intent)]
        (case intent-type
          :inc-counter (inc-counter-effect db)
          :get-count (get-count-effect db))))))

;; usage
(def my-counter-effect-handler (make-counter-effect-handler))

(my-counter-effect-handler [:get-count])
;; 0
(my-counter-effect-handler [:inc-counter])
;; 1
(my-counter-effect-handler [:get-count])
;; 1
```

We've implemented an example effect handler just to give a flavor of what an effect handler might look like. The example doesn't use any libraries or helper functions, but most applications will want to use a library to help create their effect handlers.

State management libraries should provide a means to specify an effect handler. For our examples, we'll use this snippet to "install" our effect handler:

```clojure
(defn with-effect-handler [handler view]
  (ui/on-bubble (fn [intents]
                  (run! handler intents))
                view))
```

# UI State

Now that we have a way to update state and execute side effects, we can move onto building "stateful" UI components. We'll still be using data and pure functions to build our UI components, but the code will generally look similar to UI components built with OO libraries and frameworks. However, we'll still have access to the benefits of functional code.

We'll start with the minimal case, no state, and slowly incorporate different kinds of state needed to build a feature rich user interface.

## No Updating State

This is the easiest case. We already know how to accomplish this. A view with no updating state is just a pure function that receives data as its arguments and returns a view. Unfortunately, some frameworks still mess this up. Rather than having components receive data through function arguments, the data is injected via some other mechanism. We want to avoid injecting data through a side channel even when we do have stateful components, but it's especially egregious in the case where the state doesn't ever change.

## Implicit State

For our first example, let's examine a simple component which shows a "More!" button and a counter. Clicking the "More!" button will increment the counter.

_gif of counter here_

```clojure
(defn counter-ui [num]
  (ui/horizontal-layout
   (ui/on
    :mouse-down (fn [_]
                  [[:inc-counter]])
    (ui/button "More!"))
   (ui/label (str "current count: " num))))
```
Notice that the mouse-down event handler returns the `[:inc-counter]` intent. We can test our `counter-ui` like so:

```clojure
;; check to make sure
;; :inc-counter intent is returned when
;; a mouse down event within
;; the More! button's bounds
(ui/mouse-down (counter-ui 10)
               [0 0])
;; ([:inc-counter])
```

Now that we verified the intent is returned as expected, we can hook it up to an effect handler.

```clojure
;; Using same effect handler in the Effects example
(def my-counter-effect-handler (make-counter-effect-handler))

(my-counter-effect-handler [:get-count])
;; 0

;; Execute the intents returned
;; when clicking on the "More!" button
(run! my-counter-effect-handler
      (ui/mouse-down (counter-ui 10)
                     [0 0]))

(my-counter-effect-handler [:get-count])
;; 1
```
Everything looks good. Now we can actually run the ui as a mini app. 

```clojure
;; run the ui
(backend/run
  (fn []
    (with-effect-handler my-counter-effect-handler
      (counter-ui (my-counter-effect-handler [:get-count])))))
```

Woohoo! We've built our first stateful component. It's a small victory, but we've still a long way to go.

When the "More!" button is clicked, it returns an `[:inc-counter]` intent, but the problem is that it doesn't say _which_ counter should be incremented. What if there is more than one counter? How would we reuse `counter-ui` for controlling multiple counter instances? 

The `counter-ui` is explicitly being passed the count, `num`, which is the precisely the counter we would want increment. To improve `counter-ui`, we should change the `[:inc-counter]` intent to specify _which_ counter should be incremented. To specify which counter to increment, we need a way to represent a reference to the count passed to `counter-ui`.

# References

>  By identity I mean a stable logical entity associated with a series of different values over time.
> {{blockquote-footer}}<https://clojure.org/about/state>{{/blockquote-footer}}

Many, if not most, of the intents produced by a user interface will need to refer to entities. As an example, a todo list app will probably have intents like:
- Add a new todo to a todo list
- Mark a todo as complete

It's important for the corresponding intents to be able to say _which_ todo list and _which_ todo. The way we refer to entities is with **references**.

**Reference**: Data that uniquely identifies an entity within a data model.

<!-- http://day8.github.io/re-frame/reusable-components/#what-is-identity -->

> An entity can use three main techniques to refer to another entity: nesting, identifiers, and stateful references.
> {{blockquote-footer}}Clojure Applied {{footnote}}Clojure Applied Chapter 1, top of page 14.{{/blockquote-footer}}

What data constitutes a valid reference will depend on the data model. Designing a good data model plays a huge role in building an application that works well. Data modeling is a complex subject that is beyond the scope of this post. For our purposes, we'll assume an appropriate data model that uses either nesting or identifiers for referencing entities. 

While membrane doesn't prevent using stateful references, it also offers no builtin support for the stateful reference approach. If you're curious about how a stateful reference approach might look, check out [hoplon](https://github.com/hoplon/hoplon), [reagent](https://reagent-project.github.io/), or the classic, STM based [ant sim](https://gist.github.com/michiakig/1093917).

Below are examples of intents with identifer and nesting based references. Even within a single data model, identifiers and nesting can be mixed and matched.

## Identifier

If the application has a data model where entities have unique identifiers, then simply using the entity's unique identifier as a reference will suffice. Using a todo app as an example and assuming that todo lists and todo items have unique `:id` keys, we can use the identifiers as references within intents.

Example:
``` clojure
;; Intent for "Add a new todo to a specific list"
[:add-todo (:id todo-list) {:done false :description "fix bugs"}]

;; Intent for "Mark todo as complete"
[:mark-todo-complete (:id todo)]
```

As long as the effect handler has a way to lookup and modify entities by id, then using identifiers as references is straightforward.

## Nesting

A nested data representation for a todo list app might look something like:

```clojure
{:todo-lists
 [{:name "Work"
   :todos [{:done false
            :description "fix bugs"}
           {:done false
            :description "ship it"}]}
  {:name "Home"
   :todos [{:done false
            :description "fix bugs"}
           {:done false
            :description "ship it"}]}]}
```

For this type of data model, the nested location of the entity can be used as a reference.
Example:
``` clojure
;; Intent for "Add a new todo to a specific list"
[:add-todo '[(keypath :todo-lists) (nth 0)]
           {:done false :description "fix bugs"}]

;; Intent for "Mark todo as complete"
[:mark-todo-complete '[(keypath :todo-lists)
                       (nth 1)
                       (keypath :todos)
                       (nth 0)]]
```



# Membrane's Approach

Up until this point, we've only covered abstract concepts related to building stateful components. None of the ideas are unique to membrane and similar or analogous concepts will be found in most UI frameworks.

Below, we'll cover some of the tools membrane provides for state management. However, the key idea isn't that membrane's approach is the only or best solution. The main idea is that building user interfaces out of data and pure functions is worth it and we should be doing more of it. The only reason to cover membrane's approach is to show a working example. The design space for writing fully functional user interfaces is large and there's still plenty of unexplored territory!




## Simple State with References

Now that we have a way to represent references, we can now return to our counter example and improve it.

The problem we ran into before diving into references was how to improve `[:inc-counter]` to reference the counter passed as an argument to `counter-ui`. We could use any of the reference types mentioned above, but it turns out our `counter-ui` component doesn't really care which type of identifier it uses.

In order to make it easy to build reusable components, membrane provides a macro for building UI components, `defui`.

Here's what `counter-ui` would look like if implemented with `defui`:

```clojure
(require '[membrane.component :refer [defui]])

(defui counter-ui [{:keys [num]}]
  (ui/horizontal-layout
   (ui/on
    :mouse-down (fn [_]
                  [[:inc-counter $num]])
    (ui/button "More!"))
   (ui/label (str "current count: " num))))


;; usage:
(ui/mouse-down (counter-ui {:num 10})
               [0 0])
;; ([:inc-counter [(keypath :num)]])

```

There are 3 differences from our last iteration:
1. The component is defined using `defui` instead of `defn`
2. Rather than accepting the argument, `num`, it accepts a map with a `:num` key
3. The mouse down event now returns `[[:inc-counter $num]]` rather than `[[:inc-counter]]`

The first two differences are superficial. `counter-ui` is still just a pure function. The main features `defui` provides are:
1. Providing syntax for references.
2. Automatically wiring incidental state (which we'll get to shortly).





## Syntax for references

Within a `defui` definition, prefixing a symbol with `$` will replace that symbol with the reference for that symbol's value. For example, in our `counter-ui` definition above, `$num` will be replaced with a reference for `num`.

References only make sense for data that derives from arguments to the component. However, as long as data is derived from a component argument, then a valid reference can be produced. In the example below, even though `d` isn't directly passed as an argument, its reference can still be produced.

```clojure
(def nested-data {:a {:b {:c {:d 1}}}})

(defui nested-view [{:keys [a]}]
  (let [b (:b a)
        c (:c b)
        d (:d c)]
    (ui/button "More!"
               (fn []
                 [[:inc-counter $d]]))))


(ui/mouse-down (nested-view nested-data)
               [0 0])
;; ([:inc-counter [(keypath :a)
;;                 (keypath :b)
;;                 (keypath :c)
;;                 (keypath :d)]])
```

Currently, `defui` translates references to nested references, but the same syntax could be used with a data model that wants identifiers or stateful references. One benefit of using using nested references is that they can be automatically translated to identifiers if a schema is provided.

We can now extract the button as its own reusable component.

```clojure
(defui more-button [{:keys [num]}]
  (ui/button "More!"
             (fn []
               [[:inc-counter $num]])))

```

Next, rewrite `nested-view` using the extracted `more-button`. 

```clojure
(def nested-data {:a {:b {:c {:d 1}}}})

(defui nested-view [{:keys [a]}]
  (let [b (:b a)
        c (:c b)
        d (:d c)]
    (more-button {:num d})))

(ui/mouse-down (nested-view nested-data)
               [0 0])
;; ([:inc-counter [(keypath :a)
;;                 (keypath :b)
;;                 (keypath :c)
;;                 (keypath :d)]])
```

Notice how we were easily able to extract `more-button` without changing the intents returned by `nested-view`. The component `more-button` doesn't care where or how its argument, `num`, is stored. As long as `more-button` is passed a number, it doesn't matter how it's nested.

```clojure
(def other-nested-data {:foo {:bar {:baz 1}}})
(defui other-nested-view [{:keys [foo]}]
  (let [bar (:bar foo)
        baz (:baz bar)]
    (more-button {:num baz})))

(ui/mouse-down (other-nested-view other-nested-data)
               [0 0])
;; ([:inc-counter [(keypath :foo)
;;                 (keypath :bar)
;;                 (keypath :baz)]])
```



## Why is defui a macro?

Macros that introduce syntax are viewed with skepticism and they should be. It's easy to get carried away with macros and actually make the system more complex. 

One major drawback of macros is that they often limit composability. Functions can be passed around, partially applied, and invoked programmatically and macros can't. Fortunately, this drawback doesn't apply to `defui` since its only purpose is to define a component. The component itself is just a pure function. The extra syntax is just sugar to reduce boilerplate and it's straightforward to replace a `defui` definition with either more verbose code or simply generate the same result programmatically.

A user interface is inherently about communication between a user and a software application. Application data is passed to the UI to produce the view and the user manipulates input devices like the keyboard and mouse to interact with the application data. Being able to easily refer to the nested entities being displayed aligns naturally with the UI's goal of translating raw input events like clicks and key presses into user intents.

Below is the definition for a checkbox:
```clojure
(defui checkbox
  "Checkbox component."
  [{:keys [checked?]}]
  (on
   :mouse-down
   (fn [_]
     [[::toggle $checked?]])
   ;; ui/checkbox is just
   ;; a view of a checkbox
   ;; with no event handling
   (ui/checkbox checked?)))
```

Being able to reference the `checked?` value being passed in allows the checkbox definition to succinctly state the intent of the user when the checkbox is clicked. The intent, `[::toggle $checked?]`, is the most direct representation of the user's intent to toggle the checkbox's value. 



To automatically substitute references, the `defui` macro traces derived values back to the component's arguments. Clojure programs almost exclusively interact with data using abstractions like `nth`, `get`, and keyword lookup. The result is that tracing how data is extracted and passed down can be automated effectively. Manual tracing is error prone and creates unnecessary coupling between UI components and unrelated parts of an application's data model. An alternate approach to macros is using a proxy value to track derived values. The [Om](https://github.com/omcljs/om) clojurescript library used the proxy approach. The disadvantages of the proxy approach are:
* differences between the proxy and the underlying object
* primitive types like numbers, strings, and booleans, can't be proxied
* violates referential transparency

A macro based approach circumvents these issues. The macro's only job is to reduce boilerplate by tracing derived values and automatically producing references. 

Now that our intents include references, we need our effect handlers to be able to work with those references.

## Effect Handling revisited

> Make the common case easy and the complex case possible.
> {{blockquote-footer}}Adapted from a Larry Wall quote{{/blockquote-footer}}

A major trick in our fight against complexity is to build complex components from simpler pieces. We would like to be able to build reusable components like textboxes that we can take apart for testing, debugging, tooling, etc, but we would also like them to be easy to use.

To make it easier to write effect handlers, membrane provides `defeffect` which can be use like so:

```clojure
;; provide an implementation for
;; the ::fire-missiles intent
(defeffect ::fire-missiles! [missile target]
  (fire-missile! missile target))
```

For the most part, it looks and behaves similar to a normal function definition. Below is what the macro expanded version looks like:

```clojure
(let [fvar (defn effect-fire-missiles! [dispatch! missile target]
             (fire-missile! missile target))]
  (swap! membrane.component/effects
         assoc
         :my.ns/fire-missiles! effect-fire-missiles!)
  fvar)
```

`defeffect` does two things:
* defines a function for the effect handler in the current namespace
* registers the effect handler in the global effect registry

Since the effect handler is registered globally, fully qualified keywords are highly encouraged. The name of the function defined in the current namespace will be the same as the name of the intent with "effect-" prefixed to the name. The main reason for the prefix is that the effect handler (eg. `effect-fire-missiles!`) may want to rely on a similarly named function (eg. `fire-missiles!`) in the same namespace. The effect handler function defined in the local namespace won't generally be used directly, but it should have its own name so it can be tested/debugged/etc independently of the rest of the UI.

The last difference between `defeffect` and `defn` is that an implicit argument, `dispatch!`, is prepended to its argument list. We want to allow effect handlers to define themselves in terms of other effect handlers, but we don't want to directly connect implementation of effect handlers. For example, in development we may want the effect handler for `::notify-user` to print to stdout. In production, dispatching a `::notify-user` effect may send an email or text message. 

The default effect handler uses all of the globally defined effect handlers, but an alternate effect handler that augments, instruments, replaces, or removes effect handlers can be easily be produced and provided as the effect handler for a user interface.


### Processing Effects With References



In addition to all of the globally defined effect handlers, the default effect handler also provides these  handlers:
* `[:get $ref]`
* `[:set $ref val]`
* `[:update $ref f & args]`
* `[:delete $ref]`

The `$ref`s are references. Effect handlers that need to modify state can use these builtin handlers to update state by reference. For example, below is the implementation for the `::toggle` effect used by our `checkbox` example:

```clojure
(defeffect ::toggle [$bool]
  ;; use the builtin :update effect handler
  ;; to update the reference to $bool
  (dispatch! :update $bool not))
```

**Note**: outside of `defui`, the `$` prefix has no special meaning. It's only a convention used in membrane code for bindings that represent references (like `m` for map, `coll` for collections, etc).

Frameworks that don't have good support for references require effect handlers to unpack nested data, make modifications, and then repack it again. Under the hood, membrane relies on [specter](https://github.com/redplanetlabs/specter) to efficiently update nested state. In practice, that means effect handlers require less code. Code that is simply unpacking and reconstructing nested data can simply be omitted. Below is an example that is unpacking, modifying, and reconstructing a nested data structure. The example is using re-frame, but a similar example could be taken from a number of different frameworks {{footnote}}<https://github.com/tastejs/todomvc/blob/gh-pages/examples/scalajs-react/src/main/scala/todomvc/TodoModel.scala#L46>{{/footnote}} {{footnote}}<https://github.com/tastejs/todomvc/blob/gh-pages/examples/typescript-react/js/todoModel.js#L32>{{/footnote}} {{footnote}}<https://github.com/tastejs/todomvc/blob/gh-pages/examples/react-alt/js/stores/todoStore.js#L53>{{/footnote}} {{footnote}}<https://github.com/tastejs/todomvc/blob/gh-pages/examples/react/js/todoModel.js#L54>{{/footnote}} {{footnote}}<https://github.com/tastejs/todomvc/blob/gh-pages/examples/mithril/js/controllers/todo.js#L47>{{/footnote}} {{footnote}}<https://github.com/tastejs/todomvc/blob/gh-pages/examples/vanillajs/js/view.js#L201>{{/footnote}} {{footnote}}<https://github.com/day8/re-frame/blob/master/examples/todomvc/src/todomvc/events.cljs#L189>{{/footnote}}.

```clojure
(reg-event-db
  :toggle-done
  todo-interceptors
  (fn [todos [_ id]]
    (update-in todos [id :done] not)))
```


This example doesn't look so bad, but there's a huge cost. Not only is manually writing code to unpack and repack nested data a waste of time, but the `:toggle-done` handler unnecessarily couples the toggling operation with the nested location of the value. For a small application, it's not a big deal, but the cost grows quickly as the size of the app grows. The coupling between the operation and a particular nested location doesn't just affect reuse, but it also hinders testing UI components in isolation. 


## Incidental State

Using pure functions is great and all, but we've got a huge problem. We often want to use a subcomponent and the subcomponent may have some incidental state that we really don't care about. For example, when we use a textbox, we usually only care about the the text being edited and couldn't care less about the current state of the cursor or text selection. Usually. Sometimes we do care about the cursor position, but not the text selection or vice versa. 

One common mistake made by UI frameworks is that the subcomponent author decides which state is incidental rather than the code using the subcomponent. A key observation is that whether or not subcomponent state is incidental or essential depends on the use case. The parent component should always be in charge of deciding which state is essential and which state is incidental. Essential state should be provided explicitly. Ideally, incidental state should be provided implicitly so that the parent component doesn't have to think about how to wire state that isn't directly related to the problem being solved.

### Public API vs Private API
If the parent component decides which state is essential and which state is incidental, how do you keep the parent component from mucking with implementation details in the subcomponent? Deciding which state is part of the public API and which state is part of the private API is a separate, but related question. Public/private API state vs essential/incidental state is often conflated, but they're not the same thing. State that's part of the private API for a component should be, by definition, incidental state. However, it's important to note that even if state is part of the private API, it's often useful when debugging/developing/testing for the parent component to be able to treat the private state as an opaque value. For example, a bug may only occur when private state has a certain value and if the private state is completely inaccessible, then have fun trying to write tests or debug the issue.{{footnote}}If you've ever tried to work around an issue caused by Chrome Autofill, you know the depths of despair that private, hidden state can cause.{{/footnote}} The recommended way to handle private API state in a membrane component is to put all the private state in a single map under a key named `:private`.

```clojure
;; store private API state in private variable
(defui my-component [{:keys [a b private]}]
  (let [private-num (:num private)
        private-str (:str private)]
    ...))

```


Ok, so now we know where to put private API state, but if there is no "hidden" state, then it seems like it would be a pain to plumb incidental state all the way to the component that needs it. It would certainly be a nightmare if using a textbox meant passing a bunch of extra state around for every parent component, grandparent component, and so forth. Fortunately, plumbing incidental state can be automated and is taken care of implicitly by `defui`.

When a component is defined using `defui`, its var is adorned with metadata that marks it as a membrane component. Calls to membrane components within the body of a membrane component definition will automatically provide any incidental state necessary for child components.

<!-- _Explain more about how incidental state works?_ -->
<!-- * each component can have incidental state -->
<!-- * individual state is "indexed" by the identities of the other provided state -->

### Contextual State

The next category of state we'll cover is contextual state. The most prominent example of contextual state is focus which is mostly about deciding which component should be responding to keyboard events. Generally speaking, contextual state smells a lot like global state so it is used sparingly. Contextual state is handled exactly the same way as incidental state, except rather than every component having its own incidental state, every component shares the same context. 

To declare a component property as contextual, simply add the `:membrane.component/contextual` key to the metadata for the property like so:

```clojure
(defui my-component [{:keys [a
                             b
                             ^:membrane.component/contextual
                             my-context]}]
  ...)
```

As always, the parent is in charge and if they decide that the property shouldn't be automatically passed to a child component, they can simply explicitly provide the property's value. By explicitly passing the property, it will no longer be treated like incidental state and will work exactly like any other property that makes up the essential state of the subcomponent.

### Top Level State

We'll use "Top Level" to refer to whatever the most global scope is for a user interface. For a desktop app, the top level will typically be a window. For the web, the top level will typically be a root DOM element. Some examples of contextual state are modals, context menus, pop ups, scroll state, dropdowns, and drag&drop. There's not actually anything special about top level state. The way membrane handles top level state is by providing a component, `membrane.component/top-level-ui` that receives a child component (your user interface) and takes care of all the top level state. 

For ease of use, best practice is to use `membrane.component/make-app` to wrap your user interface with a `top-level-ui` component and connect it to an effect handler. If alternate behavior is desired, the underlying pieces can be rearranged/remixed to achieve the desired outcome.



# What is a UI Component?

"Component" is often one of those words that gets used when no better name comes to mind (like Object, Manager, Widget, Controller, etc). One of the goals of membrane is to improve the precision of the jargon around UI programming.

A user interface is the combination of the two pure functions:

1. **Event Function** - a pure function which receives the application state and an event and returns data specifying the user's intent (eg. add a new todo item to the todo list).
2. **View Function** - a pure function which receives the relevant application state as an argument and returns data specifying _what_ to draw (how to draw the data will be provided elsewhere).

A UI **component** is just a user interface whose event function returns intents with references to parts of the state passed in.

Using this definition, we can now answer "What is checkbox?". We should be able to break down any component into its arguments, view function, event function, and default behavior. The break down for a checkbox is as follows:
* **checkbox arguments**: A true/false value.
* **view function**: Returns a view that can represent two states (true/false) that correspond to the argument passed in.
* **event function**: When clicked, returns an intent that toggles the value passed in. 
* **default behavior**: The default behavior for toggling should be logical negation.

In code:
```clojure
(defeffect ::toggle [$bool]
  (dispatch! :update $bool not))

(defui checkbox
  "Checkbox component."
  [{:keys [checked?]}]
  (on
   :mouse-down
   (fn [_]
     [[::toggle $checked?]])
   (ui/checkbox checked?)))
```



# Reusability

We've finally covered all the different topics needed to build users interfaces out of data and pure functions. Ultimately, the goal is to make UI code more flexible, more reusable, and easier to reason about. Briefly, we'll cover some examples that highlight our progress.

Below are several examples of how UI interfaces ususally aren't reusable:
* Testing user interfaces is cumbersome, highly manual, and/or ineffective
* Components from different frameworks don't compose
* Often, components from the same framework don't compose
* Unnecessary coupling between components and state management undermines tooling

By using data and pure functions, we can recover each of these capabilities. By default, everything snaps together, but the individual pieces can be extracted.

## Testing

Testing in UI code tends to be less common. It's difficult to break user interfaces down into testable pieces. The main area where functional frameworks have focused is effect handling, which is a challenging area for testing. An important part of effect handling that has found some success with testing is state management. However, its effectiveness is often limited by hidden state and coupling between operations and specific nested locations.

The main benefit of just using data and pure functions is that nothing special is required for testing. Testing UI code is just like testing code for any other domain.

A component can be broken into its view function and event function. Events functions are just functions that return intents (ie. data). Crucially, view functions are also just functions that return views (ie. data). Based off our work in part I and part II, it's even possible to do generative and property based testing with view functions.

Some examples of property based tests that may be interesting for views:
* Does the view fit within some bounds?
* Does the view contain overlapping text?
* Are interactive elements obscured, invisible, or too small?
* What are colors are shown?
* and more!

Arguments to the view and event functions are just data and can be described with `spec`. Given a spec for a component's arguments, it's trivial to procedurally generate views and event handlers. Given an event handler, it's trivial to programmatically generate events for the event function to generate intents. Given an effect handler, the generated events can then exercise the effect handler. Basically, programmatically testing and driving a UI is as simple as testing and driving any other program.
  
## Composing Components

Membrane components can also be used from other UI frameworks. All that is required is to write a function that wraps a component with whichever state management option you desire. The transformation is entirely mechanical. A converter for each of re-frame{{footnote}}<https://github.com/phronmophobic/membrane-re-frame-example/blob/master/src/membrane_re_frame_example/term_view.clj#L20>{{/footnote}}, fulcro{{footnote}}<https://github.com/phronmophobic/membrane-fulcro/blob/main/src/com/phronemophobic/todo.clj#L26>{{/footnote}}, and cljfx{{footnote}}<https://github.com/phronmophobic/membrane/blob/master/src/membrane/cljfx.clj#L914>{{/footnote}} is provided within membrane. For example, `membrane.re-frame/defrf` can transform any membrane component into a re-frame component. In theory, a converter could be written for any state management framework. 

Every UI framework has its own library of components that are all incompatible with every other UI framework. This is a huge waste of effort. Developers should be able to choose the framework that best suits them, but still have access to components from other frameworks. Why shouldn't UI components be usable from other frameworks? We already know how to do this{{footnote}}<ttps://day8.github.io/re-frame/reusable-components/#implications>{{/footnote}}. If we build our programs with data and pure functions, we reap flexibility and reuse.


# To Be Continued

Next time on "How to build a functional UI library from scratch", we'll discuss what simple UI components means for UI tooling. Stay tuned!


# Footnotes

{{footnotes/}}



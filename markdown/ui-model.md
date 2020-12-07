{{table-of-contents/}}

_This post is the second in a series of posts explaining the design principles behind [membrane](https://github.com/phronmophobic/membrane), a cross platform library for building fully functional user interfaces in clojure(script)._

# Introduction

[Previously](what-is-a-user-interface.html), on **How to build a functional UI library from scratch**. We defined a fully functional user interface as the combination of two pure functions, the event function and the view function. Next, we'll take a closer look at views, events, and how they are implemented in membrane. 

<!-- Many of the implementation ideas should be familiar to anyone with functional programming experience. -->

In this post, we'll get into the nitty gritty of how the concepts outlined in the previous post are implemented. Many of the illustrative examples will be internal "implementation details" of membrane (which is a little bit like showing off your underwear). There are a few reasons for being a little risqué:
* There are too many libraries building on top of React and not enough libraries working to fix the problems "under" React.
* When working on membrane, it was difficult to find good references covering the designs of platform toolkits. Trawling through code bases like Chromium, GTK, QT, Swing, AWT, etc. for insights is arduous.
* Building a library like membrane currently requires implementing a large surface area. Building user interfaces requires integrations with graphics libraries, platform toolkits, event handling, text rendering, and state management. There are high quality, functional options for state management, but most of these libraries are strongly coupled to a particular platform toolkit.{{footnote}} I've tried to extract some, but it's much more difficult than you might expect.{{/footnote}} Some design decisions in membrane were made thoughtfully and some design decisions were made to expedite the goal of having a fully working system where new ideas can be tested. Hopefully this post can provide some context for future library authors building "underneath" React.

<!-- In a sense, there's nothing new here. The principles behind membrane are neither novel or new.{{footnote}}[Out of the Tar Pit](http://curtclifton.net/papers/MoseleyMarks06a.pdf) covers many of the principles and provides references to prior art.{{/footnote}} However, membrane makes several key design decisions that differ from other libraries in the same space. -->

> As we make things simpler, we get more independence of decisions because they're not interleaved
> {{blockquote-footer}}[Rich Hickey](https://github.com/matthiasn/talk-transcripts/blob/master/Hickey_Rich/SimpleMadeEasy.md){{/blockquote-footer}}

One of the promises of building systems using simpler constructs is flexibility. Unfortunately, having more decision points can make the day to day development of software harder. To combat analysis paralysis, membrane follows "make the common case easy and the complex case possible".{{footnote}}Derived from Larry Wall's "make the easy things easy, and the hard things possible"{{/footnote}} {{footnote}}Hopefully, this isn't too disappointing for the masochistic web developer that's used to "make the common case complex, and the complex case janky".{{/footnote}} In practice, that means using simple constructs (make the complex case possible) and have tools, recipes, documentation and frameworks that package best practices for common tasks (make the common case easy). In this post, we'll be focusing on the simple constructs themselves, so it may not be clear how everything fits together until future posts. <!-- , which we'll start to cover in the next post. -->




<!-- it's the same clojure process. define a data model using clojure's abstraction constructs. write a suite of pure functions that slice, dice, inspect, and manipulate the data model. -->


# Event loops

<!-- > These are precisely the lessons which logic programming teaches us, and because of this we would like to take the lead for our ideal approach to control from logic programming which shows that control can be separated completely. -->
<!-- > {{blockquote-footer}}[Out of the Tar Pit](http://curtclifton.net/papers/MoseleyMarks06a.pdf){{/blockquote-footer}} -->

Thus far, everything has been a little fuzzy and abstract. For diving into actual code, an event loop is a great starting point. It's where the rubber hits the road. We get to `draw!`, `wait-events`, and other exciting side-effecty stuff 🤓💥🔥! Below is the event loop used by membrane's [skia backend](https://github.com/phronmophobic/membrane/blob/master/src/membrane/skia.clj). The skia backend uses [Skia](https://skia.org/) for graphics and [GLFW](https://www.glfw.org/) for window management.

```clojure
(try
  (when (init)
    (add-windows!)

    (loop []
      (wait-events)

      (glGetError)

      (add-windows!)

      (close-windows!)

      (run! repaint!
            (var-get windows))

      (when (seq (var-get windows))
        (recur))))
  (catch Exception e
    (println e))

  (finally
    (cleanup)))
```

The first observation might be that the event loop doesn't look very pure or functional (hint: it's not). The point of a functional architecture isn't to be side-effect free, but to have side effects at the edges and this is the edge.

The basic elements of this event loop are:
- initialize resources
- waiting on events
- redrawing
- if there's more to do, loop
- clean up

Many event loops will follow a similar pattern. Most UI frameworks have _the_ event loop which is hidden deep within the belly of the beast. <!-- It's easy to forget that it's even there. Since most UI libraries are monolithic, it's wouldn't be feasible to provide your own event loop anyway.  -->As mentioned before, one of the promises of simplicity is flexibility. In membrane, when the platform toolkit allows, the decision of which event loop to use can be made independently of other architectural decisions. Most developers will use the good-enough default, but there will still be the option to use or create an alternative event loop should the use case demand it.

Some requirements for the event loop are imposed by operating systems and platforms. Some requirements are dictated by the application. For example, it may be useful to have a different event loop for development and testing purposes. Allowing event loops to be swapped out is an opportunity for frameworks to provide good options for specific use cases (eg. games, editors, document viewers, etc). For complex applications, it's common to use whatever hooks are available (eg. `setTimeout`, `requestAnimationFrame`) to try to suitably customize the event loop, but adding a bunch of hooks usually leads to an [inner platform effect](https://en.wikipedia.org/wiki/Inner-platform_effect).

Event loops are messy. The trick is to construct the event loop so that we can get back to using data and pure function as soon as possible.

# Graphics

As we try to get back to functional programming land, let's begin by taking a deeper look at the `repaint!` function referenced from our event loop:

```clojure
(repaint! [this]
  (glfw-call Void/TYPE glfwMakeContextCurrent window)

  (Skia/skia_clear skia-resource)

  (binding [*image-cache* image-cache
            *font-cache* font-cache
            *window* this
            *draw-cache* draw-cache
            *skia-resource* skia-resource]
    (let [view (reset! ui (view-fn))]
      (draw view)))
  (Skia/skia_flush skia-resource)
  (glfw-call Void/TYPE glfwSwapBuffers window))
```

All of this looks icky, but don't worry. We're almost ready to return to the safe haven of pure functions and data. The main reason to show these snippets is to demystify what's going on under the hood of UI frameworks{{footnote}}Is it working?{{/footnote}}. Most of the code is just boilerplate. Really, the main bit is this snippet:

```clojure
(let [view (reset! ui (view-fn))]
  (draw view))
```

Look! It's our friend, the view function! You may notice that `view-fn` doesn't receive any arguments. At this point, it's assumed that any state has been closed over. The event loop doesn't and shouldn't care about how state is handled. It only needs to know what to draw and who to tell about new input events.

All we needed to get back to data and pure functions was to separate _what to draw_ from _how to draw it_. 


## View Function

> **View Function** - a pure function which receives the relevant application state as an argument and returns data specifying _what_ to draw (how to draw the data will be provided elsewhere). This facilitates communication from the application to the user.
> {{blockquote-footer}}[What is a User Interface?](what-is-a-user-interface.html){{/blockquote-footer}}

To get started, all we need is data that describes what to draw. We'll need a way to represent all the usual suspects:
- shapes with stroking and filling
- images
- text
- grouping
- 2D transforms

Membrane uses records. Below are some abbreviated definitions that membrane uses.{{footnote}} The actual definitions are only slightly more verbose <https://github.com/phronmophobic/membrane/blob/master/src/membrane/ui.cljc>{{/footnote}}

```clojure
(defrecord Label [text font])
(defrecord Image [image-path size opacity])
(defrecord Rectangle [width height])
(defrecord RoundedRectangle [width height border-radius])
```

## Grouping

Now that we have a few items we can draw, we need a way to compose them together. Clojure already has a data structure for grouping, vectors. A vector specifies a group of elements that should be drawn in order. Currently, sequences and maps have undefined meaning in our graphics model and are reserved for future specification. It's likely that sequences will have the same meaning as vectors.

It's still not clear what the optimal graphics data model is, but fortunately, it's pretty easy to beat `<div/>`s and `<span/>`s in terms of usability. Designing a better model is a promising area for improvement in the future. Clojure's data abstractions make it easy to provide an open model. If a newer, better model is available, it can be added without making breaking changes as long as the new model implements all of the relevant protocols.
<!-- Key Concepts: data, data, data -->

## Transforms

Everyone's favorite 2D transforms:

```clojure
(defrecord Translate [x y drawable])
(defrecord Rotate [degrees drawable])
(defrecord Skew [sx sy drawable])
(defrecord AffineTransform [matrix drawable])
```

## Convenience wrappers

Instantiating the records directly is discouraged. Each record type has a wrapper function that should be used.

Examples:
```clojure
[(ui/label "Hello World!")
 (ui/translate 0 12
               [(ui/label "Hello World!")
                (ui/translate 0 12
                              (ui/label "Hello World!"))])]
```

For more details about the graphics model, check out the [graphics tutorial](https://github.com/phronmophobic/membrane/blob/master/docs/tutorial.md#graphics).


## Hello World

Now that we have a way to describe what to draw, we can now write the Hello World program with membrane. There are several options available in membrane for running a user interface. To "run" a user interface, you need some way to hook into the graphics and events provided by a platform toolkit. The platform toolkit hooks are called **graphics backends** in membrane. A graphics backend typically exposes a `run` function:

```clojure
(run view-fn)
;; and/or
(run view-fn opts)
```

Here's the hello world code:

```clojure

(require '[membrane.ui :as ui])

(require '[membrane.skia :as backend])
;; other example backends
;; (require '[membrane.lanterna :as backend]) ;; terminal
;; (require '[membrane.java2d :as backend]) ;; Swing
;; (require '[membrane.vdom :as backend]) ;; Web divs
;; (require '[membrane.webgl :as backend]) ;; Web openGL



(defn view-fn []
  (ui/label "Hello World"))
 

(backend/run view-fn)
;; for webgl or vdom
;; (backend/run view-fn {:container (.getElementById js/document "my-hello-world-container")})

```

Creating a new backend is beyond the scope of this post, but making a minimal graphics backend only requires:

1. An event loop
2. A few draw implementations (like shapes, images, and text)
2. A way to hook up input events (like mouse clicks, movement, and key presses, etc.)

## Generic Manipulation

The main idea is that graphical elements are values. They can be manipulated and inspected on any thread without synchronization. Views are semantically transparent which facilitates serialization, network transmission, storage, and implementation in multiple languages and platforms.

To support generic manipulation, membrane provides the following functions for inspecting any view:

```clojure
;; Specifies the top left corner of a component's bounds
;; The origin is vector or 2 numbers [x, y]
(origin elem) ;; [5 10]

;; Returns a 2 element vector with the [width, height] 
;; of an element's bounds with respect to its origin
(bounds elem) ;; [50 100]

;; Returns sub elements of elem. Useful for traversal.
(children elem) 

```

These functions are only the most basic tools for inspecting views. Just like clojure's suite of functions for slicing and dicing data keeps growing, so too will membrane's suite of functions for inspecting and manipulating views. Since views are just plain ol' data, you get all the benefits of working with values.

## Coordinates

![Coordinates](ui-model/coordinates.png)

Coordinates are represented as a vector of two numbers `[x, y]`.

## Positioning and Layout

Views are just data. When, where, and how to layout views can be decided independently. 

Do you want to...
* use an [incremental constraint solver](https://constraints.cs.washington.edu/cassowary/) like iOS? 
* precompute a bunch of layout data at compile time?
* run your layout computations on a 128 core super computer and use STM (ie. refs) to build a consistent snapshot to present?
* use flexbox?
* arbitrarily split up your layout logic across 3 different languages?{{footnote}}(╯°□°）╯︵ ┻━┻{{/footnote}}

Currently, membrane only provides basic layout functions, but since views are just data, it's straightforward to make more. For example, below is the implementation of `membrane.ui/center` which will vertically _and_ horizontally center a view{{footnote}}Take that, css!{{/footnote}}:

```clojure

(defn center [elem [width height]]
  (let [[ewidth eheight] (bounds elem)]
    (translate (int (- (/ width 2)
                       (/ ewidth 2)))
               (int (- (/ height 2)
                       (/ eheight 2)))
               elem)))
```

Example usage:
```clojure

(def container (ui/rectangle 100 100))

(def centered-text (ui/center (ui/label "Hello")
                              (ui/bounds container)))
;; #Translate{:x 33, :y 43,
;;            :drawable #Label{:text "Hello",
;;                             :font #Font{:name nil,
;;                                         :size 14,
;;                                         :weight nil}}}

```

For more info, see [Basic Layout](https://github.com/phronmophobic/membrane/blob/master/docs/tutorial.md#basic-layout).

## Drawing

We won't say too much about how drawing is implemented. Most users won't be writing drawing code. One key idea is there is not one draw function, but many. The purpose of drawing is to turn a description of what to draw into pixels. There are dozens of ways to draw elements. Not only are there different algorithms and libraries, but also different targets (screens, image buffers, image files, etc.).

Many platform toolkits smush together what to draw with how to draw it. Separating _what_ from _how_ yields flexibility and reuse.

# Events

In membrane, the event model is pluggable. If the event model doesn't suit the use case, it can be swapped independently of other parts. Further, membrane's default event model isn't monolithic, so it's possible to reuse parts of the event model to construct a new event model if a small tweak is needed. Most applications will use the default event model "as is", but the flexibility to replace or augment the event model for development, testing, performance, or tooling is a nice bonus. Creating an alternate event model is beyond the scope of this post. We'll simply refer to the default event model as the event model for the rest of this post.

**Event Function** - a pure function which receives the application state and an event and returns data specifying the user's intent (eg. add a new todo item to the todo list). This facilitates communication from the user to the application.

**Event**: Data representing the actions of a user. Examples of events are mouse clicks and key presses from a keyboard.

**Intent**: Data representing a user intent. Examples of user intents are "delete a todo list item", "open a document", "navigate to a URL".

**Effect**: The carrying out of an intent.

**Event Handler**: A pure function of an **Event** to **Intents**.

In practice, it's easier to specify the events and graphics together. Instead of having completely separate view and event functions, there's a single function that acts as both the view and event function. In other words, there's one function that returns data that describes what to draw and can translate events into intentions.

For each type of event, there is a corresponding protocol:
```clojure
(defprotocol IMouseMove (-mouse-move [elem pos]))
(defprotocol IMouseMoveGlobal (-mouse-move-global [elem pos]))
(defprotocol IMouseEvent (-mouse-event [elem pos button mouse-down? mods]))
(defprotocol IDrop (-drop [elem paths pos]))
(defprotocol IScroll (-scroll [elem delta mpos]))
(defprotocol IMouseWheel (-mouse-wheel [elem delta]))
(defprotocol IKeyPress (-key-press [elem key]))
(defprotocol IKeyType (-key-type [elem key]))
(defprotocol IClipboardPaste (-clipboard-paste [elem contents]))
(defprotocol IClipboardCopy (-clipboard-copy [elem]))
(defprotocol IClipboardCut (-clipboard-cut [elem]))
```

To specify an event handler, all that is needed is to implement the corresponding protocol. For convenience, the recommended way to add an event handler is by using `membrane.ui/on`.

```clojure
(def my-elem (ui/on
              :mouse-down (fn [[mx my]]
                            [[::my-intent mx my]])
              (ui/label "hello world")))
(def mouse-pos [3 4])

;; check to make sure it's working
(ui/mouse-down my-elem mouse-pos) ;; [[::my-intent 3 4]]
```

Already, this example might look a little funny. What is `ui/mouse-down`? It's the event function! It really is just a pure function. 

The other mystery is "what's up with the handler's return value, `[[:my-intent mx my]]`?"

In membrane's event model, event handlers should return a sequence of intents. An intent is a vector{{footnote}}Some frameworks have started to move away from using vectors for intents/effects towards using maps. Membrane may follow suit at some point.{{/footnote}} where the first element is the intent type. Using namespaced keywords for the intent type is encouraged. 

It might not be apparent why having pure event functions is a big deal. For comparison, let's look at an event handler from [re-frame](http://day8.github.io/re-frame/dominoes-30k/#domino-1-event-dispatch):

```
[:div.garbage-bin 
    :on-click #(re-frame.core/dispatch [:delete-item item-id])]
```

On the surface, this appears pretty similar, but architecturally, it's very different. The event handler has a side effect. Further, the dispatch function itself is hooked into global state. Essentially, we've just tangled together parts of our application that should be decoupled. To be clear, this isn't `re-frame`'s fault{{footnote}}`re-frame` is great. It's a really practical library and the community around it is helpful and friendly.{{/footnote}}. It's a limitation imposed by an OO event model addicted to side effects.

> Computer science offers a standard way to handle complexity:hierarchical structure.
> {{blockquote-footer}}Leslie Lamport {{footnote}}[How to Write a 21st Century Proof](https://lamport.azurewebsites.net/pubs/proof.pdf){{/footnote}}{{/blockquote-footer}}

As described in the previous post, the biggest issue with OO event systems is that they are primarily or exclusively side effect driven. Side effects ruin composition. In membrane, event handlers are pure functions that take the event data as arguments and return the intents of the user. In this regard, event handlers are more akin to ring's middleware or re-frame's interceptors.

In membrane, event handlers are composed hierarchically. Parent components may pass events down to child components and may alter or ignore the intents child components return. A key principle in membrane is that the parent is in charge. When event handlers contain side effects, this principle is violated. It means the parent component no longer has the final word on what intents will be returned from the event handler.

Let's take a look at how the mouse move event is implemented in membrane. A default implementation for all objects is provided.

```clojure
(extend-type #?(:clj Object
                :cljs default)
  IMouseMove
  (-mouse-move [elem local-pos]
    (let [intents
          (some #(when-let [local-pos (within-bounds? % local-pos)]
                   (seq (-mouse-move % local-pos)))
                (reverse (children elem)))]
      (-bubble elem intents))))
```

The default implementation simply delegates to the first (in reverse draw order) child component that is under the mouse and provides a response. Any element may provide its own mouse-move implementation, typically by using `membrane.ui/on` as demonstrated previously.

Since we're not ignoring the return value, we have some powerful functional tools for parent components to interact with the handlers of child components. The simplest example is simply ignoring all event handlers for a child component (aka. the functional equivalent of `.stopPropagation`{{footnote}}Sorta{{/footnote}}).

```clojure
(def my-elem (ui/on
              :mouse-down (fn [[mx my]]
                            [[::self-destruct]])
              (ui/label "Self Destruct")))
(def mouse-pos [3 4])

;; uh oh
(ui/mouse-down my-elem mouse-pos) ;; [[::self-destruct]]

;; phew!
(ui/mouse-down (ui/on :mouse-down (fn [_] nil)
                      my-elem)
               mouse-pos)
;; => nil
```

In fact, it's possible to completely silence all event handlers for a child component by simply wrapping the component with `membrane.ui/no-events`.
```clojure
(membrane.ui/no-events child-elem)
```

<!-- ## RTree -->

<!-- The hiearchical structure of event handling also makes it easy to implement different approaches to hit detection for different parts of the view hierarchy. For example, [treemap-clj](https://github.com/phronmophobic/treemap-clj) wraps the treemap view so that hit detection uses an [RTree](https://en.wikipedia.org/wiki/R-tree)  to 10s of thousands of interactive elements. -->

### Wrapping

Wrapping child components is common and useful enough that it has its own function, `wrap-on` which will pass the child's event function as the first argument to the event handler.

Some examples of `wrap-on`:

```clojure
;; Add 10 to the x mouse position for all mouse-down
;;  events processed by child components
(ui/wrap-on
 :mouse-down (fn [child-handler [mx my]]
               (child-handler [(+ 10 mx)
                               my]))
 child-elem)

;; remove all ::delete intents returned from the child view
(ui/wrap-on
 :mouse-down (fn [child-handler [mx my]]
               (let [child-intents (child-handler [(+ 10 mx)
                                                   my])]
                 (remove #(= ::delete (first %)) child-intents)))
 child-elem)

;; Return all child intents and append some additional intents
(ui/wrap-on
 :mouse-down (fn [child-handler [mx my]]
               (let [child-intents (child-handler [(+ 10 mx)
                                                   my])]
                 (into child-intents
                       [[::send-notification]
                        [::cleanup]])))
 child-elem)
```

### Bubbling

If you were paying close attention to the default event handler implementation, you may have noticed the odd `(-bubble elem intents)` wrapping the return value. 

The default implementation of `-bubble` is to simply return the intents unchanged:

```clojure
(-bubble [this intents]
    intents)
```

The event function receives events and returns intents. Parent components may alter or ignore events before child components see the event and may alter or ignore intents before they are returned. Bubbling facilitates altering or ignoring the intents being returned by child components. In principle, modifying the outgoing intents could be achieved with `membrane.ui/wrap-on`, but that would require wrapping every event type{{footnote}}Bubbling may be implemented by wrapping all event types in the future. The main consideration is probably performance.{{/footnote}}.


Other event models also have "bubbling", but membrane's bubbling is different. The return values of event handlers aren't ignored. Functional bubbling is powerful and provides an elegant way to make components more reusable with less code.

Functional bubbling allows you to alter intents that are getting passed back up the chain. Using `membrane.ui/on`, it's possible to listen for any intent type and transform it.

```clojure

(def add-todo-button (ui/on :mouse-down (fn [_]
                                          [[::add-todo]])
                            (ui/button "Add Todo")))

;; wrap add-todo-button
;; capture all ::add-todo intents bubbling and
;; qualify that we're adding a todo to ::work-todos
(def work-add-todo-button (ui/on ::add-todo (fn []
                                              [[:add-todo ::work-todos]])
                                 add-todo-button))

(def mpos [3 4])
(ui/mouse-down add-todo-button
               mpos) ;; [[:add-todo]]

(ui/mouse-down work-add-todo-button
               mpos) ;; [[:add-todo ::work-todos]]
```

It may not be completely obvious why functional bubbling is important, but it's a critical technique for making components more reusable.

## Reusable by Default


> Often, several components need to reflect the same changing data.
> {{blockquote-footer}}React docs: [Lifting State Up](https://reactjs.org/docs/lifting-state-up.html){{/blockquote-footer}}

For a component to be reusable, it often needs to operate on nested state. Which subset of state can't be known ahead of time and multiple instances of a component may be operating on completely different state or on shared state. The recommended solution proposed by most libraries is to _rewrite_ your component so you can [lift state up](https://reactjs.org/docs/lifting-state-up.html). Membrane contends that components should be reusable by default. Rather than adding callbacks to delegate event handling, membrane advocates leveraging functional bubbling.

We'll use the the same temperature converter example from the react tutorial. The basic idea is to create temperature converter widget. In membrane, that might look something like:

```clojure
(defn temperature-input [temperature scale]
  (ui/vertical-layout
   (ui/label "Enter temperature in " (get scale-names scale))
   (textarea :value scale
             :on-change (fn [val]
                          [[::change-temperature (parse-temp val)]]))))
```

When the textarea changes, the event handler would return a `[::change-temperature parsed-temp-val]` intent. What happens if there is more than one temperature to change? Do we need to rewrite the temperature input? ... No! The temperature input doesn't need to know or care about which temperature it's operating on. We just need to adorn the returned intent with the necessary context using bubbling:

```clojure
;; Temperature calculator that shows multiple temperature inputs
;; using the temperature-input component above
(defn temperature-calculator [temps]
  (apply
   ui/vertical-layout
   (for [temp temps]
     (ui/on
      ::change-temperature
      (fn [new-temp]
        ;; adorn the intent with the temperature's id
        [[::update-temperature (:id temp) new-temp]])
      (temperature-input (:num temp)
                         (:scale temp))))))
```

We're skipping the state management part of this example for now, but we'll give it the full treatment in the next post!

## Events in the Event Loop

Similar to how we were able to separate the control part of the event loop from the purely functional `view-fn`, we can separate the event functions from the control flow of the event loop. From our event loop, let's zoom in on the `wait-events` function:

```clojure
(defn wait-events []
  (glfw-call void glfwWaitEventsTimeout (double 0.5)))
```

It's just a call to `glfwWaitEventsTimeout`{{footnote}}Other are options are `glfwWaitEvents`, `glfwPollEvents`, and `java.lang.Thread/sleep` which are sometimes used for debugging.{{/footnote}}

> **glfwWaitEventsTimeout**: It puts the thread to sleep until at least one event has been received, or until the specified number of seconds have elapsed. It then processes any received events.
> {{blockquote-footer}}GLFW [Input Guide](https://www.glfw.org/docs/latest/input_guide.html){{blockquote-footer}}

Processing events works by invoking callbacks that were set up during initialization(not shown in the above examples). Callbacks are then run in the event loop when `glfwWaitEventsTimeout` is called. Below is one of the callbacks. The other callbacks follow a similar pattern (some of the callbacks are a little messier because they need to standardize events to match other backends).

```clojure
(defn- -mouse-button-callback [window window-handle button action mods]
  (try
    (mouse-event @(:ui window)
                 @(:mouse-position window)
                 button
                 (= 1 action)
                 mods)
    (catch Exception e
      (println e)))

  (repaint! window))
```

Look! It's one the event functions, `mouse-event`! Similar to the `view-fn`, `mouse-event` doesn't explicitly get passed state. At this point, it's assumed that any state has been closed over. The event loop doesn't and shouldn't care about how state is handled. It only needs to know what to draw and who to tell about new input events.

# Conclusion

We've now covered how membrane implements the Functional UI Model covered in the first post. Using the same (old) functional techniques, we've replaced the common OO graphics and event models with functional ones. In the next post, we'll cover state management and show how to reap the benefits of working with data and pure functions.

# Appendix

## Platform Agnostic

The UI model provided by membrane is platform agnostic. It doesn't say anything that's specific to a particular operating system or environment. One accomplishment of the web is showing that user interfaces can be platform agnostic. Membrane doesn't argue that all interfaces _should_ be platform agnostic, but UI libraries and frameworks shouldn't unnecessarily be coupled to particular platform toolkits or environments. There are times when leveraging platform specific features is the best or only option. Membrane's design is flexible enough to interoperate with platforms when necessary, but it also makes it easy to write platform independent user interfaces.

## Performance

Performance is an important feature for any user interface. User interfaces that feel responsive are better. While many low hanging optimizations have been implemented, achieving the fastest benchmarks is not currently a priority. Membrane is still in the design phase and the highest priority is to optimize the design. Performance considerations will influence design decisions. While membrane is still in the design phase, bottlenecks will be addressed as necessary. If you experience a performance issue, please file a [github issue](https://github.com/phronmophobic/membrane/issues/new).

Having said that, one of the surprises of working on membrane is how responsive the resulting UIs feel. One of the main points of comparison is the web browser and it turns out the web browser spends a bunch of time monkeying around.

# Footnotes


{{footnotes/}}



{{table-of-contents/}}

_This post is the first in a series of posts explaining the design principles behind [membrane](https://github.com/phronmophobic/membrane), a cross platform library for building fully functional user interfaces in clojure(script)._


# Introduction




Building user interfaces is harder than it ought to be. 

When talking to UI developers, I hear one of two sentiments. Either:
1) Many of the challenges of user interface programming were solved by the introduction of React and we should only expect incremental improvements for at least a decade.
2) The challenges of user interface programming are simply inherent to the problem. User interfaces are sticky, icky, and irregular. That's just the way it is.

Neither is true and if we're going to face the current challenges in user interface programming, we need to take a step back and reconsider some of our assumptions.

This post actually says very little that's specific to membrane. The main goal of this post is to clearly define what a user interface is. It's really easy to skip this step, but it's crucial for understanding membrane's design. Everything in membrane is built on top of these concepts. If you're looking for actual code, stay tuned for future posts!

Membrane is still a work in progress, so if you have feedback, you can file an issue for [this post](https://github.com/phronmophobic/blog/blob/master/markdown/what-is-a-user-interface.md), or on [membrane](https://github.com/phronmophobic/membrane/issues/new).

## Questions

Rather than take a bottom up approach that starts with platform UI toolkits (Swing, JavaFX, HTML, UIKit, etc) and building up, we'll take a top down approach. We'll start with fundamental questions about how user interface programming should be modeled and work our way down to the platform toolkits.

Here are some of the questions to get started.

- What is a user interface?
- What should the building blocks be?
- How should state management work?
- What should the workflow for building user interfaces look like?
- What kind of tooling should there be?
- How should designers and programmers work together?
- How would repl driven development work?

<!-- {{contemplation-break/}} -->


# What is a user interface?

It may seem pedantic to even ask such a rudimentary question, but we're going to do it anyway. As is clojure tradition, we'll start with word etymologies.

Interface stems from the two roots, _inter_ and _face_.

**inter**- (prefix): "between", from Latin.

Between what? Obviously, it's between the user and "something else". We'll call that "something else" an application. One subtle implication of this definition is that the user interface isn't actually a part of the application itself, or at the very least, the user interface is detachable and replaceable. That would certainly be nice. Having a detachable user interface would be great for testing, debugging, or if we simply would like to upgrade a user interface without major surgery.

Currently, developing a UI is like working on a sticky, growing ball of goo that oozes onto everything it touches before eventually absorbing your whole application. Yuck. If possible, we would much rather have thin, clean user interfaces that aren't sticky, ravenous blobs.

**face**- "a plane surface regarded as the common boundary of two bodies" {{footnote}}<https://www.etymonline.com/word/interface>{{/footnote}}

<!-- 2 : a surface forming a common boundary of two bodies, spaces, or phases  -->

<!-- https://www.merriam-webster.com/dictionary/interface -->

<!-- a plane surface regarded as the common boundary of two bodies, -->
<!-- https://www.etymonline.com/word/interface -->

<!-- We've decided that our UI is going to be between an application and a user.  -->

To put it all together, a user interface is a plane surface that forms the common boundary between a user and an application. The next step is to figure how a user interface will mediate between the application and the user. We would prefer a functional design, so deciding how a user interface will mediate comes down to defining what data will be interchanged between the user and the application.


**User -> Application**, the user will use the mouse, keyboard, scroll wheel, trackpad, and other input devices to generate events. These **events** will be interpreted by the user interface and generate **intentions** for our application to act on.

**Application -> User** the application will provide some information about its internal state that will be presented to the user. It's the user interface's job to figure out the best way to present the state of the program to the user.

User interfaces have several options for communicating with the user. Generally, the most popular options are graphics (via a monitor) and sound. While there are other options, these are by far the most popular and currently, membrane solely focuses on graphics as a means of communicating with the user. However, almost all the principles outlined below could be applied to another output device and membrane will be extended to support them in the future.


# The Functional UI Model

The Functional UI Model defines a user interface as the combination of the following two pure functions:
    
1. **Event Function** - a pure function which receives the application state and an event and returns data specifying the user's intent (eg. add a new todo item to the todo list). This facilitates communication from the user to the application.
2. **View Function** - a pure function which receives the relevant application state as an argument and returns data specifying _what_ to draw (how to draw the data will be provided elsewhere). This facilitates communication from the application to the user.

These two functions are the building blocks of user interfaces. As a functional UI library, the main goal of membrane is to make it easy to construct these two functions. 

Some important features of the Function UI Model are:
1. **Universal** - The combination of these two function can describe any possible interface
2. **Composable** - A complicated user interface can be defined in terms of the composition of many simpler user interfaces.
3. **Platform agnostic** - Nothing about this model is specific to membrane, clojure, the jvm, or an operating system.

The real trick is gather the necessary tips, tricks, and techniques necessary to build the complex, feature rich user interfaces that users have come to expect just by defining and composing view and event functions. The number one rule of the Functional UI Model is that the event and view functions are pure and should avoid side effects and global state. Most of the benefits of the model derive from the purity of these functions and introducing impurities is the easiest way to forfeit the benefits of the Functional UI Model.

The view and event functions are low level and generally won't be implemented directly. Libraries can and should build abstractions in terms of the view and event functions that are easier to work with. The real power comes from using this model at the bottom. It means you can replace and interoperate with any components defined using the Function UI Model.

We'll talk more about how to compose user interfaces in future posts, but a key feature of the Functional UI Model is that it allows us to talk about how to compare, replace, substitute, and inspect user interfaces. Crucially, it even allows us to take user interfaces defined using _different_ UI frameworks and make them play well together.

Simply declaring that user interfaces should be built using pure functions may seem like cheating. What about timers, network connections, databases and all the rest? Pushing all the icky bits out of the part of the code labeled "user interface" and relabeling it the "application" doesn't really change anything... Or does it? It's important for user interfaces to connect to the real world, but we'll have to build out our conceptual framework a little more before we tackle that problem. This post will ignore that issue for now, but we will come back to it in a future post. 

Defining user interfaces as the composition of a view and an event function also highlights that at the end of the day, that's what a user interface is all about. It's so easy to get wrapped up in the day to day that we fail to consider the essence of what we're trying to accomplish.


## Definitions

Most discussions about user interface programming use words like view, event, component, render, display, and others loosely. Loose definitions make it difficult to have nuanced conversations about design decisions. Similar to how having crisp definitions for "concurrency" and "parallelism" streamline and clarify discussions around multi-threaded programming, having clear, descriptive vocabulary for user interface concepts is important for discussing UI design.

The goal for membrane is to use these words more precisely (at least within the context of membrane documentation). Generally, definitions are meant to clarify and adhere to common usage{{footnote}}The etymology of "display" would be great to use instead of render, but the common usage of display makes it a poor fit{{/footnote}}, but there are only so many good words ¯\_(ツ)_/¯.


**Event**: Data representing the actions of a user. Examples of events are mouse clicks, and key presses from a keyboard.

**Intent**: Data representing a user intent. Examples of user intents are "delete a todo list item", "open a document", "navigate to a URL".

**Effect**: The carrying out of an intent.

**Event Handler**: A pure function of an **Event** to **Intents**.

**View**: Data describing what to present to the user.

**Render** or **View function**: to produce or return a view from application data.

**Draw**: To turn a view into pixels, typically through side effects that can be displayed directly.

## Platform toolkit

To discuss some of the challenges in user interface programming, we have to talk about some of the components and systems underneath the UI frameworks we use directly. Informally, we'll be calling the environment that provides the event and graphics models the **platform toolkit**. Examples include the browser, Swing, JavaFX, and UIKit. Ideally, these toolkits would only provide a means to receive user events and a sane way to draw graphics. State management would be handled by a separate library or framework.

We don't want to ignore or bypass toolkits altogether, but we do want to be thoughtful about the best way to interoperate with them. Toolkits can offer performance, functionality, and access to existing ecosystems of working code. Membrane is designed to be hosted by a platform toolkit. Membrane strives to leverage platform facilities where it makes sense and substitute its own functionality where it doesn't.

## Graphic and Events

I know what you're thinking. _My_ UI framework has fully functional event handlers and views. It uses interceptors, first class mutations, reactions, subscriptions, and whatzits with no side effects. That's almost true, but almost every UI library uses the event and graphics model provided by the underlying platform "as is" without considering the associated baggage. These platform toolkits are fundamentally based on an object oriented model and have a deep impedance mismatch with idiomatic clojure code and functional programming generally.

React was such a breath of fresh air compared to the patterns that came before that we stopped asking questions about the stack underneath.
- Is the underlying graphics model a good one?
- Is the underlying event model a good one?

![Tip of the Iceberg](Tip-Of-The-Iceberg.png)

Some of the current challenges in UI programming:
* UI components are opaque
* No generic manipulation
* Any thing that touches the UI must run on the main thread
* Components from different frameworks don't compose
* Often, components from the same framework don't compose
* Testing user interfaces is cumbersome, highly manual, and/or ineffective
* UI manipulation on the "inside" of components rather than at the edges is common
* Heavy reliance on global state
* UI workflows are highly platform dependent
  * taking an iOS app and porting it to the web or Android isn't just a rewrite, it also requires a completely different set of tools, languages, and libraries{{footnote}}I know web browsers run on almost every type of device, but I consider web browsers to be their own platform. The ubiquity of the web shows that users interfaces don't need to be coupled to a single platform. However, the web is inextricably coupled to html/css/javascript.{{/footnote}}

React is great, but does it really free us from the grip of the underlying OO platform toolkits? The above challenges are all symptoms of a system that is not based on values. We're not going to spend any time rehashing the [value of values](https://www.infoq.com/presentations/Value-Values/) other than to say we should apply the same principles to the full UI stack. Acknowledging and alleviating the limitations imposed by the underlying toolkits opens up new opportunities for UI libraries to innovate.

> Because the problem with object-oriented languages is they’ve got all this implicit environment that they carry around with them. You wanted a banana but what you got was a gorilla holding the banana and the entire jungle.
> {{blockquote-footer}}Joe Armstrong{{footnote}}Source: [Coders at work](http://codersatwork.com/). This book is great.{{/footnote}}{{/blockquote-footer}}

The impedance mismatch between functional code and the underlying OO based toolkits leads to hacks, workarounds, and limits reusability. We'll briefly cover a few examples to give a flavor of the consequences. The examples below refer to how things work in the browser, but it's the same story with different names if you're using Swing, UIKit, JavaFX, etc.

### Events

It's hard to find examples that are easy to explain. The problems caused by having a baked in OO event model are usually pretty easy to workaround in small applications. It's when working on more complex application where the weight of incidental complexity starts to crush you.

#### What happens?

Given a UI component, what happens when when the user clicks at mouse position `(x,y)`? This isn't a trick question, nor should it be. It's one of the most basic answers a user interface should be able to provide. The problem is that when using any of the available platform toolkits, event handlers have no return value and are only a means of emitting side effects. Further, toolkits like the browser use stateful flow control like `.stopPropagation` and `.preventDefault`. The statefulness of event handlers ruins composition. 

#### Default Behavior

When an event comes in, it gets processed through a gauntlet of default behavior before application code like react sees it. The event model bakes in hit detection, scrolling, text cursor management, focus, hover state, `<img/>` src loading, text selection, and more. Default behavior that is consistent across applications is a really good default, but there are often times when tweaking, augmenting, or preventing default behavior is desired. The most common example of altering default behavior is react's [controlled components](https://reactjs.org/docs/forms.html#controlled-components), but that only covers a few cases. You can completely override default behavior using `.stopPropagation` and `.preventDefault`, but these imperative methods are sledge hammers compared to their functional counterparts. They also fail to address the most typical use case which is augmenting or tweaking default behavior. Unfortunately, `.stopPropagation` and `.preventDefault` are all or nothing. There's no way to retain most of the default behavior with some application specific tweaks.

An example of a library that requires a mess of workarounds due to the baked in event model is [CodeMirror](https://codemirror.net/) {{footnote}}[Code Mirror example 1](https://github.com/codemirror/CodeMirror/blob/0b64369b54503150f054abda50359c76f00f484f/src/edit/mouse_events.js#L400){{/footnote}} {{footnote}}[Code Mirror Example 2](https://github.com/codemirror/CodeMirror/blob/c41dec13675da74fb575006a502d7daee6abdafe/src/input/ContentEditableInput.js#L250){{/footnote}} {{footnote}}[Code Mirror Example 3](https://github.com/codemirror/CodeMirror/blob/c41dec13675da74fb575006a502d7daee6abdafe/src/input/ContentEditableInput.js#L94){{/footnote}} {{footnote}}[Code Mirror Example 4](https://github.com/codemirror/CodeMirror/blob/b5ce22f1e350431adaefbad40cbfc54dbfdb1c77/src/input/input.js#L122){{/footnote}}. 

<!-- #### Testing and Automation -->

<!-- Testing and automation of UI code is fairly uncommon and part of the reason is that the event model is rooted deep within the OO toolkits that libraries like react rely on. It's not about worrying if the event system provided by the underlying toolkit is working as intended, it's about being able to take user interfaces apart and test components of an application in isolation. Not only is driving one of these systems a huge pain, but inspecting a user interface is also unnecessarily difficult. -->

<!-- Imagine having a test that ensures it's always possible to get back to an app's home screen without getting stuck. -->


#### Global State

As a real example of how a baked in event model contorts UI libraries, let's take a look a look at re-frame [Global State Issue #137](https://github.com/day8/re-frame/issues/137).

The issue is fairly long, but I'll try to summarize. Basically, re-frame's state management (ie. subscriptions) and event handling (ie. `dispatch`) rely on global state. It would be great to be able to have multiple re-frame instances running on the same page in straightforward way.

> Except, that means you have to pass frame down through the entire function call tree which is arduous. Really arduous. There's something completely delicious and simple about the use of global dispatch and subscribe, even though it is clearly evil in some ways.

It would be really straightforward for the re-frame library to solve this issue if the event model in the browser was pluggable, but since the event model is baked in, it adds a mountain of incidental complexity. <!-- Conversely, since the event model in `membrane` is pluggable, it would be an easy fix {{footnote}}You can already try membrane's experimental [re-frame integration](https://github.com/phronmophobic/membrane-re-frame-example). Multiple re-frame instances still doesn't quite work since in addition to parameterizing `dispatch` and `subscribe`, there's a few other places `re-frame` relies on global state.{{/footnote}} -->


### Graphics

Displaying program state is a one of the main responsibilities of a user interface. Unfortunately, the graphical building blocks from platform toolkits don't match the building blocks used by designers. You're not going to find a `<div/>` tool in Photoshop or Illustrator. The browser has reasonable support for text and images, but many of the the common elements a designer would use either don't exist or are awkward to implement.

Not only is there a mismatch between the graphical elements used by designers and programmers, it's very common for UI code to use graphical elements that inextricably couple state and events. As an example, consider a checkbox. Is it possible to draw just the checkbox without any of the associated behavior? Are the graphical elements that produce the checkbox able to be extracted and inspected? As with just about every other simple task on the web, the answer is probably "sorta" with a dozen Stack Overflow posts explaining a handful of options that depend on various subtlety different circumstances. This is not a great place to be.

The building blocks we're using to specify UI graphics are unnecessarily complicated. At the bottom, we have `<div/>`s, `<span/>`s, `<input/>`s , etc. and none of these can be broken down into simpler pieces. Even just measuring the size of a snippet of HTML requires a ton of global environmental information. When we use complex, stateful constructs at the bottom, it hinders our ability to generically process and manipulate user interfaces. Consequently, tooling and testing for user interfaces suffers.

For example, if someone hands you some hiccup based mark up, `[:div.foobar]`. How do you:
* measure it's bounds?
* draw it to an image?
* determine its background color?
* divine what text will be displayed?{{footnote}}[Using CSS to insert text](https://stackoverflow.com/questions/2741312/using-css-to-insert-text)

The answers to these questions should be trivial, but they're not. 

<!-- While it may be a necessary evil to keep building layers on top of this mess in the short term, it's impractical in the long term. -->

If you're using react or any library built on top of react, then the options for inspecting or manipulating the render output of a component are limited to:
* adding, removing, editing child elements
* adding removing, editing html attributes
* editing the html tag name for an element

That's it. In all fairness, these are the most important operations. It's possible to build user interfaces with just these operations and many developers have, but we can do better.

# Recap

A user interface is a plane surface that forms the common boundary between a user and an application. A fully functional user interface defines an interface using two pure functions, the the event function and the view function. The goal is build complex user interfaces by composing simpler user interfaces.

# To Be Continued...

Stay tuned for more posts on the design of membrane where we answer questions like:
* Where did all the state go?
* How do I implement a checkbox?
* How do I build stateful components?
* How does focus work?
* Why has UI programming been so platform dependent?
* Why are user interfaces highly coupled to the applications they drive?
* How do I compose complex user interfaces from simpler user interfaces?
* How can I get this to work on mobile?
* What about performance?
* What tools and workflows should be supported?
* How can we give designers better control over the output of their work?

# Footnotes

{{footnotes/}}

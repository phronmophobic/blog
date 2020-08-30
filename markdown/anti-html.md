
It's time we start moving away from specifying our user interfaces in html{{footnote}}To the annoyance pedants, I use "html" when I'm actually referring to html + related css/js.{{/footnote}}. This idea may seem crazy since the web is one of the best, if not _the_ best platform for distributing software. Below is an overview of how html degrades the code, graphical design, and portability of our user interfaces and provides a plan for ditching the venerable `<div/>`.


# What's the value?

Selling the value of ditching html is difficult because specifying uis in html makes most tasks harder, but it rarely makes anything impossible. The truth is that switching to another format besides html won't let you do anything you couldn't already do (given enough time and resources). Anything you could do in another format, you could technically do if you were using html.

However, **html is a tax on every workflow it touches.** For reasons described below, using html is cumbersome and awkward. Fortunately, you can avoid the html tax by defining your user interfaces with a different format. Avoiding the tax doesn't directly let you do anything couldn't be doing anyways, but it enables many enhancements to your workflow that wouldn't be worth the investment otherwise. We'll get to all the exciting opportunities that await us in the tax free zone, but first let's start from the beginning and explain why html is unsuitable.

# What is a User Interace?

Every once in a while, it's worth it to take a step back and think about what we're actually trying to accomplish. We should periodically pause and ask ourselves big picture questions to make sure we're on the right track.

- What is a user interface?
- What are the basic building blocks of user interfaces?
- What should the basic building blocks be?
- What methods, processes, and tools do we need to construct complicated user interfaces without making an unintelligble mess?

_thinking space..._

{{contemplation-break/}}

Answering "What is a user interface?" could be a post all on its own, but for now, we'll start with this brief (and oversimplified) definition.

The primary duties of a user interface are:

1. Presenting the state of a program
2. Interpreting user input

For this post, we'll further simplify our definition to focus on
1. **Graphics**: visually displaying the state of a program
2. **Events**: user input from the mouse and keyboard.
3. **UI State Management**: state unrelated to our application model necessary for handling graphics and events (eg. keeping track of which element has focus).

# Graphics

## What is a div?

The easiest way to show that html is a bad fit for specifying user interfaces graphics is that UI designers don't work or design in terms of html. Even when an interface is being designed specifically for the web, it's rare for designers to use html elements as building blocks. Typically, the building blocks consist of text, shapes, groups, and images. In addition to html not being a natural representation, html has poor, ad hoc support for shapes. It's not impossible to embed shapes, but it's awkward enough that it's not very common.

Since designers don't work in terms of html elements, a considerable amount of effort is spent reimplementing ui designs with html. Since html maps poorly to UI designs, small changes in design can often mean disproportionaly large changes in the html representation. Small design tweaks can take a considerable amount of work to reimplement. This is a huge waste of time.

Flexbox has improved the ease of reimplementing ui designs into html/css, but html still remains an unnatural fit for specifying ui designs. Most graphic design tools don't even bother to export to html because html's inadequacies mean that it's difficult to figure out what an export to html option should produce.

## HTML makes poor data

It's unclear what a snippet of html conveys. To get any definitive meaning out of html requires firing up a full web browser engine, opening the web page, and "seeing what happens". Given a snippet of html (ignoring that the css is often defined elsewhere), it can be difficult to reliably guess what pixels will pop out the other side. 

This is further complicated by the fact that rendering a web page isn't just about html, it's also about the css and javascript engines **directly** attached the html renderer. Even simply producing a png image from html requires a full html and css engine. If you wanted to accomplish something even slightly more complex, like measuring the sizes of sub-elements in some html, you're going to have run it through the engine and then query the DOM by passing js to the attached javascript engine.

{{tableflip/}}

> So what if you need a full browser engine to poke at some html? How else what you do it?

Data should be semantically transparent. A data format should have meaning outside of any specific implementation. It shouldn't mean "whatever shows up in Chrome today". There's a lot of times when you'd like to generically debug, profile, manipulate, and inspect a ui component, but it's so cumbersome that that we don't do it.

## HTML is lossy

We haven't talked about what kind of data format we should be using yet, but whichever format we choose, we want it to be lossless. In other words, we should be able to convert our graphics representation to another similar format and back again without any loss of information. So many workflows struggle because their uis are defined in html and html is awful for generic processing. Sure, it's easy to manipulate the html and produce other valid html, but the meaning of the html is opaque. For example, for a sane graphical format, you could automatically validate a ui component against a style guide that specifies colors, paddings, spacing, and minimum sizes for interactable elements. When your interface is encoded in html, it's very difficult.

We have all these fancy, functional frameworks that let you build interfaces out of pure functions mapping application data to html elements, but the whole enterprise is undermined by the fact that DOM elements are basically useless as data unless the DOM elements are directly handed to the browser. Building higher level tooling on top of html is really cumbersome. Once you end up at html, that's it. There's no going back. The only useful thing you can do is hand it to a browser. Common practice is to start with html which means we're really shooting ourselves in the {{shoot-in-the-foots/}}.


# Events

Event handling in the browser is fairly reasonable. Any idiosyncryncies can be papered over by libraries. However, since the browser's event system is necessarily coupled to the graphical output of html, any tooling or testing that relies on events will require a full browser engine. If want to know what happens when the user clicks the mouse at the coordinate `(x,y)`, then you need to know which elements contain those `(x,y)` which requires rendering html. If you've ever tried to set up automated ui testing like selenium, then you have paid the html tax on event handling. 

# UI State Management

## Stateful HTML elements

There are a plethora of state management libraries for web apps. These libraries help you tame the necessary complexity of the state needed to build user interfaces. The html tax comes due whenever you use html elements that try to manage state for you. When the browser manages UI state for you, then you lose the benefits of whichever state management library you chose. Techniques like React's controlled components{{footnote}}[React Controlled Components](https://reactjs.org/docs/forms.html#controlled-components){{/footnote}} are generally effective, but only works for certain kinds of state. 

Notable exceptions include:
- scrolling
- text cursor
- text selection
- focus
- hover state. 

Hopefully, your interface is happy with the default state management because if not, you'll need to resort to a gamut of hacky work arounds.{{footnote}}[Code Mirror example 1](https://github.com/codemirror/CodeMirror/blob/0b64369b54503150f054abda50359c76f00f484f/src/edit/mouse_events.js#L400){{/footnote}} {{footnote}}[Code Mirror Example 2](https://github.com/codemirror/CodeMirror/blob/c41dec13675da74fb575006a502d7daee6abdafe/src/input/ContentEditableInput.js#L250){{/footnote}} {{footnote}}[Code Mirror Example 3](https://github.com/codemirror/CodeMirror/blob/c41dec13675da74fb575006a502d7daee6abdafe/src/input/ContentEditableInput.js#L94){{/footnote}} {{footnote}}[Code Mirror Example 4](https://github.com/codemirror/CodeMirror/blob/b5ce22f1e350431adaefbad40cbfc54dbfdb1c77/src/input/input.js#L122){{/footnote}}

# The payoff

In [Painting With Code](https://airbnb.design/painting-with-code/), airbnb's design team builds a new design tool that increases their productivity. It's buried in the middle of the story, but if you look carefully, you'll see that by using an appropriate ui representation, they unlocked massive potential.

> A breakthrough came when Airbnb engineer Leland Richardson proposed using React Native-style components. `<View>`, `<Text>`, etc. are the basic units of composition for design systems, so that semantically linked our Sketch components with their production counterparts. More excitingly, with Leland’s React Primitives project we could render real React components to Sketch, the browser, and our phones at the same time.

> We began the project to reduce the time it takes to generate static assets. But through exploring its edges and adjacent possibilities, we’re unearthing exciting and novel ways of interacting with design systems in Sketch. Many tasks that were previously unfeasible, required massive human input, or relied on sketchy (sorry) plugins are now enabled with the same code that our engineers are writing day-to-day.


### Increased performance

When we minify and/or compile our code into javascript, we produce javascript that has the same semantics as our original code, but with a reduced payload and faster runtime. It's a win for the developer and the user. There's no reason we can't do the same for html. 


## Easier testing



## Better tools

https://origami.design/


https://origami.design/documentation/workflow/Components.html
https://medium.com/knock-knock-games/building-out-an-html5-game-ui-pipeline-with-figma-9abf99f31591
Inteface builder

## Access to More Platforms

HTML really works in the browser. What you'll find if you use more natural primitives, is that you can target more platforms (like iOS, android, webgl, desktop, etc). I'm not advocating that you shouldn't levarage the unique capabilities of each platform when creating the user interface, but that we shouldn't unnecessarily limit ourselves to only stuff that works in the browser.



## Increased productivity

When your model fits your domain, it requires less code and fewer acrobatics to get the desired results. 

# What makes a good format?

When assessing which format is appropriate, we should be starting 

In 1996, the W3C published a [list of requirements](https://www.w3.org/Graphics/ScalableReq.html) when assessing proposals for a vector graphics format. Despite its age, it's actually a very good starting point.

> **Open specification**
> 
> - Open specification - not subject to sudden change by a single vendor. Preferably submitted to an open forum such as W3C, IETF RFC, etc.
> - Ready availability to the casual implementor is desirable.
> - Extensible to cope with changing requirements
> - Widely implemented; at minimum, proof of concept implementation.
> - Reference code desirable
> - Lack of subset problems, incompatible generator/reader sets. 
> 
> **Graphical facilities**
> 
> - Vector graphics - line segments, closed filled shapes
> - Curved elements
> - Text, ISO10646 repertoire, font selection
> - Truecolor mode - not restricted to indexed color
> - Transparency (alpha)
> - Layering, stencilling/masking
> - Control of line termination and mitring
> - Levels of detail
> - Include raster data 

The requirements also specify some requirements for interaction. However, we have to be really careful about glomming an interaction/event model onto a graphics format. User interfaces do require an event model, but graphics don't. We should be able to extract just the graphics or just the interaction part of our user interface.

Another source of inspiration for our desired format is the [llvm IR (Intermediate Representation)](https://llvm.org/docs/LangRef.html). I want to make it clear that the llvm IR is targetted at representing programs and is not a suitable representation for user interfaces. Its role in the compilation process does share some similarities with the intermediate representation we would like to have for our user interfaces. It's this similarity that can help us cheat off their work a bit. 



Here's what some analalogous requirements might be for a UI format:

> An intermediate representation (IR) is the data structure used internally to represent a user interface
- An IR is designed to be conducive for further processing, such as optimization and translation.
- A "good" IR must be accurate – capable of representing the user interfaces without loss of information
– Independent of any particular source or target language.

Nice to haves
- basis in mathematical or computer science theory amenable to rigorous analysis and formal proof

Unknown Questions
- 

Closer to a graphic design format than to HTML and may include support for scaffolding{{footnote}}Scaffolding are the part of a source work that supports development, but has no direct impact on the final output. For example, comments in source code or guides in design files.{{/footnote}}.

Some other qualities we would like in our format:

- can easily translate from graphic design program formats to this format
- can easily translate from this format to other formats (especially html)
  
  Choosing a file format is an important decision, but using a file format that translates to and from other analagous formats easily significantly reduces the risk of choosing a format. More importantly, any format that translates well to and from other formats will likely have the following properties:
- semantically transparent
- appropriately abstract: not tied to a specific implementation, language, or runtime








{{footnote}}[W3C Scalable Graphics Requirements](https://www.w3.org/Graphics/ScalableReq.html){{/footnote}}

{{footnote}}[Nile Programming Language](https://raw.githubusercontent.com/wiki/damelang/nile/socal.pdf){{/footnote}}







# The escape plan

The good news is that we already have a playbook for ditching HTML. It's not the first time we were forced to use a technology that was a poor fit for the work. That's right! Javascript. The javascript that gets shipped to the browser isn't the same code we work with in development. In some cases, we're not even developing in javascript. We code using a more comfortable language or dialect and then transpile, minify, and repackage the source into javascript that produces the same behavior, but with better performance and a smaller payload. We've increased productivity and performance by improving our js options and we can do the same with HTML. We get to work in a medium that matches are mental model and we still get to leverage the ubiquity of the web for software distribution.

The goal isn't to abandon the web browser, it's to work with a more natural representation and use an automatic process to generate HTML that produces the same result. The crazy thing is that one of the main objections to working directly in HTML and not producing HTML from some other graphical specification is a dissatisfaction with the quality of the generated HTML. In other words, html has such has such an impedance mismatch with the way we conceptualize UI that it's difficult to produce efficient html that solves our problem. The impedance mismatch is the reason we shouldn't be using html in the first place! Fortunately, the libraries, tooling, and techniques needed for transpiling have come a long way and building a quality transpiler is very doable.

We're already doing this {{footnote}}[react-sketchapp](http://airbnb.io/react-sketchapp/docs/guides/universal-rendering.html){{/footnote}}
{{footnote}} [react-primitives](https://github.com/lelandrichardson/react-primitives){{/footnote}}

{{footnotes/}}


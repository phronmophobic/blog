{{table-of-contents/}}

> Prudence, indeed, will dictate that data formats long established should not be changed for light and transient causes. Accordingly all experience hath shown, that programmers are more disposed to suffer, while evils are sufferable, than to right themselves by abolishing the forms to which they are accustomed.
> {{blockquote-footer}}Thomas Json{{/blockquote-footer}}

# Introduction

It's time we start moving away from specifying our user interfaces in HTML. This idea may seem crazy since the web is one of the best, if not _the_ best platform for distributing software. Below is an overview of how HTML degrades the quality of our user interfaces and provides a not so crazy plan for ditching the venerable `<div/>`.

<!-- {{footnote}}To the annoyance pedants, I use "HTML" when I'm actually referring to HTML + related css/js.{{/footnote}} -->


# The Problem



Convincing folks to abandon HTML is difficult because specifying user interfaces in HTML makes most tasks harder, but it rarely makes anything impossible. The truth is that switching to another format besides HTML won't let you do anything you couldn't already do (given enough time and resources). Anything you could do in another format, you could technically do in HTML.

However, **HTML is a tax on every workflow it touches.** For reasons described below, using HTML is cumbersome and awkward. Fortunately, you can avoid the HTML tax by defining your user interfaces in a different format. Evading the HTML tax doesn't directly let you do anything you couldn't be doing anyways, but it enables many enhancements to your workflow that wouldn't be worth the investment otherwise. We'll get to all the exciting opportunities that await us in the tax free zone, but first let's start from the beginning and explain why HTML is unsuitable.


# What is a User Interface?

Every once in a while, it's worth it to take a step back to think about what we're actually trying to accomplish. We should periodically pause and ask ourselves big picture questions to make sure we're on the right track.

- What is a user interface?
- What are the basic building blocks of user interfaces?
- What should the basic building blocks be?
- What methods, processes, and tools do we need to build complex complicated user interfaces without making an unintelligible mess?
<!-- _thinking space..._ -->
<!-- {{contemplation-break/}} -->

Answering "What is a user interface?" could be a post all on its own, but for now, we'll start with this brief (and oversimplified) definition.

The primary duties of a user interface are:

1. Presenting the state of a program
2. Interpreting user input

For this post, we'll further simplify our definition to focus on
1. **Graphics**: visually displaying the state of a program
2. **Events**: user input from the mouse and keyboard.
3. **UI State Management**: state unrelated to our application model necessary for handling graphics and events (eg. keeping track of which element has focus).

# What is HTML?

At 1,297 pages, the [HTML Living Standard](https://html.spec.whatwg.org/print.pdf) is expansive. In the next sections, we're going to beat up a bit on HTML and it's important to clarify which parts of HTML are problematic. HTML isn't all bad and parts of it are straightforward, pragmatic, and endearing. To really tease apart the good from the bad, we'll have to retread some definitions we all already know.

> Hypertext Markup Language (HTML) is the standard markup language for documents designed to be displayed in a web browser. It can be assisted by technologies such as Cascading Style Sheets (CSS) and scripting languages such as JavaScript. 
> {{blockquote-footer}}[HTML - Wikipedia](https://en.wikipedia.org/wiki/HTML){{/blockquote-footer}}

Logically, HTML is a tree of HTML elements. Each element has a tag name and optional attributes. Depending on the tag type, an element may also have other HTML elements as children. HTML markup contains special elements like `<div>` and `<span>` that the browser interprets to display the HTML document and provide interactivity. 

**For the rest of this post**, when we refer to HTML, we're really referring to the special meaning given to the special elements. Further, we're really only focusing on the special elements that affect the graphics, event model, and state management of user interfaces targeting the web browser.

While we're clearing the air, I'd like to emphasize that there is absolutely nothing wrong with defining a user interface in terms of a tree of tagged elements and attributes. The issues brought up in this post focus solely on the semantics associated with the special elements imparted by the browser. Practically speaking, that means that if you're using a ui framework that doesn't care whether you're using `<div>`s, `<View>`s, or any other specific tag type, then your framework is in harmony with the views of this post. Fortunately, most popular frameworks like React, re-frame, Angular, or any framework that allows user defined components are philosophically compatible.

<!-- > HTML uses "markup" to annotate text, images, and other content for display in a Web browser.  -->
<!-- > {{blockquote-footer}}[HTML - MDN](https://developer.mozilla.org/en-US/docs/Web/HTML){{/blockquote-footer}} -->


<!-- > HTML markup includes special "elements" such as `<head>`, `<title>`, `<body>`, `<header>`, `<footer>`, `<article>`, `<section>`, `<p>`, `<div>`, `<span>`, `<img>`, `<aside>`, `<audio>`, `<canvas>`, `<datalist>`, `<details>`, `<embed>`, `<nav>`, `<output>`, `<progress>`, `<video>`, `<ul>`, `<ol>`, `<li>` and many others. -->
<!-- > {{blockquote-footer}}[MDN HMTL Reference](https://developer.mozilla.org/en-US/docs/Web/HTML){{/blockquote-footer}} -->

<!-- ![asdfd](https://media.prod.mdn.mozit.cloud/attachments/2012/07/09/3704/07b3e5bb546840a09bb35d45b36009a6/Content_categories_venn.png) -->

<!-- https://html.spec.whatwg.org/multipage/indices.html#element-content-categories -->



## Graphics

### What is a div?

The easiest way to show that HTML is a bad fit for specifying user interfaces is that UI designers don't work or design in terms of HTML. Even when an interface is being designed specifically for the web, it's rare for designers to use HTML elements as building blocks. Typically, the building blocks consist of text, shapes, groups, and images. In addition to HTML not being a natural representation, HTML has poor, ad hoc support for shapes. It's not impossible to embed shapes, but it's awkward enough that it's not very common.

Since designers don't work in terms of HTML elements, a considerable amount of effort is spent reimplementing UI designs in HTML. Since HTML maps poorly to UI designs, small changes in design can often mean disproportionately large changes in the HTML representation. Small design tweaks can take a considerable amount of work to reimplement. This is a huge waste of time.

Flexbox and other improvements have eased the process of reimplementing UI designs into HTML/CSS, but HTML still remains an unnatural fit for specifying UI designs. Most graphic design tools don't even bother to export HTML because HTML's inadequacies mean that it's difficult to figure out what an "Export to HTML" option should produce.



### HTML makes poor data

It's unclear what a snippet of HTML conveys. To get any definitive meaning out of HTML requires firing up a full web browser, opening the web page, and "seeing what happens". Given a snippet of HTML, it can be difficult to reliably guess what pixels will pop out the other side. 

This is further complicated by the fact that rendering a web page isn't just about HTML, it's also about the CSS and JavaScript engines **directly** attached to the HTML renderer. Even simply producing a png image from HTML requires a full HTML and CSS engine. If you wanted to accomplish something even slightly more complex, like measuring the sizes of sub-elements in some HTML, you're going to have to run it through the engine and then query the DOM by passing js to the attached JavaScript engine.

{{tableflip/}}

> So what if you need a full browser engine to poke at some HTML? How else what you do it?

Data should be semantically transparent. A data format should have meaning outside of any specific implementation. It shouldn't mean "whatever shows up in Chrome today". There's a lot of times when we'd like to generically debug, profile, manipulate, and inspect a UI component, but it's so cumbersome that that we don't do it.

<!-- https://www.figma.com/blog/behind-the-feature-shadow-spread/ -->

It may seem like I'm being a bit unfair and I wish that were true, but the elusive nature of HTML is baked into the living standard.

> User agents are not required to present HTML documents in any particular way. However, this section provides a set of suggestions for rendering HTML documents that, if followed, are likely to lead to a user experience that closely resembles the experience intended by the documents' authors.
> {{blockquote-footer}}[The HTML Living Standard](https://html.spec.whatwg.org/multipage/rendering.html#rendering){{/blockquote-footer}}

### HTML is lossy

We haven't talked about what kind of data format we should be using yet, but whichever format we choose, we want it to be lossless. In other words, we should be able to convert our graphics representation to another similar format and back again without any loss of information. So many workflows struggle because their UIs are defined in HTML and HTML is awful for generic processing. Sure, it's easy to manipulate HTML and produce other valid HTML, but the semantic meaning of HTML is opaque. For example, with a sane graphical format you could automatically validate a UI component against a style guide that specifies colors, paddings, spacing, and minimum sizes for interactive elements. If your interface is encoded in HTML, it's very difficult.

We have all these fancy, functional frameworks that let you build interfaces out of pure functions mapping application data to HTML elements, but the whole enterprise is undermined by the fact that DOM elements are basically useless as data unless the DOM elements are directly handed to the browser. Building higher level tooling on top of HTML is really cumbersome. Once you end up with HTML, that's it. There's no going back. The only useful thing you can do is hand it to a browser. Common practice is to start with HTML which means we're really shooting ourselves in the {{shoot-in-the-foots/}}.

## Events

Event handling in the browser is fairly reasonable. Any idiosyncrasies can be papered over by libraries. However, since the browser's event system is necessarily coupled to the graphical output of HTML, any tooling or testing that relies on events will require a full browser engine. If you want to know what happens when the user clicks the mouse at the coordinate `(x,y)`, then you need to know which elements contain `(x,y)` which requires rendering HTML. If you've ever tried to set up automated UI testing like Selenium, then you have paid the HTML tax on event handling.

## UI State Management

There are a plethora of state management libraries for web apps. These libraries help tame the necessary complexity of the state needed to build user interfaces. The HTML tax comes due whenever you use HTML elements that try to manage state for you. When the browser manages UI state for you, you lose the benefits of whichever state management library you chose. Techniques like React's [controlled components](https://reactjs.org/docs/forms.html#controlled-components) are generally effective, but only works for certain kinds of state. 

Notable exceptions include:
- scrolling
- text cursor
- text selection
- focus
- hover state
- `<img/>` src loading

Hopefully, your interface is happy using the builtin behavior because if not, you'll need to resort to a gamut of hacky workarounds.{{footnote}}[Code Mirror example 1](https://github.com/codemirror/CodeMirror/blob/0b64369b54503150f054abda50359c76f00f484f/src/edit/mouse_events.js#L400){{/footnote}} {{footnote}}[Code Mirror Example 2](https://github.com/codemirror/CodeMirror/blob/c41dec13675da74fb575006a502d7daee6abdafe/src/input/ContentEditableInput.js#L250){{/footnote}} {{footnote}}[Code Mirror Example 3](https://github.com/codemirror/CodeMirror/blob/c41dec13675da74fb575006a502d7daee6abdafe/src/input/ContentEditableInput.js#L94){{/footnote}} {{footnote}}[Code Mirror Example 4](https://github.com/codemirror/CodeMirror/blob/b5ce22f1e350431adaefbad40cbfc54dbfdb1c77/src/input/input.js#L122){{/footnote}}

# The Escape Plan

The good news is that we already have a playbook for ditching HTML. It's not the first time we've been required to use a technology that was a poor fit. 

JavaScript 👀

The JavaScript that gets shipped to the browser isn't the same code we work with in development. In some cases, we're not even developing in JavaScript. We code using a more comfortable language or dialect and then transpile, minify, and repackage the source into JavaScript that produces the same behavior, but with better performance and a smaller payload. We've increased productivity and performance by expanding our language options and we can do the same with HTML. We can work in a medium that matches our mental model and still leverage the ubiquity of the web for software distribution.

The goal isn't to abandon the web browser. It's to work with a more natural representation and use an automatic process to generate HTML that produces the same result. The crazy thing is that a common objection to generating HTML from another source is a dissatisfaction with the quality of the generated HTML. In other words, HTML has such an impedance mismatch with the way we conceptualize UI that it's difficult to programmatically generate HTML that matches our intention. This impedance mismatch is the reason we shouldn't be using HTML in the first place! Fortunately, the libraries, tooling, and techniques needed for transpiling have come a long way and building a quality transpiler is very doable.

# The Escape

## Painting with Code

Not only is using an alternative UI source representation possible, it's already happening. In [Painting With Code](https://airbnb.design/painting-with-code/), Airbnb's design team builds a new design tool that increases their productivity. It's buried in the middle of the story, but if you read carefully, you'll see that by using an appropriate UI representation, they have unlocked massive potential.

> A breakthrough came when Airbnb engineer Leland Richardson proposed using React Native-style components. `<View>`, `<Text>`, etc. are the basic units of composition for design systems, so that semantically linked our Sketch components with their production counterparts. More excitingly, with Leland’s React Primitives project we could render real React components to Sketch, the browser, and our phones at the same time.

> We began the project to reduce the time it takes to generate static assets. But through exploring its edges and adjacent possibilities, we’re unearthing exciting and novel ways of interacting with design systems in Sketch. Many tasks that were previously unfeasible, required massive human input, or relied on sketchy (sorry) plugins are now enabled with the same code that our engineers are writing day-to-day.

For more, check out [react-sketchapp](http://airbnb.io/react-sketchapp/docs/guides/universal-rendering.html) and [react-primitives](https://github.com/lelandrichardson/react-primitives).

## The Game Industry

If you want to see what life could be like when you don't have to pay the HTML tax, check out what's going on in the game industry. The game industry develops interfaces that are complicated, beautiful, fun, and efficient. The interfaces run natively on multiple platforms, and their designers have direct control over the look and feel of the final product. There's no reason we couldn't be doing the same for web interfaces.
<!-- ## Elm lang -->

# Benefits

> "Data dominates. If you've chosen the right data structures and organized things well, the algorithms will almost always be self-evident. Data structures, not algorithms, are central to programming." {{blockquote-footer}}Rob Pike{{/blockquote-footer}}

## Increased productivity

As an industry, we waste an enormous amount of time reimplementing user interfaces in HTML. Instead, we should be empowering our UI designers with direct control over the output of their work. Requiring UI designers to go through a programmer to fix a pixel is immoral. 

## Better tooling

 If you're using the right format, you can build better tools. Tools like [react-sketchapp](http://airbnb.io/react-sketchapp/docs/guides/universal-rendering.html), [origami](https://origami.design/), [Interface Builder](https://developer.apple.com/xcode/interface-builder/), [Tiled](https://www.mapeditor.org/), and others can help increase productivity and they're only worth it for non HTML source formats. 
 
Chrome devtools is really great, but again, it's directly attached to the web browser. It's so cumbersome to do anything with HTML outside the web browser that more and more tools are getting forced into the web browser itself. We should be using the best tool for the job and not be forcing ourselves into a corner based off the platforms we're targeting.

## Increased performance

When we minify and/or compile our code into JavaScript, we produce JavaScript that has the same semantics as our original code, but with a reduced payload and faster runtime. It's a win for the developer and the user. There's no reason we can't do the same for HTML.

## Accessibility

When we stop hand coding every `<div>` and `<span>`, it makes it easier, not harder, to improve the accessibility of the resulting HTML. Working at a higher level allows us to implement better verification of accessibility. The biggest win of working at a higher level is the ability to generate multiple builds. It would then be possible to have a build target specifically optimized for accessibility.

## Easier testing

Setting up Selenium or other another browser automation tool to test our code is a huge pain. We should be able to test our UI code as simply as we test all of our other code.

## Access to More Platforms

HTML really only works in the browser. If you use more natural primitives, you can target more platforms like iOS, android, webgl, desktop, and more. I'm not advocating that we forgo the unique capabilities of each platform, but we shouldn't unnecessarily limit ourselves to only being able to run within a browser.

# Alternative Formats

If you want to get started today, some options to consider are [react-primitives](https://github.com/lelandrichardson/react-primitives) and svg. These options aren't perfect, but they're all better than HTML in the ways we talked about. 

Ideally, we would like an **intermediate representation** analogous to [llvm IR (Intermediate Representation)](https://llvm.org/docs/LangRef.html) targeted at representing user interfaces. The purpose of the intermediate representation would be to have a common format between source representations from design programs (like sketch, figma, and illustrator) and platform formats (like web/HTML, iOS/UIView, android/View, etc.). The llvm IR is targeted at representing programs and is not a suitable for representing user interfaces, but its utility as part of the compilation pipeline can provide inspiration for what an intermediate representation for specifying user interfaces might look like.

## What makes a good format?

If we're not going to use HTML, our options are:

1. Choosing an existing format while possibly adding some improvements
2. Creating a new format
3. "Fixing" HTML{{footnote}}It seems difficult, but it's entirely possible that there may be a way to define a subset of HTML/CSS that runs as is within the browser, but also addresses the issues raised above{{/footnote}}

If we're considering multiple options, we should start with some criteria to help evaluate which option is the best fit. In 1996, the W3C published a [list of requirements](https://www.w3.org/Graphics/ScalableReq.html) when assessing proposals for a vector graphics format. Despite its age, it's actually a pretty good starting point.

> **Open specification**
> 
> - Open specification - not subject to sudden change by a single vendor. Preferably submitted to an open forum such as W3C, IETF RFC, etc.
> - Ready availability to the casual implementer is desirable.
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

The requirements also specify some requirements for interaction. However, we have to be really careful about glomming an interaction/event model onto a graphics format. User interfaces do require an event model, but graphics don't. We should be able to extract just the graphics or just the interactive part of our user interface.

Additionally, whichever format we choose will be acting as an intermediate representation(IR) between design formats and platform formats. As an intermediate representation, we should also be evaluating the following qualities:

- An IR is designed to be conducive for further processing, such as optimization and translation.
- A "good" IR must be accurate – capable of representing the user interfaces without loss of information
- Independent of any particular source or target language.

Choosing a representation that makes a good IR also has the benefit that it reduces our risk of choosing the wrong format. Since an intermediate representation is already suited for translation, it means we can easily switch to a better format should the need arise.

Since our IR will be used for tooling, we would like to be able to generically manipulate and inspect the format. Transformations and inspection should be achievable without a web browser. Examples of inspection and transformation

* Generically walk elements and sub-elements
* Produce images from the format
* Query layout information of elements and sub-elements
* Determine which event handlers would be triggered by which events

# Conclusion

> I am going to pull a date of the air right now. April 13th, 2002. That is the last day you evaluated your {{square-bracket-left/}}ui data format{{square-bracket-right/}} needs. Is it not? 
> {{blockquote-footer}}Michael Scott{{/blockquote-footer}}

HTML is the default option for writing web apps. We pick HTML not because we've determined that it's the best option, but because it's the easiest way to get started. Picking a ui data format has a huge influence on our development workflows and the quality of our products.

* How much time spent building user interfaces with HTML is spent on incidental complexity?
* Does working in HTML help us focus on domain problems?
* Is HTML great for tooling?
* Do hand picked HTML tags produce the best performance?
* Is HTML easy to maintain and update?
* Are interfaces built in HTML easy to test?

As a data format, HTML isn't going anywhere, but that doesn't mean we should be hand picking every `<div/>` and `<span/>`. As we continue to build bigger and better browser apps, we should evaluate whether HTML really is the best source format for user interfaces and whether that will continue to be true in the future.

# Footnotes

{{footnotes/}}


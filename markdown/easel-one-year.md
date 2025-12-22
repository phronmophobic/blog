{{table-of-contents/}}

Posted: December 20, 2025

## What is Easel?

Easel is an IDE written in Clojure that I've been working on for the last year. See it in action <https://youtu.be/sceGtaNHxcM>.

Easel itself is relatively focused. It provides a model for specifying tools and a simple user interface for arranging tools in the same window. The bulk of the functionality you would expect from an IDE is provided via simple, modular tools that can share data with each other. Tools can also be used independently outside of Easel. Think of it as an IDE as a library. Some examples of tools are text editing, terminal emulation, data inspection, web browsing and more.

## Why Easel?

Imagine it's 10 years from now and you're working in the IDE of your dreams. What would that look like? For me, it would be something like the following:
- The IDE can be extended at runtime using Clojure
- Strong support for REPL-driven development (ie. work on your program while it's running)
- Direct access to data. Lots and lots of tools to visualize, summarize, search, transform, and interact with data
- 2d, 3d graphics
- Data orientation over text orientation
- Provide a platform for experimentation
- Situated tools that work together by sharing data
- IDE as a library
- User oriented
- Open source
- Full access to the capabilities of the hardware

### The Hard Problems

You may notice that none of the items on our wishlist are revolutionary. In fact, all of them exist as features in popular IDEs from today and yesteryear. However, there isn't an IDE that has all of these features. If my goal was to have a demo as soon as possible, I would look at the available options and start building on whichever option checked the most boxes. However, my goal isn't to have most of these features. I want them all!  With that in mind, I oriented my search for a foundation based on the hardest problems.

The foundation that I chose was to build Easel using Clojure, running on the JVM. The JVM solves many hard problems that would likely take decades to solve for other foundations. Some hard problems solved by the JVM:
- Large ecosystem of high quality libraries across many domains
- World class garbage collector
- Excellent support for multi-threading and shared memory
- High performance, dynamic execution and evaluation at runtime
- Industrial strength developer tooling (eg. profiling, benchmarking, building, deploying, monitoring, etc)

Clojure also solves many hard problems:
- Strong support for immutable data in the language and ecosystem
- Long-term stability for projects that take time to build
- Excellent support for REPL driven development
- Simple and flexible constructs for data oriented programming

However, this choice of foundation comes at a cost. While the JVM solves lots of hard problems, it has one major weakness, the UI libraries provided by the JVM (Swing and JavaFX) are clunky and dated. However, even though writing a UI library from scratch isn't trivial, it is a much easier problem to solve than hard problems like writing a garbage collector that supports multi-threading and shared memory.

For various reasons, other IDEs are built on other foundations that are unlikely to solve all these problems in the next decade. **This provides an opportunity for a focused effort to achieve these goals before any other existing IDE.**


### Social Reasons for Easel

Software tools used to work together. Check out this [video](https://www.youtube.com/watch?v=rf5o5liZxnA) from 30 years ago of Steve Jobs demoing NeXTSTEP. Notice how there are a dozen applications that all share data and work together. This isn't even a particularly old example. Here's a [Smalltalk demo](https://www.youtube.com/watch?v=s6HJEnGRt88) from a decade earlier (see [https://jackrusher.com/classic-ux/](https://jackrusher.com/classic-ux/) for lots more).

Historically, Emacs users have extended their Emacs environment not just to improve their coding workflow, but they've also built personal applications, games, note-taking apps, calculators, email clients, messaging apps, meal planners, music players, etc, etc. In previous eras, computers came with various authoring tools for writing documents, drawing pictures, recording audio, and writing programs. I feel like we've lost a bit of that ethos of using the computer as a creative tool (although there are still wonderful examples like <https://scratch.mit.edu/>). 


It's important for Easel to not just provide excellent tools for writing clojure programs. Easel should also support making games, art, and productivity apps. Each tool can benefit from having a data oriented suite of other tools that can be mixed and matched. Easel isn't just for writing software, it's a tool to help make the computer do what you want.

There are a few interesting programming problems related to making different software tools work together, but I would argue the main impediments to building software tools that work together are social. The problem is that in most cases, I don't actually want my applications to share data with each other because I don't really trust most of the software running on my computer. Sharing any data at all with an application usually feels like a reluctant concession rather than a confident, helpful collaboration. I don't have a solution to the problem of building trust other than doing it the hard way. 

It's still important to have tools that are free as in freedom, rather than free as in Faustian. One of the reasons I appreciate Emacs is that the community has built and maintained that trust over decades. I have zero worries that I will update Emacs and see three new sparkle buttons and a popup that I never wanted or asked for. We need more software tools like this. I'm a big fan of Emacs, but unfortunately, I don't think it's going to get first class 2d and 3d graphics or switch to using Clojure any time soon.

## Why Easel Can Succeed

The benefit of having used Easel as my main IDE for the last year is that I can confidently say that Easel is 80% done. I know the second 80% often takes just as long as the first 80%, but most major features have already been demonstrated, even if they are still unpolished and rough around the edges. There are still some design challenges left, but most of the remaining work is grunt work, polish, documentation, guides, and bug fixes. In other words, **there are no major roadblocks to making Easel a reality.**

Another virtue of dogfooding Easel for a year is that while there are still missing features, there are also several features in Easel that I rely on that would be difficult to implement in other IDEs. The desire to keep the unique features that Easel offers is a big motivator to continue adding missing functionality.

## Status

I've been using Easel as my main IDE essentially since Easel was announced. However, I was leaning heavily on an embedded terminal emulator running emacs to do a lot of typical IDE things. The main effort over the last year was writing [clobber](https://github.com/phronmophobic/clobber), a text editor. I now use clobber as my main text editor (even though I still sometimes cheat and use emacs for specific features that haven't been implemented yet). Clobber has many familiar features you would expect in a typical clojure editor:

- Syntax highlighting for many popular languages via [tree-sitter](https://tree-sitter.github.io/)
- Paredit
- Configurable key bindings
- Inline Evaluation
- Indentation according to the [Clojure Style Guide](https://github.com/bbatsov/clojure-style-guide) that is configurable via an [indents config](https://github.com/weavejester/cljfmt/blob/master/docs/INDENTS.md)
- Full unicode support
- Undo/redo
- Autocomplete

Some other notable tools for Easel that were implemented or improved over the past year:

- [Membrane](https://github.com/phronmophobic/membrane): A Simple UI library that runs anywhere
- [Membrandt](https://github.com/phronmophobic/membrandt/): A WIP UI component library for membrane using the [ant design system](https://ant.design/).
- [llama.clj](https://github.com/phronmophobic/llama.clj): Run LLMs locally
- [Terminal Emulator](https://github.com/phronmophobic/membrane.term)
- [Embedded Browser](https://github.com/phronmophobic/clj-cef2) via Chromium Embedded Framework
- [Viscous](https://github.com/phronmophobic/viscous): A generic data viewer that can work with very large collections
- [Dewey](https://github.com/phronmophobic/dewey): A public dataset that indexes clojure libraries available on github
- [Bifurcan](https://github.com/phronmophobic/bifurcan): A fork of Zach Tellman's [bifurcan](https://github.com/lacuna/bifurcan) with improvements to the Rope implementation

It's difficult to convey the joy of using Easel, but I've uploaded a short demo to <https://youtu.be/sceGtaNHxcM> that tries to convey how I use Easel.

## "Roadmap"

Most of the past year was spent working on boring features like recreating features that you find in typical IDEs. I still have a few boring features to work through, but I'm excited to start showing off features that you can't find in other IDEs. Stay tuned!

A short list of features that I'll be working on in the near term:

- Improved Error Messages: I've always thought error messages would best be addressed by improved tools. Now that I'm building my own IDE, I would like to contribute to the problem.
- FlowStorm Integration: I started working on integrating FlowStorm last year. I believe FlowStorm can provide powerful facilities to help understand and debug programs.
- Extension: I'm still not sure what approach to take to support extension and customization by end users. There's a lot of design work to do. If you have ideas, please reach out!


## Get involved

All the code for Easel is publicly available, but I don't think it's quite ready for folks to try on their own. There are still some rough edges and missing pieces that I'm working to fix.

If you're interested in Easel and want to help make it a reality, the best way to help is to spread the word and engage with us.

- Join us in the [Clojurian's slack](http://clojurians.net/) in [#easel](https://clojurians.slack.com/archives/C086K1WGFA4)
- Share this post: <https://blog.phronemophobic.com/easel-one-year.html>
- Star Easel on GitHub: <https://github.com/phronmophobic/easel>
- Sponsor on GitHub: <https://github.com/sponsors/phronmophobic>
- Follow on Bluesky: <https://bsky.app/profile/phronemophobic.bsky.social>


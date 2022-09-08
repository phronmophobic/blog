{{table-of-contents/}}

Posted: September 6th, 2022
**Update**: September 7th, 2022. Added [Failed Analyses](http://localhost:8000/dewey-analysis.html#Failed-Analyses) section and fixed total repository count.

_See [source](https://github.com/phronmophobic/blog/blob/master/markdown/dewey-analysis.md) for full history._

# Introduction

I'm generally interested in tools like [cljdoc](https://cljdoc.org/) that work at the ecosystem level. As part of my work on [dewey](https://github.com/phronmophobic/dewey), which builds an index of all clojure libraries on github, I thought it would be a straightforward extension to statically analyze all the projects found. A bare, shallow checkout of every clojure project found by dewey{{footnote}}Nine projects were excluded because they are ginormous by themselves.{{/footnote}} is only about 14GB, which is a very tractable size.

There's also a couple of features of the clojure ecosystem that make it an interesting target for wholesale analysis:
* libraries address a wide range of problems
* libraries tend to be stable and concise
* clojure syntax is regular and minimal
* clojure has strong support and conventions for namespaces

There are several tools available for analysis. Projects like cljdoc and [getclojure](https://github.com/devn/getclojure) use dynamic analysis, but for this initial run, I chose to use [clj-kondo's static analysis](https://github.com/clj-kondo/clj-kondo/tree/master/analysis). Given a project, clj-kondo will report{{footnote}}Clj-kondo also allows you to write your own linters, but that will have to wait for a future update.{{/footnote}}:
* namespace definitions and usages
* locals and usages
* var definitions and usages
* keywords
* protocol implementations

**Analysis Rate**: 87%

Out of the 13,274 repositories found by dewey on github, 11,573 (87%) were successfully{{footnote}}See [Failed Analyses](#Failed-Analyses).{{/footnote}} analyzed.

# Results

The main goal of this project is to make the resulting analyses available for other projects and tools to consume. The full analysis of projects can be found under the [dewey releases](https://github.com/phronmophobic/dewey/releases) in the `analysis.edn.gz` file.

In addition to providing the data, I also wanted to do some cursory investigation based on the analyses which I present below.

## Hidden Gems

The clojure.core library is {{tooltip text='teehee'}}rich{{/tooltip}} and I always seem to find useful nuggets that I've somehow missed. Just to make sure that I'm not the only person with clojure.core FOMO, I thought I'd share some hidden gems found among the least used public clojure.core vars. To be fair, many of these functions are recent additions, but not all of them!

### pvalues and pcalls

I'll often use `pmap` for quick and dirty parallel computation. `pvalues`(15 usages) and `pcalls`(19 usages) also seem useful for quick and dirty parallelization.

`(pvalues & exprs)`: Returns a lazy sequence of the values of the exprs, which are evaluated in parallel.
`(pcalls & fns)`: Executes the no-arg fns in parallel, returning a lazy sequence of their values.

### parse-*

The core library added a handful of useful parse-* functions in `1.11` like `parse-double`(26 usages), `parse-boolean` (6 usages), `parse-long`(202 usages), `parse-uuid`(25 usages). They do basically what you would expect. I usually search for the proper java interop call or go the lazy route and just use `read-string`. I'm happy to have a better alternative now.

### iteration

Another gem is `iteration` (11 usages, once by [dewey itself!](https://github.com/phronmophobic/dewey/blob/b5c29261ca1feed4c213606499c5160305e1b622/src/com/phronemophobic/dewey.clj#L57)) which was added in `1.11`. The doc string doesn't immediately illuminate why or how to use it, but it's great. Check it out!{{footnote}}Here's a blog post explaining more about `iteration`. [https://www.juxt.pro/blog/new-clojure-iteration](https://www.juxt.pro/blog/new-clojure-iteration) {{/footnote}}

### min-key and max-key

Two functions that I think are criminally under used are `min-key`(256 usages) and `max-key`(431 usages). They tend to get overshadowed by their much more popular cousin, `sort-by` (4,926 usages), but are still useful in their own right.

```clojure
(min-key k x)
(min-key k x y)
(min-key k x y & more)
```
```
Returns the x for which (k x), a number, is least. 
If there are multiple such xs, the last one is returned.
```

`max-key` is the same, but different.

### random-uuid

`random-uuid`(60 usages) is another `1.11` addition and a welcome one.

## Usage of clojure.core functions added in 1.10 and 1.11

There's always a delay between new clojure versions and projects getting around to adopting and leveraging the latest additions. I was curious which of the latest features were gaining traction.

{{vega-embed-edn}}
{
 "$schema" "https://vega.github.io/schema/vega-lite/v5.json",
 "description" "description"
 "data"
 {"values" ({:c 818, :name ex-message, :added "1.10"}
            {:c 412, :name abs, :added "1.11"}
            {:c 301, :name requiring-resolve, :added "1.10"}
            {:c 202, :name parse-long, :added "1.11"}
            {:c 104, :name tap>, :added "1.10"}
            {:c 102, :name ex-cause, :added "1.10"}
            {:c 67, :name add-tap, :added "1.10"}
            {:c 66, :name update-vals, :added "1.11"}
            {:c 60, :name random-uuid, :added "1.11"}
            {:c 26, :name parse-double, :added "1.11"}
            {:c 25, :name parse-uuid, :added "1.11"}
            {:c 22, :name update-keys, :added "1.11"}
            {:c 19, :name remove-tap, :added "1.10"}
            {:c 16, :name PrintWriter-on, :added "1.10"}
            {:c 11, :name iteration, :added "1.11"}
            {:c 9, :name infinite?, :added "1.11"}
            {:c 7, :name NaN?, :added "1.11"}
            {:c 6, :name parse-boolean, :added "1.11"}
            {:c 5, :name read+string, :added "1.10"}
            {:c 2, :name seq-to...ucturing*, :added "1.11"})
  },
 "layer" [{
           "mark" "bar"
           },
          {
           "mark" {
                   "type" "text",
                   "align" "left",
                   "baseline" "middle",
                   "dx" 3
                   },
           "encoding" {
                       "text" {"field" "c", "type" "nominal"}
                       }
           }],

 "encoding" {
             "x" {"field" "c", "type" "quantitative", "title" "usage count"},
             "color" {"field" "added"
                      "legend" {"orient" "bottom"}
                      "type" "nominal"}
             ,
             "y" {"field" "name", "type" "nominal"
                  "title" nil
                  "sort" {"field" "c"
                          "op" "max"}}}
 }
{{/vega-embed-edn}}


{{quote}}*{{/quote}} seq-to-map-for-destructuring{{footnote}}CSS is hard{{/footnote}}


## Reference type usage

{{vega-count-chart}}

{:data 
{ref 2000
atom 18592
agent 581
volatile! 1329}
:xlabel "count"
:ylabel "ref type"
:sort-field "count"
}

{{/vega-count-chart}}



The results match my expectation that `atom` is, by far, the most common reference type. It was interesting to see that `ref` usage is higher than `volatile!`, but `volatile!` is much newer. I'm not surprised that `agent` is the least used reference type.

**Average mutable reference usage per repository**: 1.94
**Repositories with no mutable reference usages**: 7,245 (63%)

Truly bananas. Clojure libraries really do have less state. It probably isn't surprising if you've been programming in clojure for any length of time, but it's pretty wild to see the data back this up. I don't think I would have believed this 10 years ago.


## Var Definitions and Macros

{{vega-count-chart}}
{
:sort-field "percentage"
:xlabel "percentage"
:ylabel "def type"
:data
(["declare" 2.0203450981924513]
 ["def" 21.65492785761036]
 ["definline" 0.04884386569890687]
 ["definterface" 0.016769727223291363]
 ["defmacro" 2.931853038576885]
 ["defmulti" 0.8088544159738978]
 ["defn" 63.55620789251745]
 ["defonce" 0.9233932810378345]
 ["defprotocol" 3.8424655080901724]
 ["defrecord" 3.2857268455654656]
 ["deftype" 0.9106124695132871])
}

{{/vega-count-chart}}

**total var definitions**: 1,228,404

Most var definitions are functions. No surprise. I am sort of surprised that over 20% of vars are `def`s. What are people `def`ing all over the place? That's something I would like to follow up on at some point.

I consider `defprotocol` to be an important, fundamental building block, so to see that `defmacro` usage (2.93%) is similar to `defprotocol` usage (3.84%) is interesting.


## Lein vs deps

{{vega-count-chart}}
{
:sort-field "count"
:xlabel "count"
:ylabel "project type"
:data

[
["lein" 9790,]
[ "deps" 1405,]
[ "deps+lein" 505]
[ "neither" 1574]
]

}

{{/vega-count-chart}}


These numbers don't directly match the latest [state of clojure](https://clojure.org/news/2022/06/02/state-of-clojure-2022) numbers (ie. over 50% deps.edn usage), but I don't think they're inconsistent since clojure repositories on github will be weighted towards older projects.

The "neither" category means that no `deps.edn` or `project.clj` file was found. Many of them were shadow-cljs projects. Detecting other build tools would be a good improvement in the future.

## Local names

Clojurians like using short local variable names and this proves it!

### Local name lengths

 <!-- ["4 without \"this\"" 536081] -->
 <!-- ["1 excluding \"_\"" 601781] -->
{{vega-count-chart}}
{
:sort-field "count"
:xlabel "count"
:ylabel "local name length"
:data
([1 757732]

 [2 257745]
 [3 348750]
 [4 628377]
 ["\"_\"" 155951]
 [5 362796]
 [6 303371]
 [7 273346]
 [8 213330]
 [9 171295]
 [10 140454]
 [11 103798]
 [12 81235]
 [13 62657]
 [14 44340]
 [15 35847]
 [16 29827]
 [17 18797]
 [18 13279]
 [19 10194]
 [20 7604]
 [21 11310]
 [22 4581]
 [23 3387]
 [24 3257]
 [25 1549]
 [26 1316]
 [27 1225]
 [28 753]
 [29 655]
 [30 569]
 [31 349]
 [32 386]
 [33 191]
 [34 188]
 [35 141]
 [36 192]
 [37 252]
 [38 104]
 [39 103]
 [40 124]
 [41 38]
 [42 23]
 [43 19]
 [44 14]
 [45 10]
 [46 11]
 [47 5]
 [48 14]
 [49 6]
 [50 7]
 [51 2]
 [52 3]
 [53 1]
 [54 1]
 [56 2]
 [59 3]
 [61 1]
 [62 1])


}

{{/vega-count-chart}}

The most common local name is `_` with 155,951 followed by `this` at 92,296. I'm not sure what it says that 4% of locals are throw aways. What a waste.

### Local binding names that are a single letter

{{vega-count-chart}}
{
:sort-field "local name"
:xlabel "count"
:ylabel "local name"
:data
(["출" 2]
 ["입" 2]
 ["값" 2]
 ["✳" 3]
 ["ஆ" 2]
 ["с" 1]
 ["ϵ" 2]
 ["ϕ" 12]
 ["ω" 6]
 ["ψ" 11]
 ["φ" 34]
 ["τ" 3]
 ["σ" 34]
 ["ρ" 2]
 ["π" 2]
 ["μ" 4]
 ["λ" 5]
 ["κ" 1]
 ["θ" 98]
 ["η" 1]
 ["ε" 17]
 ["δ" 21]
 ["γ" 10]
 ["β" 11]
 ["α" 38]
 ["Ω" 3]
 ["Σ" 8]
 ["Π" 1]
 ["Δ" 3]
 ["ø" 2]
 ["|" 20]
 ["z" 4145]
 ["y" 27908]
 ["x" 75792]
 ["w" 8251]
 ["v" 50609]
 ["u" 2805]
 ["t" 16770]
 ["s" 38352]
 ["r" 14911]
 ["q" 5564]
 ["p" 16929]
 ["o" 8306]
 ["n" 29860]
 ["m" 35856]
 ["l" 6066]
 ["k" 43163]
 ["j" 3849]
 ["i" 20805]
 ["h" 7044]
 ["g" 9868]
 ["f" 41873]
 ["e" 35583]
 ["d" 11255]
 ["c" 23209]
 ["b" 23039]
 ["a" 32925]
 ["_" 155951]
 ["Z" 51]
 ["Y" 373]
 ["X" 650]
 ["W" 62]
 ["V" 171]
 ["U" 84]
 ["T" 248]
 ["S" 608]
 ["R" 218]
 ["Q" 116]
 ["P" 181]
 ["O" 22]
 ["N" 309]
 ["M" 379]
 ["L" 204]
 ["K" 91]
 ["J" 40]
 ["I" 68]
 ["H" 98]
 ["G" 95]
 ["F" 105]
 ["E" 132]
 ["D" 134]
 ["C" 294]
 ["B" 302]
 ["A" 611]
 ["?" 36]
 [">" 3]
 ["=" 4]
 ["<" 3]
 ["-" 15]
 ["+" 18]
 ["*" 60]
 ["%" 123]
 ["$" 757]
 ["!" 18])


}

{{/vega-count-chart}}

I couldn't find an official reference for what single character symbols are supported by clojure, but the [edn spec](https://github.com/edn-format/edn#symbols) states that:

> Symbols begin with a non-numeric character and can contain alphanumeric characters and `. * + ! - _ ? $ % & = < >`

I like the idea of using Greek letters as locals, but I find using `|` or `!` as locals offensive. 

> Additionally, `:` and `#` are allowed as constituent characters in symbols other than as the first character.

Does anybody do that? Well, there's only a single local that contains `#` in the name other than at the very end (which is used by syntax quote), but 21 repositories use locals with `:` in the name. It's not something I've thought a lot about before, but now that I know that I can...

## Emoji and unicode in names

It's not clear if using emoji and unicode as var names is officially supported, but it's also not clear if it's not not supported{{footnote}}Clearly{{/footnote}}. I mean, it works when you type it into the repl:

```clojure
user> (def 😊 42)
#'user/😊
user> (+ 😊 😊)
84
```

I'm not saying it's a good idea. Anyway, there are 25 brave repositories that use emoji/unicode in var definition names. There is a single namespace that includes unicode.

Usages range from [questionable](https://github.com/adereth/unicode-math) to [╯°□°╯ ┻━┻](https://github.com/jstepien/flip).

# Failed Analyses

For the 13% of repositories that aren't included, what went wrong?

{{vega-count-chart}}
{:sort-field "count"
 :xlabel "count"
 :ylabel "fail type"
 :data

{:empty-analyses 1398,
 :max-bytes-limit-exceeded 31,
 "java.lang.RuntimeException" 45,
 "java.lang.IllegalArgumentException" 12,
 "java.lang.NumberFormatException" 2,
 "java.util.concurrent.ExecutionException" 2,
 :download-error 10,
 "java.lang.OutOfMemoryError" 62,
 "java.lang.InterruptedException" 4,
 :no-findings 128,
 "java.lang.ThreadDeath" 1,
 :read-error 6}
}
{{/vega-count-chart}}

In the future, I'd like to do a better job of breaking down what errors happened during analysis, but here's the coarse breakdown for now.

Descriptions:
* `:empty-analyses`: No analysis was produced. The most common (only?) reason is that no project files were found (only deps.edn and project.clj supported).
* `:no-findings`: Analysis succeeded without error, but no findings were found. In theory, this could mean that everything worked and there was just nothing to analyze, but I'm assuming that it's an error for now.
* various Exceptions: Exceptions thrown while trying to analyze a project.
* `:max-bytes-limit-exceeded`: Repository checkout was over file size limit (currently 100Mb).
* `:download-error`: An exception was thrown while trying to download the repository from github.
* `:read-error`: Analyses for each repository were saved to intermediate files before being aggregated. This error means the resulting analysis file was unreadable.

# Biases

I guess I wouldn't say the data is biased so much as unrefined. I tried simply looking at the "most popular X" among usages, but you get misleading results if you don't account for biases like:
* popular libraries have more forks which amplify their usage patterns
* outliers: a single or small number of repositories that do "weird" things can skew simple counts and averages
* copy and pasting: for a number reasons, shared code is sometimes added to a repository by copy and pasting the code into a library rather than referencing the code as a dependency.
* example and test code: test/example code can often exhibit very different usage patterns than "normal" code.
* failed analyses: around ~87% of repositories were successfully analyzed. That's not terrible, but the 13% of libraries that failed to produce analysis probably aren't uncorrelated which can introduce biases.

Most of these biases are pretty straightforward to account for and will hopefully be addressed in future work.

# Future Work

Most language ecosystems have figured out that directly supporting community repositories/tools like clojars is a big win. Based on some smart design choices by clojure, its maintainers, and the community, I think that clojure is well positioned to take advantage of tools that work at the ecosystem level. I'm not totally sure what other ecosystem tools look like, but I have some ideas.

## Mundane Improvements

While the vision is to work on innovative ecosystem tooling, many of the planned improvements are mundane:
* crawl other open-source hosts besides github
* improve support for clojurescript and other clojure dialects
* automate more of the data gathering and analysis


## Use a database

Currently, the full analysis of every clojure repository is made available as a giant 600+ MB gzipped edn file. It would be great to make the data available in a more organized, structured, and queryable format (like a database).

## Example Usages

Examples for clojure functions can be illustrative. It should be possible to pull up examples for _any_ function.

## Improved Code Search

Github search is bad. [grep.app](https://grep.app/) is better, but a search tool that only targets clojure code could be much, much better!

## Java/Javascript Interop

One of the common roadblocks for learning clojure/clojurescript is that you eventually need some amount of host interop to be productive. Finding commonly used java/javascript host interop can provide valuable input for improving learning resources with common interop knowledge or creating wrapper libraries for frequently required host APIs.

## Clojure Specs

Afaict, specs are designed with a global, universal orientation in mind. We should build tooling that leverages and supports this. As a simple example, building a searchable database of all specs should be straightforward.

## N-grams

> In the fields of computational linguistics and probability, an n-gram (sometimes also called Q-gram) is a contiguous sequence of n items from a given sample of text or speech.
> {{blockquote-footer}}[N-grams Wikipedia](https://en.wikipedia.org/wiki/N-gram){{/blockquote-footer}}

I'm not exactly sure what I'd expect to find, but it would be interesting to find common patterns and idioms in clojure code.

## Namespace Alias usage

It's common to use an alias when requiring a namespace. Since aliases are short, it's easy for multiple libraries to "claim" the same alias. I'd like to build a database of all namespace alias usage so that it's easier to pick a preferred alias for your library that is less likely to conflict. This is probably a dumb idea, but at least it's an easy dumb idea.

# Related Work

* [100 Most Used Clojure Expressions](https://ericnormand.me/article/100-most-used-clojure-expressions)
* [getclojure](https://github.com/devn/getclojure)
* [crossclj](https://github.com/fbellomi/crossclj)

# Footnotes

{{footnotes/}}

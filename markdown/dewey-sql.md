{{table-of-contents/}}

Posted: October 23, 2024

# Introduction


It's been about two years since the [last dewey post](https://blog.phronemophobic.com/dewey-analysis.html). [Dewey](https://github.com/phronmophobic/dewey) is now tracking 12,947 libraries (1,401 more repos!).

Starting with the `2024-10-21` release, dewey now includes a sqlite dump that makes it easier to investigate the analyses generated. For those unfamiliar with dewey, dewey is a project that scans github for clojure projects and runs [clj-kondo analysis](https://github.com/clj-kondo/clj-kondo/tree/master/analysis) for each. The results are then [released](https://github.com/phronmophobic/dewey/releases) weekly.

For this post, my goal is to show how the sql dump can be used by doing a bit of curiosity driven analysis.

## Getting Started

To get started, download the `dewey.sqlite3.sql.gz` file from the [dewey releases](https://github.com/phronmophobic/dewey/releases) page. The sql can be loaded into a fresh sqlite db named `dewey.sqlite` as follows.

```bash
;; note: some systems use zcat instead of gzcat
gzcat dewey.sqlite3.sql.gz | sqlite3 dewey.sqlite
```

That's it! You are now ready to start querying the analyses. The sql dump doesn't include any indexes. Queries are generally pretty fast, but it may be useful to add indexes for specific columns depending on the queries you expect to run.

## Running queries

For our analysis, we'll be using [next.jdbc](https://github.com/seancorfield/next-jdbc) and [HoneySQL](https://github.com/seancorfield/honeysql/).

```clojure
;; require the helper libraries
(require '[clojure.java.io :as io]
         '[honey.sql :as sql]
         '[next.jdbc :as jdbc])

;; next.jdbc db spec
(def db {:dbtype "sqlite"
         :dbname "dewey.sqlite"})

;; helper functions
(defn q
  "Takes a honey sql query and runs it on db, returning the results."
  [m]
  (jdbc/execute! db (sql/format m
                                {:quoted true})))

(def total-repos
  (-> (q {:select [[[:count [:distinct :repo]] :num-repos]]
       :from :basis})
      first
      :num-repos))
;; 12974

```

# Clojure State

One of the more interesting takeaways from the last dewey post is that most libraries don't have mutable state. For our purposes, we'll say that a repo doesn't have any mutable state if they don't use any of the managed state references. There are other types of mutable state available in clojure, but we'll assume they are negligible.

### Reference Type Usage

```clojure
(q {:select [:name [[:count :*] :c]]
    :from :var-usages
    :where [:and
            [:in :name ["ref" "agent" "volatile!" "atom"]]
            [:= "clojure.core" :to]]
    :group-by [:name]})
;; [{:var-usages/name "agent", :c 701}
;;  {:var-usages/name "atom", :c 25048}
;;  {:var-usages/name "ref", :c 2332}
;;  {:var-usages/name "volatile!", :c 2249}]
```

Compared to two years ago, `volatile!` has nearly doubled in usage. The usage of the other reference types seems steady and unsurprising. The `atom` is still, by far, the goto for state in clojure.

The raw counts are only a rough measure since they can be skewed by outliers.

```clojure
(def repos-with-state
  (-> (q {:select [[[:count [:distinct :repo]] :c]]
          :from :basis
          :left-join [:var-usages [:and
                                   [:= :var-usages.basis-id :basis/id]]]
          :where [:and
                  [:in :name ["ref" "agent" "volatile!" "atom"]]
                  [:= "clojure.core" :to]]})
      first
      :c))
;; 4818
(def repos-without-state-ratio
  (- 1.0 (/ repos-with-state total-repos)))
;; 0.6286418991829813

```

Around 62% of clojure repos have no mutable references. This is down from 63% the last time we checked. I blame `volatile!`.

# Java Interop

Another change since the last post is that dewey now includes analysis related to Java interop. A common question from folks interested in learning clojure is if learning Java is required. Let's see if we can collect some data.

First, let's count the number of repos that use Java classes via interop:

```clojure

(def uses-java-class-repo-count
  (q {:select [[[:count [:distinct :repo]] :uses-java-classes]]
      :from :basis
      :left-join [:java-class-usages
                  [:and
                   [:= :java-class-usages.basis-id :basis/id]]]
      :where [:is-not :class nil]}))
;; [{:uses-java-classes 9830}]
```

There are 9,380 repos that use Java classes which is about 76%. This is actually higher than expected. Let's see if we can figure out the distribution of class usages by repo.

```clojure
(def class-usages-by-repo
  (q {:select [:repo [[:count [:distinct :class]]  :c]]
      :from :basis
      :inner-join [:java-class-usages
                   [:and
                    [:= :java-class-usages.basis-id :basis/id]]]
      :group-by [:repo]}))


;; counting repos by the number of
;; distinct classes used.

(->> class-usages-by-repo
     (group-by (fn [{:keys [c]}]
                 ;; bucket all 10+ together
                 (if (< c 10)
                   c
                   10)))
     (map (fn [[c repos]]
            [c (count repos)]))
     (sort-by first))
([1 1498]
 [2 1228]
 [3 849]
 [4 673]
 [5 556]
 [6 438]
 [7 393]
 [8 314]
 [9 313]
 ;; greater than 10
 [10 3568])

```

Looking more closely at the numbers, most of the repos that do use classes are using just a handful of classes.

Out of curiousity, let's see which classes are the most commonly used.

```clojure
(def class-usage-counts
  (q {:select [:class :class [[:count [:distinct :repo]]  :c]]
      :from :basis
      :inner-join [:java-class-usages
                   [:and
                    [:= :java-class-usages.basis-id :basis/id]]]
      :group-by [:class]
      :order-by [[:c :desc]]
      :limit 30}))
;; [{:java-class-usages/class "java.lang.Exception", :c 4183}
;;  {:java-class-usages/class "java.lang.String", :c 3532}
;;  {:java-class-usages/class "java.lang.System", :c 3515}
;;  {:java-class-usages/class "java.lang.Integer", :c 3145}
;;  {:java-class-usages/class "java.lang.Thread", :c 2084}
;;  {:java-class-usages/class "java.io.File", :c 1745}
;;  {:java-class-usages/class "java.lang.Throwable", :c 1665}
;;  {:java-class-usages/class "java.lang.Math", :c 1593}
;;  {:java-class-usages/class "java.lang.Object", :c 1549}
;;  {:java-class-usages/class "java.lang.Long", :c 1514}
;;  {:java-class-usages/class "java.lang.IllegalArgumentException",
;;   :c 1286}
;;  {:java-class-usages/class "java.util.Date", :c 1103}
;;  {:java-class-usages/class "java.util.UUID", :c 1082}
;;  {:java-class-usages/class "java.lang.Double", :c 996}
;;  {:java-class-usages/class "java.lang.Boolean", :c 791}
;;  {:java-class-usages/class "java.lang.Class", :c 697}
;;  {:java-class-usages/class "java.io.ByteArrayOutputStream", :c 678}
;;  {:java-class-usages/class "java.lang.Runtime", :c 649}
;;  {:java-class-usages/class "java.io.ByteArrayInputStream", :c 637}
;;  {:java-class-usages/class "java.lang.RuntimeException", :c 628}
;;  {:java-class-usages/class "java.lang.Character", :c 590}
;;  {:java-class-usages/class "java.net.URL", :c 579}
;;  {:java-class-usages/class "java.io.Writer", :c 558}
;;  {:java-class-usages/class "java.io.InputStream", :c 526}
;;  {:java-class-usages/class "java.net.URI", :c 521}
;;  {:java-class-usages/class "java.io.PushbackReader", :c 519}
;;  {:java-class-usages/class "java.util.concurrent.TimeUnit", :c 516}
;;  {:java-class-usages/class "clojure.lang.IPersistentMap", :c 499}
;;  {:java-class-usages/class "clojure.lang.ExceptionInfo", :c 499}
;;  {:java-class-usages/class "java.util.Map", :c 495}]

```

As can be seen, most java class usage is related to exceptions, strings, and I/O. Some of these usages can probably be replaced with libraries (eg. [babashka.fs](https://github.com/babashka/fs)) or namespaces in the clojure core library (eg. [clojure.math](https://clojure.github.io/clojure/clojure.math-api.html), [clojure.string](https://clojuredocs.org/clojure.string), and [clojure.java.io](https://clojuredocs.org/clojure.java.io)). However, I'm not aware of any pure clojure equivalent for `java.lang.Exception` which is the most popular class.

# Namespaced Keywords

I've been curious to see how prevelant namespaced keywords are. Let's see!

```clojure
(def namespaced-keyword-usages
  (q {:select [[[:is-not :ns nil] :namespaced] [[:count :*]  :c]]
      :from :keywords
      :group-by [:namespaced]}))
;; [{:namespaced 0, :c 13570743}
;;  {:namespaced 1, :c  2630640}]
```

There's about an order of magnitude more unqualified keywords vs qualified keywords. As mentioned, raw counts can be a bit misleading, so let's compare the number of repos that use qualified keywords vs those that don't.

```clojure

(def namespaced-keywords-repo-count
  (q {:select [[[:count [:distinct :repo]]  :c]]
      :from :basis
      :left-join [:keywords
                  [:and
                   [:= :basis-id :basis/id]]]
      :where [:is-not :ns nil]}))
;; [{:c 5084}]
(def qualified-usage-ratio
  (/ (-> namespaced-keywords-repo-count
         first
         :c
         double)
     total-repos))
;; 0.3918606443656544
(- 1.0 qualified-usage-ratio)
;; 0.6081393556343456

```

About 60% of repos don't use any qualified keywords. That's a bit higher than expected, but I guess I'm not sure what I expected. It would be interesting to see if this number has increased over time.

# Awards

Hopefully, this post has shown how easy it is to extract some useful insights from the dewey data, but I didn't want to sign off before showing how to extract some mostly useless insights.

## Whatchamcallit Award

This award goes to the namespace that you just don't know how to alias. To qualify for this award, the namespace must have been referenced at least 10 times.

```clojure

(def alias-counts
  (q {:select [:to
               [[:count [:distinct :alias]] :cdistinct]
               [[:count :*]
                :total]
               [
                [(keyword "/")
                 [:* 1.0 [:count [:distinct :alias]]]
                 [:count :*]]
                :cnorm]]
      :from :namespace-usages
      :order-by [[:cnorm :desc]]
      :having [[:> :total 10]]
      :group-by [:to]}))

(first alias-counts)
;; {:namespace-usages/to "clojurewerkz.elastisch.rest.bulk",
;;  :cdistinct 6,
;;  :total 11,
;;  :cnorm 0.5454545454545454}
```

And the award goes to `clojurewerkz.elastisch.rest.bulk`! This namespace is used 11 times, but somehow managed to be given 6 different aliases.

## Early Adopter Award

Clojure recently released 1.12. This release included 8 new vars in `clojure.core`. The new release is still fresh, but let's see which new vars are the most popular.

```clojure
(def clojure-added
  (->> (the-ns 'clojure.core)
       (ns-publics)
       vals
       (keep meta)
       (filter #(= "1.12"
                   (:added %)))
       (into {}
             (map (fn [m]
                    [(:name m)
                     m])))))

(q {:select [:name :filename :repo]
    :from :var-usages
    :inner-join [:basis [:= :basis-id :basis/id]]
    :where [:and
            [:in :name (map str (keys clojure-added))]
            [:= "clojure.core" :to]]
    :group-by [:name]})
;; [{:var-usages/name "*repl*", :var-usages/filename "src/fdb/core.clj", :basis/repo "filipesilva/fdb"}]

```

Out of the 8 vars, it seems like `clojure.core/*repl*` wins the award for first usage in the wild!

# Web Interface

In addition to adding the sqlite dump to the dewey releases, a simple web interface for searching names is available at [https://cloogle.phronemophobic.com/name-search.html](https://cloogle.phronemophobic.com/name-search.html).

# Conclusion

Clojure is powerful and succinct. One of the neat consequences is that the Clojure ecosytem is an interesting dataset for data mining. The ecosystem has tools, applications, and libraries that cover a large range of use cases, but the actual volume of code is impressively tractable to analyze.



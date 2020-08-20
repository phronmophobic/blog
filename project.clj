(defproject com.phronemophobic/blog "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.764"]
                 [com.phronemophobic/membrane "0.9.12-beta-SNAPSHOT"]
                 [com.phronemophobic/treemap-clj "0.2.0"]
                 [com.vladsch.flexmark/flexmark-all "0.62.2"]
                 [hiccup "1.0.5"]]
  :main ^:skip-aot blog.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :provided {:dependencies [[org.clojure/test.check "0.9.0"]
                                       ]}})

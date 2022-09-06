(defproject com.phronemophobic/blog "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.10.764"]

                 [org.clojure/data.json "2.4.0"]

                 [com.phronemophobic/membrane "0.9.31.8-beta"]
                 [com.phronemophobic.membrane/skialib-macosx-x86-64 "0.9.31.0-beta"]
                 [com.phronemophobic.membrane/skialib-macosx-aarch64 "0.9.31.0-beta"]
                 [com.phronemophobic.membrane/skialib-linux-x86-64 "0.9.31.0-beta"]

                 [net.java.dev.jna/jna "5.10.0"]
                 [com.phronemophobic/treemap-clj "0.2.0"]
                 [venantius/glow "0.1.6"]
                 [com.vladsch.flexmark/flexmark-all "0.62.2"]
                 [hiccup "1.0.5"]]
  :main ^:skip-aot blog.mdown
  ;; :javac-options     ["-target" "1.8" "-source" "1.8"]
  :java-source-paths ["src-java"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}
             :provided {:dependencies [[org.clojure/test.check "0.9.0"]
                                       ]}})

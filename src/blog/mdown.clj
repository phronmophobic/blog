(ns blog.mdown
  (:require [clojure.zip :as z]
            [hiccup.core :refer [html]])
  (:import com.vladsch.flexmark.util.ast.Node
           com.vladsch.flexmark.html.HtmlRenderer
           com.vladsch.flexmark.parser.Parser
           com.vladsch.flexmark.ext.attributes.AttributesExtension
           com.vladsch.flexmark.ext.xwiki.macros.MacroExtension
           com.vladsch.flexmark.util.data.MutableDataSet))


;; //options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

(def POSTS (atom {}))
(defmacro defpost [name val]
  `(let [v# (def ~name ~val)]
     (swap! POSTS assoc (:id ~name) ~name)
     v#))

(defn parse [s]
  (let [options (doto (MutableDataSet.)
                  (.set Parser/EXTENSIONS [(AttributesExtension/create)
                                           (MacroExtension/create)]))
        parser (-> (Parser/builder options)
                   (.build))
        doc (.parse parser s)]
    doc))

(defn doc->tree-seq [doc]
  (tree-seq #(.hasChildren %)
            #(seq (.getChildren %))
            doc))

(defn doc->zip [doc]
  (z/zipper #(.hasChildren %)
            #(seq (.getChildren %))
            identity
            doc))

(defn children [doc]
  (seq (.getChildren doc)))

(defprotocol IBlogHtml
  (blog-html [this]))

(extend-type com.vladsch.flexmark.util.ast.Document
  IBlogHtml
  (blog-html [this]
    [:div {}
     (map blog-html (children this))]))

(extend-type com.vladsch.flexmark.ast.Paragraph
  IBlogHtml
  (blog-html [this]
    [:p {}
     (map blog-html (children this))
     ]))

(extend-type com.vladsch.flexmark.ast.Heading
  IBlogHtml
  (blog-html [this]
    (let [tag (keyword (str "h" (.getLevel this)))]
      [tag {}
       (map blog-html (children this))])))

(extend-type com.vladsch.flexmark.ast.StrongEmphasis
  IBlogHtml
  (blog-html [this]
    [:strong
     (map blog-html (children this))]))


(extend-type com.vladsch.flexmark.ast.BlockQuote
  IBlogHtml
  (blog-html [this]
    [:blockquote.blockquote
     (map blog-html (children this))]))

(extend-type com.vladsch.flexmark.ast.FencedCodeBlock
  IBlogHtml
  (blog-html [this]
    [:pre
     [:code
      (map blog-html (children this))]]))


(extend-type com.vladsch.flexmark.ast.Code
  IBlogHtml
  (blog-html [this]
    [:code (map blog-html (children this))]))

(extend-type com.vladsch.flexmark.ast.Text
  IBlogHtml
  (blog-html [this]
    (str (.getChars this))))


(extend-type com.vladsch.flexmark.ast.Link
  IBlogHtml
  (blog-html [this]
    [:a {:href (-> this .getUrl str)}
     (-> this (.getText) str)]))

(extend-type com.vladsch.flexmark.ast.Image
  IBlogHtml
  (blog-html [this]
    [:img {:src (-> this .getUrl str)
           :alt (-> this (.getText) str)
           :style "max-width: 90vw;height:auto"}]))


(extend-type com.vladsch.flexmark.ast.SoftLineBreak
  IBlogHtml
  (blog-html [this]
    [:br]))


(defmulti markdown-macro (fn [macro]
                           (.getName macro)))


(defmethod markdown-macro "blockquote-footer" [macro]
  [:footer.blockquote-footer
   (map blog-html (drop-last (children macro)))])

(extend-type com.vladsch.flexmark.ext.xwiki.macros.Macro
  IBlogHtml
  (blog-html [this]
    (markdown-macro this)))








(defn parse-blog [fname]
  (-> (slurp fname)
      parse
      blog-html))

(defn blog-page [title post-subheading nav body]
  [:html {:lang "en"}
   [:head

    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=no"}] 
    ;; [:meta {:name "description" :content ""}]
    [:meta {:name "author" :content "Adrian smith"}]

    ;; <link rel="icon" href="../../favicon.ico">
    [:link {:rel "icon"
            :href "favicon.ico"}]
    [:title title]

    [:link {:href "bootstrap.min.css"
            :rel "stylesheet"}]
    [:link {:href "blog.css"
            :rel "stylesheet"}]]

   [:body

    (when nav
      [:div {:class "blog-masthead"}
       ])
    [:div.blog-header
       [:div.container
        [:h1.blog-title title]
        [:p.lead.blog-description post-subheading]]]


    [:div.container
     [:div.row
      [:div.col-sm-8.blog-main
       [:div.blog-post
        body]]]]


    ]]
  )


(defpost treemap-post
  {:id :treemap
   :title "Treemaps are awesome!"
   :subheading "An alternative to pprint for generically visualizing heterogeneous, hierarchical data"
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link.active {:href "#"}
           "Treemaps are awesome!"]
          [:a.nav-link {:href "treemap-demo.html"}
           "Treemap Demo"]
          [:a.nav-link {:href "https://github.com/phronmophobic/treemap-clj"}
           "Code on Github"]]]
   :src "markdown/treemaps-are-awesome.md"
   :out "resources/public/treemaps-are-awesome.html"})

(defpost treemap-post2
  {:id :not-treemap
   :title "Treemaps are awesome!"
   :subheading "An alternative to pprint for generically visualizing heterogeneous, hierarchical data"
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link.active {:href "#"}
           "Treemaps are awesome!"]
          [:a.nav-link {:href "treemap-demo.html"}
           "Treemap Demo"]
          [:a.nav-link {:href "https://github.com/phronmophobic/treemap-clj"}
           "Code on Github"]]]
   :src "markdown/treemaps-are-awesome.md"
   :out "resources/public/treemaps-are-awesome.html"})

(defn render-post! [{:keys [title
                            subheading
                            nav
                            src
                            out]
                     :as post}]
  (let [page-html (blog-page title
                             subheading
                             nav
                             (parse-blog src))
        html-str (html page-html)]
    (spit (:out post) html-str)))

(defonce running? (atom false))
(defn watch-blog [post-id]
  (let [post (get @POSTS post-id)
        _ (assert post (str "No post for id " (pr-str post-id) ". \n"
                            (clojure.string/join "\n" (keys @POSTS))) )
        f (clojure.java.io/file (:src post))
        get-val #(.lastModified f)]
    (when (not @running?)
      (reset! running? true)
      @(future
         (loop [last-val nil]
           (let [current-val (get-val)]
             (when @running?
               (when (not= current-val last-val)

                 (print "rendering blog...")
                 (flush)
                 (render-post! post)
                 (print " done.\n")
                 (flush))

               (Thread/sleep 500 )
               (recur current-val))))))))

(defn -main [ & args]
  (watch-blog (keyword (first args))))

;; for fenced code blog
;; (.getinfo adsf) to find lang
(comment(-> (parse "```foo

(+ 1 2)
```
")
            doc->zip
            z/next
            z/node
            (.getInfo)
            ))

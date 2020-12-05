(ns blog.mdown
  (:require [clojure.zip :as z]
            [membrane.ui :as ui]
            [membrane.skia :as skia]
            [glow.core :as glow]
            glow.parse
            glow.html
            [glow.colorschemes]
            [hiccup.core :refer [html]
             :as hiccup])
  (:import com.vladsch.flexmark.util.ast.Node
           com.vladsch.flexmark.html.HtmlRenderer
           com.vladsch.flexmark.parser.Parser
           com.vladsch.flexmark.ext.attributes.AttributesExtension
           com.vladsch.flexmark.ext.xwiki.macros.MacroExtension
           com.vladsch.flexmark.util.data.MutableDataSet
           com.phronemophobic.blog.HiccupNode))


;; //options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

(def POSTS (atom {}))
(defmacro defpost [name val]
  `(let [v# (def ~name ~val)]
     (swap! POSTS assoc (:id ~name) ~name)
     v#))

(defn doc->tree-seq [doc]
  (tree-seq #(.hasChildren %)
            #(seq (.getChildren %))
            doc))

(defn doc->zip [doc]
  (z/zipper #(.hasChildren %)
            #(seq (.getChildren %))
            (fn [node children]
                (.removeChildren node)
                (doseq [child children]
                  (.appendChild node child))
              node)
            doc))

(defn children [doc]
  (seq (.getChildren doc)))

(def ^:dynamic *parse-state*)

(defn zip-walk
  "Depth first walk of zip. edit each loc with f"
  [zip f]
  (loop [zip zip]
    (if (z/end? zip)
      (z/root zip)
      (recur (-> (z/edit zip f)
                 z/next)))))

(declare hiccup-node)

(defmulti macroexpand1-doc
  (fn [m]
    (if (instance? com.vladsch.flexmark.ext.xwiki.macros.Macro m)
      (.getName m)
      (if (instance? com.vladsch.flexmark.ext.xwiki.macros.MacroBlock m)
        (.getName (.getMacroNode m))
        (type m)))))

(defmethod macroexpand1-doc :default [node]
  node)

(defn inc-footnote-count []
  (let [k [::footnote-macro ::footnote-index]]
    (set! *parse-state*
          (update-in *parse-state* k (fnil inc 0)))
    (get-in *parse-state* k)))
(defn add-footnote [hiccup]
  (set! *parse-state*
        (update-in *parse-state*
                   [::footnote-macro ::footnotes] (fnil conj []) hiccup)))

(declare blog-html)
(defmethod macroexpand1-doc "footnote" [macro]
  (let [idx (inc-footnote-count)
        childs (->> (children macro)
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.MacroClose %))
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.Macro %)))]
    (add-footnote (map blog-html childs))
    (hiccup-node
     [:sup
      [:a {:href (str "#footnote-" idx)
           :name (str "footnote-ref-" idx)
           :title (clojure.string/join (map #(.getChars %) childs))} idx]])))

(defn get-footnotes []
  (get-in *parse-state* [::footnote-macro ::footnotes]))

(defmethod macroexpand1-doc "footnotes" [macro]
  (hiccup-node
   [:div
    (for [[i footnote] (map-indexed vector (get-footnotes))
          :let [idx (inc i)]]
      [:div
       [:a {:name (str "footnote-" idx)
            :href (str "#footnote-ref-" idx)} idx]
       ". " footnote])]))



(defn preprocess-footnotes [doc]
  (binding [*parse-state* nil]
    (zip-walk (doc->zip doc)
              macroexpand1-doc)))

(defn header->tag-name [header]
  (let [tag-chars (clojure.string/join
                     (map #(.getChars %) (children header)))
        tag-name (-> tag-chars
                       (clojure.string/replace #"[ ]" "-")
                       (clojure.string/replace #"[^A-Z\-a-z0-9]" ""))]
    tag-name))

(defn make-toc []
  (z/zipper (constantly true)
            #(get % :children [])
            (fn [node children]
              (assoc node :children children))
            {}))


(defn add-section [toc header]

  (loop [toc toc]
    (let [p (z/up toc)
          level (.getLevel header)]
      (if (and p (>= (get (z/node toc) :level)
                     level))
        (recur p)
        (let [res (-> toc
                      (z/append-child {:title (clojure.string/join
                                               (map #(.getChars %) (children header)))
                                       :name (header->tag-name header)
                                       :level level})
                      z/down
                      z/rightmost)]
          res)))))


(defn gen-table-of-contents [doc]
  (loop [zip (doc->zip doc)
         toc (make-toc)]
    (if (z/end? zip)
      (z/root toc)
      (let [node (z/node zip)]
        (if (instance? com.vladsch.flexmark.ast.Heading node)
          (recur (z/next zip) (add-section toc node))
          (recur (z/next zip) toc))))))




(defn gen-toc-html [toc]
  (let [html ()
        html (if-let [childs (:children toc)]
               (cons [:ul (map gen-toc-html childs)]
                     html)
               html)
        html (if-let [title (:title toc)]
               (cons [:li [:a {:href (str "#" (:name toc))} title]]
                     html)
               html)]
    html))


(defn preprocess-table-of-contents [doc]
  (let [toc (gen-table-of-contents doc)
        toc-html (gen-toc-html toc)]
    (zip-walk (doc->zip doc)
              (fn [node]
                (if (instance? com.vladsch.flexmark.ext.xwiki.macros.MacroBlock node)
                  (if (= (.getName (.getMacroNode node)) "table-of-contents")
                    (hiccup-node toc-html)
                    node)
                  node)))))

(defn parse [s]
  (let [options (doto (MutableDataSet.)
                  (.set Parser/EXTENSIONS [(AttributesExtension/create)
                                           (MacroExtension/create)]))
        parser (-> (Parser/builder options)
                   (.build))
        doc (.parse parser s)
        doc (-> doc
                (preprocess-footnotes)
                (preprocess-table-of-contents))]

    doc))

(defprotocol IBlogHtml
  (blog-html [this]))

(extend-type Object
  IBlogHtml
  (blog-html [this]
    (println "No implementation of method :blog-html found for class: " (type this))
    (println "line: " (.getLineNumber this))
    (let [s (str (.getChars this))]
      (println (subs s 0 (min 30 (count s)))))
    (throw (Exception. ""))))

(defn hiccup-node [content]
  (HiccupNode. content))

#_(defn doall* [s]
    (dorun (tree-seq #(do
                        (prn %)
                        (seqable? %)) seq s)) s)
(extend-type HiccupNode
  IBlogHtml
  (blog-html [this]
    (.hiccup this)))

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
      [tag {:id (header->tag-name this)}
       (map blog-html (children this))])))

(extend-type com.vladsch.flexmark.ast.StrongEmphasis
  IBlogHtml
  (blog-html [this]
    [:strong
     (map blog-html (children this))]))

(extend-type com.vladsch.flexmark.ast.Emphasis
  IBlogHtml
  (blog-html [this]
    [:em
     (map blog-html (children this))]))


(extend-type com.vladsch.flexmark.ast.BlockQuote
  IBlogHtml
  (blog-html [this]
    [:blockquote.blockquote
     (map blog-html (children this))]))

(extend-type com.vladsch.flexmark.ast.FencedCodeBlock
  IBlogHtml
  (blog-html [this]


    (case (.getInfo this)

      "clojure"
      (let [source (clojure.string/join (map #(.getChars %) (children this)))]
        (glow/highlight-html source))

      ;; else
      [:pre
       [:code
        (map blog-html (children this))]]
      )

    ))


(extend-type com.vladsch.flexmark.ast.Code
  IBlogHtml
  (blog-html [this]
    [:code (map blog-html (children this))]))

(extend-type com.vladsch.flexmark.ast.Text
  IBlogHtml
  (blog-html [this]
    (hiccup/h (str (.getChars this)))))

(extend-type com.vladsch.flexmark.ast.Link
  IBlogHtml
  (blog-html [this]
    [:a {:href (-> this .getUrl str)}
     (-> this (.getText) str)]))

(extend-type com.vladsch.flexmark.ast.AutoLink
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

(extend-type com.vladsch.flexmark.ast.OrderedList
  IBlogHtml
  (blog-html [this]
    [:ol (map blog-html (children this))]))

(extend-type com.vladsch.flexmark.ast.OrderedListItem
  IBlogHtml
  (blog-html [this]
    [:li (map blog-html (children this))]))


(extend-type com.vladsch.flexmark.ast.BulletList
  IBlogHtml
  (blog-html [this]
    [:ul (map blog-html (children this))]))

(extend-type com.vladsch.flexmark.ast.BulletListItem
  IBlogHtml
  (blog-html [this]
    [:li (map blog-html (children this))]))

(extend-type com.vladsch.flexmark.ast.HtmlCommentBlock
  IBlogHtml
  (blog-html [this]
    nil))

(extend-type com.vladsch.flexmark.ast.HtmlInlineComment
  IBlogHtml
  (blog-html [this]
    nil))


;; (extend-type com.vladsch.flexmark.ast.OrderedList
;;   IBlogHtml
;;   (blog-html [this]
;;     [:ol.list-group (map blog-html (children this))]))
;; com.vladsch.flexmark.ast.BulletList

(defmulti markdown-macro (fn [macro]
                           (.getName macro)))


(defmethod markdown-macro "tableflip" [macro]
  [:p {:title "table flip"} "(╯°□°）╯︵ ┻━┻"])

(defmethod markdown-macro "blockquote-footer" [macro]
  [:footer.blockquote-footer
   (map blog-html (drop-last (children macro)))])

(defmethod markdown-macro "contemplation-break" [macro]
  [:div {:style "width: 90%;height: 500px;border-top: #c4c4c4 1px solid;border-bottom: #c4c4c4 1px solid;margin-bottom:30px;"
         :title "This space intentionally left blank for contemplation."}])


(defmethod markdown-macro "shoot-in-the-foots" [macro]
  [:span {:title "everybody is each shooting just one foot in this metaphor"}
   "foots"])

(defmethod markdown-macro "square-bracket-left" [macro]
  "[")

(defmethod markdown-macro "square-bracket-right" [macro]
  "]")

(defmethod markdown-macro "quote" [macro]
  (let [childs (->> (children macro)
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.MacroClose %))
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.Macro %)))]
    (hiccup/h (clojure.string/join (map #(.getChars %) childs)))))

;; (defmethod markdown-macro "footnote" [macro]
;;   (let [idx ]
;;     [:a {:href "#"}
;;      (hiccup/h (str "[" idx "]"))]))

;; (defmethod markdown-macro "footnotes" [macro]
;;   "Footnotes!"
;;   )



(extend-type com.vladsch.flexmark.ext.xwiki.macros.Macro
  IBlogHtml
  (blog-html [this]
    (markdown-macro this)))

(extend-type com.vladsch.flexmark.ext.xwiki.macros.MacroBlock
  IBlogHtml
  (blog-html [this]
    (markdown-macro (.getMacroNode this))))


(extend-type com.vladsch.flexmark.ast.ThematicBreak
  IBlogHtml
  (blog-html [this]
    [:hr]))


(defn parse-blog [fname]
  (-> (slurp fname)
      parse
      blog-html))

(defn blog-page [{:keys [title
                         subheading
                         nav
                         src
                         asset-prefix]
                  :as post}]
  (let [body (parse-blog src)]
    [:html {:lang "en"}
     [:head

      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=no"}] 
      ;; [:meta {:name "description" :content ""}]
      [:meta {:name "author" :content "Adrian smith"}]

      ;; <link rel="icon" href="../../favicon.ico">
      [:link {:rel "icon"
              :href (str asset-prefix "favicon.ico")}]
      [:title title]

      [:link {:href (str asset-prefix "bootstrap.min.css")
              :rel "stylesheet"}]
      [:link {:href (str asset-prefix "blog.css")
              :rel "stylesheet"}]
      [:style {:type "text/css"}
       (glow/generate-css
        {:exception "#f00"
         :repeat  "#f00"
         :conditional "#30a"
         :variable "black"
         :core-fn "#30a"
         :definition "#00f"
         :reader-char "#555"
         :special-form "#30a"
         :macro "#05a"
         :number "#164"
         :boolean "#164"
         :nil "#164"
         :s-exp "#997"
         :keyword "#708"
         :comment "#a50"
         :string "#a11"
         :character "#f50"
         :regex "#f50"}
        )
       " div.syntax { padding: 4px ; background-color: #f8f8f8; margin-bottom: 18px }"
       " div.syntax pre { margin-bottom: 0 }"]
]

     [:body

      (when nav
        [:div {:class "blog-masthead"}
         nav])
      [:div.blog-header
       [:div.container
        [:h1.blog-title title]
        [:p.lead.blog-description subheading]]]


      [:div.container
       [:div.row
        [:div.col-sm-8.blog-main
         [:div.blog-post
          body]]]]


      ]])
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

(defpost functional-ui-post
  {:id :functional-ui
   :title "Rethinking Functional UI Software design"
   :subheading "The div must die"
   :nav nil #_[:div {:class "container"}
               [:nav.nav.blog-nav
                [:a.nav-link.active {:href "#"}
                 "Treemaps are awesome!"]
                [:a.nav-link {:href "treemap-demo.html"}
                 "Treemap Demo"]
                [:a.nav-link {:href "https://github.com/phronmophobic/treemap-clj"}
                 "Code on Github"]]]
   :src "markdown/functional-ui.md"
   :out "resources/public/functional-ui.html"})


(defpost what-is-a-ui
  {:id :what-is-a-ui
   :title "What is a User Interface?"
   :subheading "How to build a functional UI library from scratch: Part I"
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           "&nbsp;"]]]
   :asset-prefix "what-is-a-user-interface/"
   :src "markdown/what-is-a-user-interface.md"
   :out "resources/public/what-is-a-user-interface.html"})

(defpost ui-model
  {:id :ui-model
   :title "UI Model"
   :subheading "How to build a functional UI library from scratch: Part II"
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           "&nbsp;"]]]
   :asset-prefix "ui-model/"
   :src "markdown/ui-model.md"
   :out "resources/public/ui-model.html"})

(defpost html-tax-post
  {:id :html-tax
   :title "The HTML Tax"
   :subheading "Html is a poor medium for specifying user interfaces"
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           "&nbsp;"]]]
   :asset-prefix "html-tax/"
   :src "markdown/html-tax.md"
   :out "resources/public/html-tax.html"})

(defn render-post! [{:keys [title
                            subheading
                            nav
                            src
                            out]
                     :as post}]
  (let [page-html (blog-page post)
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

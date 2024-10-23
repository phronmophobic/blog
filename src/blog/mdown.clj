(ns blog.mdown
  (:require [clojure.zip :as z]
            [clojure.data.json :as json]
            [membrane.ui :as ui
             :refer [horizontal-layout
                     vertical-layout
                     spacer
                     on]]
            ;; [membrane.skia :as skia]
            [membrane.component :refer [defui defeffect]
             :as component]
            [membrane.basic-components :as basic
             :refer [textarea checkbox]]
            [clojure.java.io :as io]
            [glow.core :as glow]
            clojure.edn
            [clojure.string :as str]
            glow.parse
            glow.html
            [glow.colorschemes]
            [tiara.data :refer [ordered-map ordered-set oset]]
            [hiccup.util :as hiccup-util]
            [hiccup.core :refer [html]
             :as hiccup])
  (:import com.vladsch.flexmark.util.ast.Node
           com.vladsch.flexmark.html.HtmlRenderer
           com.vladsch.flexmark.parser.Parser
           com.vladsch.flexmark.ext.attributes.AttributesExtension
           com.vladsch.flexmark.ext.xwiki.macros.MacroExtension
           com.vladsch.flexmark.util.data.MutableDataSet
           com.phronemophobic.blog.HiccupNode
           java.text.DecimalFormat))


;; //options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));

(def POSTS (atom (ordered-map)))
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

(defn macro-type [m]
  (if (instance? com.vladsch.flexmark.ext.xwiki.macros.Macro m)
    (.getName m)
    (if (instance? com.vladsch.flexmark.ext.xwiki.macros.MacroBlock m)
      (.getName (.getMacroNode m))
      (type m))))

(defmulti macroexpand1-doc macro-type)

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
   [:div.footnotes
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

(defmulti markdown-macro macro-type)

(defmethod markdown-macro "tableflip" [macro]
  [:p {:title "table flip"} "(╯°□°）╯︵ ┻━┻"])

(defmethod markdown-macro "blockquote-footer" [macro]
  [:footer.blockquote-footer
   (map blog-html (drop-last (children macro)))])

(defmethod markdown-macro "tooltip" [macro]
  (let [tooltip-text-macro (->> (children macro)
                                  (some #(= "tooltip-text" (macro-type %))))
        tooltip-text (get (.getAttributes macro) "text")
        childs (->> (children macro)
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.MacroAttribute %))
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.MacroClose %))
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.Macro %)))]
    (assert tooltip-text "Tooltip requires a text attribute. {{tooltip text='asdf'}}...")
    [:span {:title tooltip-text}
     (map blog-html childs)]))

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


(defmethod markdown-macro "vega-embed" [macro]
  (let [childs (->> (children macro)
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.MacroClose %))
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.Macro %)))
        viz-id (gensym "viz_")]
    (hiccup-util/raw-string
     (str "
<div id=\"" viz-id "\"></div>
  <script type=\"text/javascript\">
      var yourVlSpec_" viz-id " = " (clojure.string/join (map #(.getChars %) childs))
          ";
      vegaEmbed('#"viz-id "', yourVlSpec_" viz-id ", {actions: false});
    </script>"))))

(defmethod markdown-macro "video" [macro]
  (let [childs (->> (children macro)
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.MacroClose %))
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.Macro %)))
        src (clojure.string/join (map #(.getChars %) childs))]
    (hiccup-util/raw-string
     (str
      "<p><video controls preload=\"none\">
  <source src=\""src "\" type=\"video/mp4\" />
</video></p>"))))


(defmethod markdown-macro "vega-embed-edn" [macro]
  (let [childs (->> (children macro)
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.MacroClose %))
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.Macro %)))
        viz-id (gensym "viz_")

        edn-str (clojure.string/join (map #(.getChars %) childs))
        edn (clojure.edn/read-string edn-str)
        vega-spec-json (json/write-str edn)]
    (hiccup-util/raw-string
     (str "
<div id=\"" viz-id "\"></div>
  <script type=\"text/javascript\">
      var yourVlSpec_" viz-id " = " vega-spec-json
          ";
      vegaEmbed('#"viz-id "', yourVlSpec_" viz-id ", {actions: false});
    </script>"))))

(def two-decimal-format (DecimalFormat. "#.##"))
(defmethod markdown-macro "vega-count-chart" [macro]
  (let [childs (->> (children macro)
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.MacroClose %))
                    (remove #(instance? com.vladsch.flexmark.ext.xwiki.macros.Macro %)))
        edn-str (clojure.string/join (map #(.getChars %) childs))
        {:keys [xlabel
                ylabel
                sort-field
                description
                data]
         :as edn} (clojure.edn/read-string edn-str)
        label-prop (str xlabel " str")

        vega-spec {
                   "$schema" "https://vega.github.io/schema/vega-lite/v5.json",
                   "description" description
                   "data" 
                   {"values" (for [[i [k v]] (map-indexed vector (:data edn))]
                               {"_index" i
                                xlabel v
                                label-prop (if (integer? v)
                                             (format "%,d" v)
                                             (.format two-decimal-format v))
                                ylabel (str k)})
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
                                         "text" {"field" label-prop, "type" "nominal"}
                                         }
                             }],

                   "encoding" {
                               "x" {"field" xlabel, "type" "quantitative", },
                               ,
                               "y" (merge
                                    {"field" ylabel, "type" "nominal"}
                                    (if sort-field
                                      {"sort" {"field" sort-field
                                               "op" "max"}}
                                      {"sort" {"field" "_index"
                                               "op" "max"}}))
                               }
                   }

        vega-spec-json (json/write-str vega-spec)

        viz-id (gensym "viz_")]
    [:div {:style "width:100%;overflow:scroll;"}
     (hiccup-util/raw-string
      (str "
<div id=\"" viz-id "\"></div>
  <script type=\"text/javascript\">
      var yourVlSpec_" viz-id " = " vega-spec-json
           ";
      vegaEmbed('#"viz-id "', yourVlSpec_" viz-id ", {actions: false});
    </script>"))]))


(extend-type com.vladsch.flexmark.ext.xwiki.macros.Macro
  IBlogHtml
  (blog-html [this]
    (markdown-macro this)))

(extend-type com.vladsch.flexmark.ext.xwiki.macros.MacroBlock
  IBlogHtml
  (blog-html [this]
    (markdown-macro this
                    #_(.getMacroNode this))))


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
                         body
                         asset-prefix
                         vega?]
                  :as post}]
  (let [body (if body
               body
               (parse-blog src))]
    [:html {:lang "en"}
     [:head

      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, shrink-to-fit=no"}] 
      ;; [:meta {:name "description" :content ""}]
      [:meta {:name "author" :content "Adrian Smith"}]

      ;; <link rel="icon" href="../../favicon.ico">
      [:link {:rel "icon"
              :href (str asset-prefix "favicon.ico")}]
      [:title title]


      (when vega?
        (for [src ["https://cdn.jsdelivr.net/npm/vega@5.22.1"
                   "https://cdn.jsdelivr.net/npm/vega-lite@5.5.0"
                   "https://cdn.jsdelivr.net/npm/vega-embed@6.21.0"]]
          [:script {:src src}]))

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




#_(defpost functional-ui-post
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
   :out "functional-ui.html"})
(defpost dewey-sql
  {:id :dewey-sql
   :title "Dewey SQL"
   :subheading "Analyzing Every Clojure Project with SQL"
   :vega? false
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           {:href "/"}
           "Home"]]]
   :asset-prefix "dewey-sql/"
   :src "markdown/dewey-sql.md"
   :out "dewey-sql.html"})

(defpost clojure-plays-mario
  {:id :mairio
   :title "Clojure Plays Mario"
   ;; :subheading ""
   :vega? false
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           {:href "/"}
           "Home"]]]
   :asset-prefix "mairio/"
   :src "markdown/mairio.md"
   :out "mairio.html"})

(defpost dewey-analysis
  {:id :dewey-analysis
   :title "Analyzing Every Clojure Project on Github"
   ;; :subheading ""
   :vega? true
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           {:href "/"}
           "Home"]]]
   :asset-prefix "dewey-analysis/"
   :src "markdown/dewey-analysis.md"
   :out "dewey-analysis.html"})

(defpost membrane-topics
  {:id :membrane-topics
   :title "Membrane Topics"
   ;; :subheading ""
   :vega? false
   :index? false
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link {:href "https://github.com/phronmophobic/membrane"}
           "Membrane Github"]]]
   :asset-prefix "membrane-topics/"
   :src "/Users/adrian/workspace/membrane/docs/topics.md"
   :out "/Users/adrian/workspace/membrane/docs/membrane-topics.html"})

(defpost what-is-a-ui
  {:id :what-is-a-ui
   :title "What is a User Interface?"
   :subheading "How to build a functional UI library from scratch: Part I"
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           {:href "/"}
           "Home"]]]
   :asset-prefix "what-is-a-user-interface/"
   :src "markdown/what-is-a-user-interface.md"
   :out "what-is-a-user-interface.html"})

(defpost ui-model
  {:id :ui-model
   :title "Implementing a Functional UI Model"
   :subheading "How to build a functional UI library from scratch: Part II"
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           {:href "/"}
           "Home"]]]
   :asset-prefix "ui-model/"
   :src "markdown/ui-model.md"
   :out "ui-model.html"})


(defpost reusable-ui-components
  {:id :reusable-ui-components
   :title "Reusable UI Components"
   :subheading "How to build a functional UI library from scratch: Part III"
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           {:href "/"}
           "Home"]]]
   :asset-prefix "ui-model/"
   :src "markdown/reusable-ui-components.md"
   :out "reusable-ui-components.html"})


(defpost html-tax-post
  {:id :html-tax
   :title "The HTML Tax"
   :subheading "Html is a poor medium for specifying user interfaces"
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           {:href "/"}
           "Home"]]]
   :asset-prefix "html-tax/"
   :src "markdown/html-tax.md"
   :out "html-tax.html"})

(defpost treemap-post
  {:id :treemap
   :title "Treemaps are awesome!"
   :subheading "An alternative to pprint for generically visualizing heterogeneous, hierarchical data"
   :asset-prefix "treemaps-are-awesome/"
   :nav [:div {:class "container"}
         [:nav.nav.blog-nav
          [:a.nav-link
           {:href "/"}
           "Home"]
          [:a.nav-link.active {:href "#"}
           "Treemaps are awesome!"]
          [:a.nav-link {:href "treemap-demo.html"}
           "Treemap Demo"]
          [:a.nav-link {:href "https://github.com/phronmophobic/treemap-clj"}
           "Code on Github"]]]
   :src "markdown/treemaps-are-awesome.md"
   :out "treemaps-are-awesome.html"})

(defn render-post! [{:keys [title
                            subheading
                            nav
                            src
                            out]
                     :as post}]
  (let [page-html (blog-page post)
        html-str (html page-html)
        out-path (if (str/starts-with? out "/")
                   out
                   (io/file "resources" "public" out))]
    (spit out-path html-str)))

(defonce running? (atom false))
(defn watch-blog [post-id]
  (let [post (get @POSTS post-id)
        _ (assert post (str "No post for id " (pr-str post-id) ". \n"
                            (clojure.string/join "\n" (keys @POSTS))) )
        f (clojure.java.io/file (:src post))
        get-val #(.lastModified f)]
    (when (not @running?)
      (reset! running? true)
      (try
        @(future
           (loop [last-val nil]
             (let [current-val (get-val)]
               (when @running?
                 (when (not= current-val last-val)

                   (print "rendering blog...")
                   (flush)
                   (try
                     (render-post! post)
                     (print " done.\n")
                     (catch Exception e
                       (println "error: \n")
                       (prn e)))
                   (flush))

                 (Thread/sleep 500 )
                 (recur current-val)))))
        (finally
          (reset! running? false)))
      nil)))

(defn render-index []
  (let [page-html (blog-page
                   {:title "Phronemophobic's Blog"
                    :body [:div
                           (for [post (vals @POSTS)
                                 :when (get post :index? true)]
                             [:div
                              [:a {:href (:out post)}
                               (:title post)]])]
                    :asset-prefix "/"})
        html-str (html page-html)]
    (spit "resources/public/index.html" html-str)))

(defn render-all! []
  (render-index)
  (run! render-post! (vals @POSTS)))

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

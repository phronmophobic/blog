(ns blog.posts.ui-state.main
  (:require [membrane.ui :as ui
             :refer [horizontal-layout
                     vertical-layout
                     spacer
                     on]]
            [membrane.skia :as skia]
            [membrane.skia :as backend]
            [membrane.component :refer [defui defeffect]
             :as component]
            [membrane.basic-components :as basic
             :refer [textarea checkbox]])
  (:gen-class))


(defn delete-button []
  (ui/with-style :membrane.ui/style-stroke
    (ui/with-color
      [1 0 0]
      (ui/with-stroke-width
        3
        [(ui/path [0 0]
                  [10 10])
         (ui/path [10 0]
                  [0 10])]))))

;; (defui todo-item [ & {:keys [todo]}]
;;   (horizontal-layout
;;    (on :mouse-down (fn [[mx my]]
;;                      [[:delete $todo]])
;;        (delete-button))
;;    (basic/checkbox :checked? (:complete? todo))
;;    (basic/textarea :text (:description todo))))

;; (defui todo-item [ & {:keys [todo]}]
;;   (horizontal-layout
;;    (on :mouse-down (fn [[mx my]]
;;                      [[:delete $todo]])
;;        (delete-button))
;;    (checkbox :checked? (:complete? todo))
;;    (textarea :text (:description todo))))

(comment
  (skia/draw-to-image!
   "resources/public/ui-state/todo-item.png"
   (ui/padding 2 2
               (todo-item :todo
                          {:complete? false
                           :description "fix me"}))
   ))

(comment
  (skia/run (component/make-app #'todo-item {:todo
                                             {:complete? false
                                              :description "fix me"}})))

(defeffect ::add-todo [$todos]
  (dispatch! :update $todos #(conj % {:description ""
                                      :complete? false})))

;; (defui todo-list [ & {:keys [todos]}]
;;   (vertical-layout
;;    (horizontal-layout
;;     (basic/button :text "Add Todo"
;;                   :on-click (fn []
;;                               [[::add-todo $todos]])))
;;    (apply
;;     vertical-layout
;;     (for [todo todos]
;;       (todo-item :todo todo)))))

(comment
  (skia/run (component/make-app #'todo-list {:todos
                                             [{:complete? false
                                               :description "fix me"}
                                              {:complete? false
                                               :description "fix me"}]})))



(defn -main [ & args]
  (println "hello"))


;; (defui hello-component [& {:keys [prop1 prop2 prop3]}]
;;   ;; return a view
;;   (ui/label "Hello"))

(defn my-checkbox [checked]
  (on
   :mouse-down (fn [mpos]
                 [[::toggle]])
   (ui/checkbox checked)))

(defn control-panel [{:keys [thrusters
                             shield
                             windshield-wipers]}]
  (vertical-layout
   (on ::toggle (fn [] [[::toggle-thrusters]])
       (my-checkbox thrusters))
   (on ::toggle (fn [] [[::toggle-shield]])
       (my-checkbox shield))
   (on ::toggle (fn [] [[::toggle-windshield-wipers]])
       (my-checkbox windshield-wipers))))


;; (defui print-positions [& {:keys []}]
;;   (ui/wrap-on
;;    :mouse-down (fn [handler mpos]
;;                  (let [steps (handler mpos)]
;;                    (when-let [step (first steps)]
;;                      (prn (mapv int mpos) step))
;;                    steps))
;;    (control-panel {})))

;; (def  cp (control-panel {}))
  
;; (ui/mouse-down cp [8 6]) ;; [:blog.posts.ui-state.main/toggle-thrusters]
;; (ui/mouse-down cp [8 16]) ;; [:blog.posts.ui-state.main/toggle-shield]
;; (ui/mouse-down cp [8 26]) ;; [:blog.posts.ui-state.main/toggle-windshield-wipers]
  


(comment
  (skia/run (component/make-app #'print-positions
                                [])))

(defprotocol IFoo
  (foo [this]))


(defn delete-button []
  (ui/with-style :membrane.ui/style-stroke
    (ui/with-color
      [1 0 0]
      (ui/with-stroke-width
        3
        [(ui/path [0 0]
                  [10 10])
         (ui/path [10 0]
                  [0 10])]))))

;; (defui todo-item [ & {:keys [todo]}]
;;   (horizontal-layout
;;    (on :mouse-down (fn [[mx my]]
;;                      [[:delete $todo]])
;;        (delete-button))
;;    (basic/checkbox :checked? (:complete? todo))
;;    (basic/textarea :text (:description todo))))

;; (defui todo-item [ & {:keys [todo]}]
;;   (horizontal-layout
;;    (on :mouse-down (fn [[mx my]]
;;                      [[:delete $todo]])
;;        (delete-button))
;;    (checkbox :checked? (:complete? todo))
;;    (textarea :text (:description todo))))

(comment
  (skia/draw-to-image!
   "resources/public/ui-state/todo-item.png"
   (ui/padding 2 2
               (todo-item :todo
                          {:complete? false
                           :description "fix me"}))
   ))

(comment
  (skia/run (component/make-app #'todo-item {:todo
                                             {:complete? false
                                              :description "fix me"}})))

(defeffect ::add-todo [$todos]
  (dispatch! :update $todos #(conj % {:description ""
                                      :complete? false})))

;; (defui todo-list [ & {:keys [todos]}]
;;   (vertical-layout
;;    (horizontal-layout
;;     (basic/button :text "Add Todo"
;;                   :on-click (fn []
;;                               [[::add-todo $todos]])))
;;    (apply
;;     vertical-layout
;;     (for [todo todos]
;;       (todo-item :todo todo)))))

(comment
  (skia/run (component/make-app #'todo-list {:todos
                                             [{:complete? false
                                               :description "fix me"}
                                              {:complete? false
                                               :description "fix me"}]})))
(defn counter-ui [num]
  (ui/horizontal-layout
   (ui/on
    :mouse-down (fn [_]
                  [[:increment-num]])
    (ui/button "More!"))
   (ui/label (str "current count: " num))))


(backend/run
  (fn []
    (with-effect-handler my-counter-effect-handler
      (ui/padding 10 10
                  (counter-ui (my-counter-effect-handler [:get-count]))))))



;; handler for [:inc-counter counter-key]
(defn inc-counter-effect [db k]
  (swap! db update k (fnil inc 0)))

;; handler for [:dec-counter counter-key]
(defn dec-counter-effect [db k]
  (swap! db update k (fnil dec 0)))

;; handler for [:get-count counter-key]
(defn get-count [db k]
  (get @db k 0))

(defn make-counter-effect-handler []
  (let [db (atom {})]
    (fn [intent]
      (let [intent-type (first intent)
            intent-args (rest intent)]
        (case intent-type
          :inc-counter (apply inc-counter-effect db intent-args)
          :dec-counter (apply dec-counter-effect db intent-args)
          :get-count (apply get-count db intent-args))))))

;; usage
(def my-counter-effect-handler (make-counter-effect-handler))

(my-counter-effect-handler [:get-count :a])
;; 0
(my-counter-effect-handler [:inc-counter :a])
;; 1
(my-counter-effect-handler [:get-count :a])
;; 1
(my-counter-effect-handler [:dec-counter :a])
;; 0
(my-counter-effect-handler [:get-count :a])
;; 0

(defn counter-ui [num]
  (ui/horizontal-layout
   (ui/on
    :mouse-down (fn [_]
                  [[:increment-counter]])
    (ui/button "More!"))
   (ui/label (str "current count: " num))))

(ui/mouse-down (counter-ui 10)
               [3 3])




(defn multi-counter-ui [counters]
  (vec
   (for [[i num] (map-indexed vector counters)]
     (ui/translate 0 (* 20 i)
                   (ui/on
                    :increment-counter
                    (fn []
                      [[:increment-counter i]])
                    (counter-ui num))))))

(comment
  (skia/run #(ui/wrap-on
              :mouse-down
              (fn [handler [mx my]]
                (prn [mx my]
                     (handler [mx my])))
              (multi-counter-ui
               [0 0 0 0]))))

(ui/mouse-down (multi-counter-ui [0 0 0])
               [0 20])


(defn many-multi-counter-ui [counter-lists]
  (vec
   (for [[i [k counters]] (map-indexed vector counter-lists)]
     (ui/translate (* 250 i) 0
                   (ui/on
                    :increment-counter
                    (fn [j]
                      [[:increment-counter [k j]]])
                    (multi-counter-ui counters))))))

(def test-ui (many-multi-counter-ui
              {:foo [0 0 0 0]
               :bar [0 0 0 0]
               :baz [0 0 0 0]}))

;; UI is a grid with columns 250px apart and rows 20px apart
(for [col (range 3)
      row (range 4)]
  [[col row] (ui/mouse-down test-ui
                            [(* col 250) (* row 20)])])
;; ([[0 0] ([:increment-counter [:foo 0]])]
;;  [[0 1] ([:increment-counter [:foo 1]])]
;;  [[0 2] ([:increment-counter [:foo 2]])]
;;  [[0 3] ([:increment-counter [:foo 3]])]
;;  [[1 0] ([:increment-counter [:bar 0]])]
;;  [[1 1] ([:increment-counter [:bar 1]])]
;;  [[1 2] ([:increment-counter [:bar 2]])]
;;  [[1 3] ([:increment-counter [:bar 3]])]
;;  [[2 0] ([:increment-counter [:baz 0]])]
;;  [[2 1] ([:increment-counter [:baz 1]])]
;;  [[2 2] ([:increment-counter [:baz 2]])]
;;  [[2 3] ([:increment-counter [:baz 3]])])





;; 

(comment
  (skia/run #(ui/wrap-on
              :mouse-down
              (fn [handler [mx my]]
                (prn [mx my]
                     (handler [mx my])))
              (many-multi-counter-ui
               {:foo [0 0 0 0]
                :bar [0 0 0 0]
                :baz [0 0 0 0]})))
  ,)


{:todo-lists
 {0 {:name "Work"
     :todos [{:done false
              :description "fix bugs"}
             {:done false
              :description "ship it"}]}
  1 {:name "Home"
     :todos [{:done false
              :description "fix bugs"}
             {:done false
              :description "ship it"}]}}}


(defn inc-counter-effect [db]
  (swap! db inc))

;; handler for [:get-count]
(defn get-count [db]
  @db)

;; helper function for creating an effect handler
(defn make-counter-effect-handler []
  (let [db (atom 0)]
    (fn [intent]
      (let [intent-type (first intent)]
        (case intent-type
          :inc-counter (inc-counter-effect db)
          :get-count (get-count db))))))

;; usage
(def my-counter-effect-handler (make-counter-effect-handler))

(my-counter-effect-handler [:get-count])
;; 0
(my-counter-effect-handler [:inc-counter])
;; 1
(my-counter-effect-handler [:get-count])
;; 1


(defn counter-ui [num]
  (ui/horizontal-layout
   (ui/on
    :mouse-down (fn [_]
                  [[:inc-counter]])
    (ui/button "More!"))
   (ui/label (str "current count: " num))))

;; check to make sure
;; :increment-counter intent is returned when
;; a mouse down event occurs above the button
(ui/mouse-down (counter-ui 10)
               [0 0])
;; ([:inc-counter])

;; Using same effect handler in the Effects example
(def my-counter-effect-handler (make-counter-effect-handler))


(my-counter-effect-handler [:get-count])
;; 0

(run! my-counter-effect-handler
      (ui/mouse-down (counter-ui 10)
                     [0 0]))

(my-counter-effect-handler [:get-count])
;; 1

(defn with-effect-handler [handler body]
  (ui/on-bubble (fn [intents]
                  (run! handler intents))
                body))

(require '[membrane.skia :as backend])
(backend/run
  (fn []
    (with-effect-handler my-counter-effect-handler
      (counter-ui (my-counter-effect-handler [:get-count])))))


(defui counter-ui [{:keys [num]}]
  (ui/horizontal-layout
   (ui/on
    :mouse-down (fn [_]
                  [[:inc-counter $num]])
    (ui/button "More!"))
   (ui/label (str "current count: " num))))

(defeffect :inc-counter [$num]
  (dispatch! :update $num inc))

(ui/mouse-down (counter-ui {:num 10})
               [0 0])
;; ([:inc-counter [(keypath :num)]])

(backend/run (component/make-app #'counter-ui {:num 42}))

(backend/run
  (fn []
    (with-effect-handler my-counter-effect-handler
      (counter-ui {:num (my-counter-effect-handler [:get-count])}))))



(def todo-lists
  [{:name "Work"
    :todos [{:done false
             :description "fix bugs"}
            {:done false
             :description "ship it"}]}
   {:name "Home"
    :todos [{:done false
             :description "fix bugs"}
            {:done false
             :description "ship it"}]}])

(defui todo-list-ui [{:keys [todo-lists]}]
  (apply
   ui/horizontal-layout
   (for [todo-list todo-lists]
     (apply
      ui/vertical-layout
      (ui/button "Add todo"
                 (fn []
                   [[:add-todo $todo-list]]))
      (ui/label (:name todo-list))
      (let [todos (:todos todo-list)]
        (for [todo todos]
          (ui/horizontal-layout
           (on
            :mouse-down
            (fn [_]
              [[:toggle-todo $todo]])
            (ui/checkbox (:done todo)))
           (ui/label (:description todo)))))))))

(defeffect :add-todo [$todo-list]
  (dispatch! :update $todo-list update :todos
             conj {:description "adsf"
                   :done false}))

(defeffect :toggle-todo [$todo]
  (prn $todo)
  (dispatch! :update $todo update :done not))

(def todo-state (atom {:todo-lists todo-lists}))

(backend/run (component/make-app #'todo-list-ui todo-state))

(defui my-component [{:keys [a b private]}]
  (let [private-num (:num private)
        private-str (:str private)]
    ...))

(defui my-component [{:keys [a
                             b
                             ^:membrane.component/contextual my-context]}]
  ...)


(require '[membrane.skia :as backend]
         '[membrane.ui :as ui])
(backend/run #(ui/label "Hello World"))


(defn flatten-a-sequence [s]
  (reduce (fn myflatten [collection element]
            (if (sequential? element)
              (reduce myflatten collection element)
              (conj collection element))) [] s))

(require '[clojure.zip :as z])

(defn zip-flatten [xs]
  (let [zip (z/zipper seqable? seq #(into (empty %1) %2) xs)]
    (loop [flattened []
           zip zip]
      (if (z/end? zip)
        flattened
        (let [x (z/node zip)]
          (if (seqable? x)
            (recur flattened (z/next zip))
            (recur (conj flattened x)
                   (z/next zip))))))))

(def nested-data {:a {:b {:c 1}}})

(defui nested-view [{:keys [a]}]
  (let [b (:b a)
        c (:c b)
        d (:d c)]
    (ui/button "More!"
               (fn []
                 [[:inc-counter $d]]))))


(ui/mouse-down (nested-view nested-data)
               [0 0])
(def effect-fire-missiles!
  (let [effect (fn [dispatch! missile target]
                 (fire-missile! missile target))]
    (swap! membrane.component/effects
           assoc :blog.mdown/fire-missiles! effect)    
    effect))


(macroexpand-1 '(defeffect ::fire-missiles! [missile target]
                  (fire-missile! missile target)))

(let [fvar (defn effect-fire-missiles! [dispatch! missile target]
             (fire-missile! missile target))]
  (swap! membrane.component/effects
         assoc
         :blog.mdown/fire-missiles! effect-fire-missiles!)
  fvar)


(def nested-data {:a {:b {:c {:d 1}}}})

(defui more-button [{:keys [num]}]
  (ui/button "More!"
             (fn []
               [[:inc-counter $num]])))

(defui nested-view [{:keys [a]}]
  (let [b (:b a)
        c (:c b)
        d (:d c)]
    (more-button {:num d})))

(ui/mouse-down (nested-view nested-data)
               [0 0])

(def other-nested-data {:foo {:bar {:baz 1}}})
(defui other-nested-view [{:keys [foo]}]
  (let [bar (:bar foo)
        baz (:baz bar)]
    (more-button {:num baz})))

(ui/mouse-down (other-nested-view other-nested-data)
               [0 0])
;; ([:inc-counter [(keypath :foo)
;;                 (keypath :bar)
;;                 (keypath :baz)]])

(def app-state (atom other-nested-data))
(skia/run (component/make-app #'other-nested-view app-state))

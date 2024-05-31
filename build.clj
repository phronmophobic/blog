(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.string :as str]))

(def lib 'com.phronemophobic/blog)
(def version "0.1")

(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
  (b/delete {:path "target"}))


(defn compile [_]
  (b/javac {:src-dirs ["src-java"]
            :class-dir class-dir
            :basis basis
            ;;:javac-opts ["-source" "8" "-target" "8"]
            }))

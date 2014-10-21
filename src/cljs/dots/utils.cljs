(ns dots.utils
  (:require [clojure.string :refer [split]]))

(defn log<- [thing]
  (. js/console log (clj->js thing)))

(defn parse-int [s]
  (js/parseInt s))

(defn key-or-int
  ([i prefix] (->> i (str prefix) keyword))
  ([i] (-> i name (split #"-") last parse-int)))

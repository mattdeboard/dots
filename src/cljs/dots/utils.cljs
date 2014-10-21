(ns dots.utils)

(defn log<- [thing]
  (. js/console log (clj->js thing)))

(defn key-or-int
  [i prefix-or-sep]
  (cond
   (integer? i) (->> i (str prefix i) keyword)
   (keyword? i) (-> i
                    name
                    (clojure.string/split (re-pattern prefix-or-sep))
                    last
                    js/parseInt)))


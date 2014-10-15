(ns dots.utils)

(defn log<- [thing]
  (. js/console log (clj->js thing)))

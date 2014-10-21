(ns dots.appstate
  (:require [dots.components.screen :refer [rand-colors]]
            [dots.utils :refer [key-or-int]]))

(def BOARD-SIZE 6)

(defn- column-state [column]
  {(key-or-int column "col-")
   {:column column
    :rows-map (zipmap (map #(key-or-int % "row-") (range BOARD-SIZE))
                      (rand-colors nil))}})

(defn- columns []
  (apply merge (map column-state (range BOARD-SIZE))))

(def app-state
  (atom

   {;; `ui' is the state of the UI outside the gameplay area.
    :ui {:active-view "score-screen"
         :board-size BOARD-SIZE}

    ;; `game-state' is the state of the gameplay area.
    :game-state {:time 900
                 :score 0
                 :game-complete? false
                 :columns (columns)}
    }))


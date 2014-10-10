(ns dots.components.board
  (:require [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true]))

(defn header-col
  "Component for individual column headers (i.e. Time and Score)."
  [props owner]
  (reify
    om/IRender
    (render [_]
      (let [text (-> props :title clojure.string/capitalize (str " "))
            elem-id (-> props :title (str "-val"))]
        (d/div
         #js {:className "heads"}
         text
         (d/span #js {:id elem-id} (:val props)))))))

(defn header
  "Component for the game board header."
  [props owner]
  (reify
    om/IInitState
    (init-state [_]
      {:time 60 :score 0})

    om/IRender
    (render [_]
      (let [{:keys [time score]} (om/get-state owner)]
        (d/div
         #js {:className "header"}
         (om/build header-col {:title "time" :val time})
         (om/build header-col {:title "score" :val score}))))))

(defn board-area [props owner]
  (reify
    om/IRender
    (render [this]
      (d/div #js {:className "board-area"}
       (d/div #js {:className "chain-line"}
        (d/div #js {:className "dot-highlights"}
         (d/div #js {:className "board"})))))))

(defn game-board [props owner]
  (reify
    om/IRender
    (render [this]
      (d/div
       #js {:className "dots-game" :id "main"}
       (om/build header nil)
       (om/build board-area nil)))))

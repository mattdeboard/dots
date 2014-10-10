(ns dots.core
  (:require [dots.dev :refer [is-dev?]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true]))

(defonce app-state (atom {:text "Hello Chestnut!"}))

(defn header [props owner]
  (reify
    om/IRender
    (render [this]
      (d/div #js {:className "header"}
             (d/div #js {:className "heads"} "Time "
                    (d/span #js {:className "time-val"}))
             (d/div #js {:className "heads"} "Score "
                    (d/span #js {:className "score-val"}))))))

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
       #js {:className "dots-game"}
       (om/build header nil)
       (om/build board-area nil)))))

(om/root
 (fn [app owner]
   (reify om/IRender
     (render [_]
       (om/build game-board nil))))
 app-state
 {:target (. js/document (getElementById "app"))})

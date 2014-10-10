(ns dots.core
  (:require [dots.components.board :refer [game-board]]
            [dots.dev :refer [is-dev?]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true]))

(defonce app-state (atom {}))

(om/root
 (fn [app owner]
   (reify om/IRender
     (render [_]
       (om/build game-board nil))))
 app-state
 {:target (. js/document (getElementById "dots-game-container"))})

(ns dots.core
  (:require [dots.chans :as ch]
            [dots.components.main :refer [game-container]]
            [dots.components.screen :refer [rand-colors]]
            [dots.dev :refer [is-dev?]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true]))

(def app-state (atom {:active-view "score-screen"
                      :board-size 6
                      :header {:time 60 :score 0}}))

(om/root
 (fn [app owner]
   (reify om/IRender
     (render [_]
       (om/build game-container @app-state))))
 app-state
 {:target (. js/document (getElementById "app"))})


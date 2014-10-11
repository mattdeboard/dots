(ns dots.core
  (:require [dots.chans :as ch]
            [dots.components.main :refer [game-container]]
            [dots.dev :refer [is-dev?]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true]))

(defonce app-state (atom {:active-view "score-screen"}))

(om/root
 (fn [app owner]
   (reify om/IRender
     (render [_]
       (om/build game-container @app-state))))
 app-state
 {:target (. js/document (getElementById "app"))})

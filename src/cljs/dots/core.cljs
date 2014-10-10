(ns dots.core
    (:require [dots.dev :refer [is-dev?]]
              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]))

(defonce app-state (atom {:text "Hello Chestnut!"}))

(om/root
  (fn [app owner]
    (reify om/IRender
      (render [_]
        (dom/h1  nil(:text app)))))
  app-state
  {:target (. js/document (getElementById "app"))})

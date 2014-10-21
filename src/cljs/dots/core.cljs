(ns dots.core
  (:require [cljs.core.async :as async]
            [dots.appstate :refer [app-state]]
            [dots.chans :as ch]
            [dots.components.main :refer [game-container]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(om/root
 (fn [app owner]
   (reify om/IRender
     (render [_]
       (om/build game-container app))))
 app-state
 {:target (. js/document (getElementById "app"))
  :shared {:timer-pub-chan (async/pub ch/timer-chan #(:topic %))
           :remove-pub-chan (async/pub ch/remove-chan #(:topic %))
           :click-pub-chan (async/pub ch/click-chan #(:topic %))
           :trans-pub-chan (async/pub ch/transition-chan #(:topic %))}})

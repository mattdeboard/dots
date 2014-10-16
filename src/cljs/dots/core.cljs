(ns dots.core
  (:require [cljs.core.async :as async]
            [dots.chans :as ch]
            [dots.components.main :refer [game-container]]
            [dots.components.screen :refer [rand-colors]]
            [dots.dev :refer [is-dev?]]
            [dots.utils :refer [log<-]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def app-state (atom {:ui {:active-view "score-screen" :board-size 6}
                      :game-state {:time 1 :score 0 :game-complete? false}}))

(om/root
 (fn [app owner]
   (reify om/IRender
     (render [_]
       (om/build game-container app))))
 app-state
 {:target (. js/document (getElementById "app"))
  :shared {:timer-pub-chan (async/pub ch/timer-chan #(:topic %))}})


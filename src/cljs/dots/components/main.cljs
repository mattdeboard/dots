(ns dots.components.main
  (:require [cljs.core.async :as async :refer [chan <!]]
            [dots.appstate :refer [app-state]]
            [dots.chans :refer [timer-chan]]
            [dots.components.board :refer [game-board]]
            [dots.components.screen :refer [score-screen]]
            [dots.utils :refer [log<-]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn ui-state-cur []
  (om/ref-cursor (:ui (om/root-cursor app-state))))

(defn handle-click [e owner cursor]
  (go (>! timer-chan {:topic :game-complete})))

(defn display [name owner]
  (if (= name (om/get-state owner :active-view))
    "inline" "none"))

(defn switch-screen [owner]
  (let [current-view (om/get-state owner :active-view)]
    (if (= current-view "game-board") "score-screen" "game-board")))

(defn switch-active-view [channel owner]
  (let [cur (ui-state-cur)]
    (go-loop []
      (let [next-view (switch-screen owner)
            topic (<! channel)]
        (om/update! cur :active-view next-view))
    (recur))))

(defn game-container [props owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [timer-pub-chan (om/get-shared owner :timer-pub-chan)
            timer-sub-chan (chan)]
        (async/sub timer-pub-chan :game-complete timer-sub-chan)
        (switch-active-view timer-sub-chan owner))
      (om/set-state! owner :active-view (get-in props [:ui :active-view])))

    om/IWillReceiveProps
    (will-receive-props [this next-props]
      (let [view (get-in next-props [:ui :active-view])]
        (if (not= view (om/get-state owner :active-view))
          (om/set-state! owner :active-view view))))

    om/IRender
    (render [_]
      (log<- (om/get-state owner :active-view))
      (d/div
       #js {:className "dots-game-container no scroll"
            :ondragstart "return false;"
            :ondrop "return false;"}
       (om/build score-screen
                 {:game-state (get props :game-state)
                  :click-handler #(handle-click % owner props)
                  :style {:display (display "score-screen" owner)}}
                 {:react-key "score-screen"})
       (om/build game-board
                 {:game-state (get props :game-state)
                  :ui (get props :ui)
                  :style {:display (display "game-board" owner)}}
                 {:react-key "game-board"})))))

(ns dots.components.main
  (:require [cljs.core.async :as async :refer [chan <!]]
            [dots.chans :refer [timer-chan]]
            [dots.components.board :refer [game-board]]
            [dots.components.screen :refer [score-screen]]
            [dots.utils :refer [log<-]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn handle-click [e owner cursor]
  (go (>! timer-chan {:topic :game-complete})))

(defn active?
  "Returns a boolean indicating whether or not the named component is
  'active'.

  This can be used for multiple reasons, but one is to determine the
  display style of a component. For example, if this function returns
  false, then the `:style' would be `{:display \"none\"}'."
  [name cursor]
  (= name (get-in cursor [:ui :active-view])))

(defn switch-screen [owner]
  (let [current-view (om/get-state owner :active-view)]
    (if (= current-view "game-board") "score-screen" "game-board")))

(defn switch-active-view [channel owner]
  (go-loop []
    (let [next-view (switch-screen owner)
          topic (<! channel)]
      (om/set-state! owner :active-view next-view)
    (recur)))

(defn game-container [props owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [shared-chan (om/get-shared owner :timer-pub-chan)
            timer-sub-chan (chan)]
        (async/sub shared-chan :game-complete timer-sub-chan)
        (switch-active-view timer-sub-chan owner))
      (om/set-state! owner :active-view (get-in props [:ui :active-view])))

    om/IWillReceiveProps
    (will-receive-props [this next-props]
      (let [view (get-in next-props [:ui :active-view])]
        (if (not= view (om/get-state owner :active-view))
          (om/set-state! owner :active-view view))))

    om/IRender
    (render [_]
      (d/div
       #js {:className "dots-game-container no scroll"
            :ondragstart "return false;"
            :ondrop "return false;"}
       (om/build score-screen
                 {:game-state (get props :game-state)
                  :click-handler #(handle-click % owner props)
                  :style {:display (if (active? "score-screen" props)
                                     "inline" "none")}}
                 {:react-key "score-screen"})
       (om/build game-board
                 {:game-state (get props :game-state)
                  :ui (get props :ui)
                  :style {:display (if (active? "game-board" props)
                                     "inline" "none")}}
                 {:react-key "game-board"})))))

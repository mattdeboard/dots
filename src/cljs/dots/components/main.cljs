(ns dots.components.main
  (:require [cljs.core.async :as async :refer [chan <!]]
            ; [dots.chans :refer [timer-chan]]
            [dots.components.board :refer [game-board]]
            [dots.components.screen :refer [score-screen]]
            [dots.utils :refer [log<-]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn handle-click [e owner cursor]
  (let [active (if (= (om/get-state owner :active-view) "score-screen")
                 "game-board" "score-screen")]
    (om/transact!
     cursor :ui
     ;; We need to update the value of `(get-in cursor [:ui :active-view])',
     ;; and ths is the only way I know of to do that. Inside the transaction
     ;; handler, we get the current view, switch to the opposite view, then
     ;; return a hashmap that updates `:active-view'.
     (fn [m] (let [current-view (:active-view m)
                   next-view (if (= current-view "score-screen")
                               "game-board" "score-screen")]
               (merge m {:active-view next-view}))))
    ;; We need to reset game state back to the proper values when clicking on
    ;; the "new game" button at the end of the game.
    (om/transact!
     cursor :game-state
     (fn [m] (merge m {:score 0 :game-complete? false})))))

(defn active?
  "Returns a boolean indicating whether or not the named component is
  'active'.

  This can be used for multiple reasons, but one is to determine the
  display style of a component. For example, if this function returns
  false, then the `:style' would be `{:display \"none\"}'."
  [name cursor]
  (= name (get-in cursor [:ui :active-view])))

(defn switch-active-view [channel owner]
  (go-loop []
    (let [current-view (om/get-state owner :active-view)
          next-view (if (= current-view "game-board")
                      "score-screen" "game-board")
          topic (<! channel)]
      (log<- {:current current-view :next next-view})
      (om/set-state! owner {:active-view next-view}))
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
          (om/set-state! owner {:active-view view}))))

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

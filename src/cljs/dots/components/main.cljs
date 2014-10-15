(ns dots.components.main
  (:require [dots.components.board :refer [game-board]]
            [dots.components.screen :refer [score-screen]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true]))

(defn handle-click [e owner cursor]
  (let [active (if (= (om/get-state owner :active-view) "score-screen")
                 "game-board" "score-screen")]
    (om/transact! cursor :active-view #(if (= % "score-screen")
                                         "game-board" "score-screen"))))

(defn active? [name cursor]
  (= name (get cursor :active-view)))

(defn game-container [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :active-view (get cursor :active-view)))

    om/IWillReceiveProps
    (will-receive-props [this next-props]
      (if (not= (:active-view next-props) (om/get-state owner :active-view))
        (om/set-state! owner {:active-view (:active-view next-props)})))

    om/IRender
    (render [_]
      (d/div
       #js {:className "dots-game-container no scroll"
            :ondragstart "return false;"
            :ondrop "return false;"}
       (om/build score-screen
                 {:click-handler #(handle-click % owner cursor)
                  :header (get cursor :header)
                  :style {:display (if (active? "score-screen" cursor)
                                     "inline" "none")}}
                 {:react-key "score-screen"})
       (om/build game-board
                 {:board-size (:board-size cursor)
                  :header (get cursor :header)
                  :style {:display (if (active? "game-board" cursor)
                                     "inline" "none")}}
                 {:react-key "game-board"}))))))

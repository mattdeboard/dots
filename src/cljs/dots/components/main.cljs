(ns dots.components.main
  (:require [dots.components.board :refer [game-board]]
            [dots.components.screen :refer [score-screen]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true]))

(defn handle-click [e owner]
  (let [active (if (= (om/get-state owner :active) "score-screen")
                 "game-board" "score-screen")]
    (om/set-state! owner :active active)))

(defn game-container [props owner]
  (reify
    om/IInitState
    (init-state [_]
      {:active "score-screen"})

    om/IWillReceiveProps
    (will-receive-props [this next-props]
      (if (not= (:active next-props) (om/get-state owner :active))
        (om/set-state! owner {:active (:active next-props)})))

    om/IRender
    (render [_]
      (let [component (if (= (om/get-state owner :active) "score-screen")
                        score-screen game-board)
            view (om/build component
                           {:board-size (:board-size props)
                            :click-handler #(handle-click % owner)
                            :header (:header props)}
                           {:react-key (:active props)})]
       (d/div
         #js {:className "dots-game-container no scroll"
              :ondragstart "return false;"
              :ondrop "return false;"}
         view)))))

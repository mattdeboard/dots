(ns dots.components.board
  (:require [dots.components.screen :refer [rand-colors]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true]))

(defn header-col
  "Component for individual column headers (i.e. Time and Score)."
  [props owner]
  (reify
    om/IRender
    (render [_]
      (let [text (-> props :title clojure.string/capitalize (str " "))
            elem-id (-> props :title (str "-val"))]
        (d/div
         #js {:className "heads"}
         text
         (d/span #js {:id elem-id} (:val props)))))))

(defn header
  "Component for the game board header."
  [props owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (js/setInterval
       (fn []
         (if (om/cursor? props)
           (om/transact! props :time dec)
           (swap! props assoc :time dec)))
       1000))

    om/IRender
    (render [_]
      (let [{:keys [time score]} props]
        (d/div
         #js {:className "header"}
         (om/build header-col {:title "time" :val (get props :time)})
         (om/build header-col {:title "score" :val score}))))))

(defn dot [props owner]
  ;; A component for an individual dot on the game board.
  (reify
    om/IWillMount
    (will-mount [_]
      (om/set-state! owner :color (:color props))
      (om/set-state! owner :column (:column props))
      (om/set-state! owner :row (:row props)))

    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      (doseq [k [:color :column :row]]
        (let [next-k (k next-props)]
          (if (not= k next-k) (om/set-state! owner k next-k)))))

    om/IRenderState
    (render-state [_ state]
      (let [color (:color state)
            col (:column state)
            row (:row state)
            className (str "dot levelish " (name color) " level-" row)
            left (str (+ 23 (* 45 col)) "px")]
        (d/div #js {:className className
                    :style #js {:top "-112px" :left left}})))))

(defn board-area [props owner]
  (reify
    om/IRender
    (render [this]
      (let [board-size (:board-size props)
            dots (for [col (range board-size) row (range board-size)]
                   {:column col :row row :color (first (take 1 (rand-colors nil)))})
            grid (om/build-all dot dots)]
        (d/div #js {:className "board-area"}
               (d/div #js {:className "chain-line"})
               (d/div #js {:className "dot-highlights"})
               (apply d/div #js {:className "board"} grid))))))

(defn game-board [cursor owner]
  (reify
    om/IRender
    (render [this]
      (d/div
       #js {:className "dots-game" :id "main"}
       (om/build header (get cursor :header))
       (om/build board-area props)))))

(ns dots.components.board
  (:require [cljs.core.async :as async :refer [>! <! chan]]
            [dots.chans :refer [timer-chan]]
            [dots.components.screen :refer [rand-colors]]
            [dots.utils :refer [log<-]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

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
  [cursor owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [timer-id (js/setInterval
                      (fn []
                        (let [time (om/get-state owner :time)]
                          (cond
                           (> time 0) (om/update-state! owner :time dec)
                           :else (om/set-state! owner :time 0))))
                      1000)]
        (om/set-state! owner :timer-id timer-id)))

    om/IDidUpdate
    (did-update [_ prev-props prev-state]
      (if (= (:time prev-state) 0)
        (let [timer-id (om/get-state owner :timer-id)]
          (js/clearInterval timer-id)
          (go (>! timer-chan {:topic :game-complete})))))

    om/IRender
    (render [_]
      (let [{:keys [time score]} (om/get-state owner)]
        (d/div
         #js {:className "header"}
         (om/build header-col {:title "time" :val time})
         (om/build header-col {:title "score" :val score}))))))

(defn dot
  "Component for an individual dot."
  [props owner]
  (reify
    ;; om/IWillMount
    ;; (will-mount [_]
    ;;   (om/set-state! owner :color (:color props))
    ;;   (om/set-state! owner :column (:column props))
    ;;   (om/set-state! owner :row (:row props)))

    ;; om/IWillReceiveProps
    ;; (will-receive-props [_ next-props]
    ;;   (doseq [k [:color :column :row]]
    ;;     (let [next-k (k next-props)]
    ;;       (if (not= k next-k) (om/set-state! owner k next-k)))))

    om/IRender
    (render [_]
      (let [color (:color props)
            col (:column props)
            row (:row props)
            className (str "dot levelish " (name color) " level-" row)
            left (str (+ 23 (* 45 col)) "px")]
        (d/div #js {:className className
                    :style #js {:top "-112px" :left left}})))))

(defn board-area [props owner]
  (reify
    om/IRender
    (render [this]
      (let [board-size (get-in props [:ui :board-size])
            dots (for [col (range board-size) row (range board-size)]
                   {:column col :row row :color (first (take 1 (rand-colors nil)))})
            grid (om/build-all dot dots)]
        (d/div
         #js {:className "board-area"}
         (d/div #js {:className "chain-line"})
         (d/div #js {:className "dot-highlights"})
         (apply d/div #js {:className "board"} grid))))))

(defn game-board [props owner]
  (reify
    om/IDisplayName
    (display-name [_] "game-board")

    om/IRender
    (render [this]
      (d/div
       #js {:className "dots-game" :id "game-board"
            :style (clj->js (:style props))}
       ;; Only render the header if we're the active element on the page.
       ;; This way, the timer doesn't start until it's visible on the page.
       ;; Honestly this feels a little hacky.
       (if (= (get-in props [:style :display]) "inline")
         (om/build header {:ui (get props :ui)
                           :game-state (get props :game-state)}
                   {:init-state
                    {:time (get-in props [:game-state :time])
                     :score (get-in props [:game-state :score])}}))
       (om/build board-area props)))))

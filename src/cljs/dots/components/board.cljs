(ns dots.components.board
  (:require [cljs.core.async :as async :refer [>! <! chan]]
            [clojure.string :refer [join]]
            [dots.chans :refer [timer-chan click-chan]]
            [dots.components.screen :refer [rand-colors]]
            [dots.utils :refer [log<-]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defn- left-pos [col]
  (+ 23 (* 45 col)))

(defn adjacent? [dot1 dot2]
  (let [abs #(. js/Math abs %)]
  (if (or
       ;; horizontal adjacency: same row, different columns
       (and (= (:row dot1) (:row dot2))
            (= 1 (abs (- (:column dot1) (:column dot2)))))
       ;; vertical adjacency: same column, different rows
       (and (= 1 (abs (- (:row dot1) (:row dot2))))
            (= (:column dot1) (:column dot2))))
    true false)))

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

(defn chain-line [props owner]
  (reify
    om/IRender
    (render [_]
      (let [top nil
            left nil
            width nil
            height nil
            color nil
            orientation nil]
        (d/div #js {:style {:top top :left left :width width :height height}
                    :className (join " " ["line" color orientation])})))))

(defn dot
  "Component for an individual dot."
  [props owner]
  (reify
    om/IInitState
    (init-state [_]
      (let [left (left-pos (:column props))]
        (merge (select-keys props [:color :column :row])
               {:top -112 :left left})))

    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      (let [left (left-pos (:column next-props))]
        (om/set-state! owner
                       (merge (select-keys next-props [:color :column :row])
                              {:top -112 :left left}))))

    om/IRenderState
    (render-state [_ state]
      (let [color (:color state)
            col (:column state)
            row (:row state)
            className (str "dot levelish " (name color) " level-" row)
            left (str (:left state) "px")
            top (str (:top state) "px")]
        (d/div #js {:className className
                    :onMouseDown (fn [_] (log<- "Mouse down!"))
                    :onMouseOver (fn [_] (log<- (str "Mouse over " state)))
                    :onTouchStart (fn [_] (log<- "Touch start!"))
                    :style #js {:top top :left left}})))))

(defn dot-trace
  "Reads from `channel', accumulating a vector of dot states in order to
  create a chain-line between the dots."
  [owner channel]
  (go-loop [dots [(om/get-state owner)]]
    (let [next-dot (<! channel)
          start (if (> (count dots) 1) (first dots))
          end (if start (last dots))]
      (if (adjacent? (last dots) next-dot)
        (do
          (om/update-state! owner :chain {:start start :end end})
          (recur (merge dots next-dot)))
        (recur dots)))))

(defn board-area [props owner]
  (reify
    om/IInitState
    (init-state [_] {})

    om/IWillMount
    (will-mount [_]
      (let [click-pub (om/get-shared owner :click-pub-chan)
            click-sub-chan (chan)]
        (for [e [:mouse-down :mouse-up :mouse-over]]
          (async/sub click-pub e click-sub-chan))))

    om/IRender
    (render [this]
      (let [board-size (get-in props [:ui :board-size])
            dots (for [col (range board-size) row (range board-size)]
                   {:column col :row row
                    :color (first (take 1 (rand-colors nil)))})
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

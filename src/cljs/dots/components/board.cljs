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

(defn orient
  "Check for the adjacency of two dots.

  If the dots are adjacent, return `:horizontal' or `:vertical' to
  indicate orientation.

  Otherwise, return `false'."
  ([dot1 dot2] (let [abs #(. js/Math abs %)]
                 (cond
                  ;; if colors aren't equivalent, then for my purposes here, the
                  ;; dots are not adjacent.
                  (not= (:color dot1) (:color dot2))
                  false

                  ;; horizontal adjacency: same row, different columns
                  (and (= (:row dot1) (:row dot2))
                       (= 1 (abs (- (:column dot1) (:column dot2)))))
                  :horizontal

                  ;; vertical adjacency: same column, different rows
                  (and (= 1 (abs (- (:row dot1) (:row dot2))))
                       (= (:column dot1) (:column dot2)))
                  :vertical)))

  ([dot1 dot2 orientation]
     (cond
      (false? orientation) (orient dot1 dot2)
      (= orientation (orient dot1 dot2)) orientation
      :else false)))

(defn dot-trace
  "Reads from `channel', accumulating a vector of dot states in order to
  create a chain-line between the dots."
  [channel owner]
  ;; The loop params are hopefully self-explanatory but just in case:
  ;;   - `-orientation': This gives us the capacity to know which way we were
  ;;     creating a chain line last go 'round, so that we can make decisions
  ;;     about "adjacency". Please see the comments/docstring for `orient'.

  ;;  - `dragging?': This gives us some capacity to determine whether or not the
  ;;    user is in the process of attempting to build a chain line. The value
  ;;    starts as `false', then when a mouse-down event is detected, this is
  ;;    changed to `true'. When a mouse-up event is detected, this is changed
  ;;    back to `false'.
  (go-loop [dots [] -orientation false dragging? false]
    (let [{next-dot :dot-state event-type :topic} (<! channel)
          start (if (> (count dots) 1) (first dots) next-dot)
          end (if (> (count dots) 1) (last dots) next-dot)
          orientation (orient (last dots) next-dot -orientation)
          [next-val chain-val]
          (cond
           ;; If the last dot in `dots' and `next-dot' are adjacent, conj `dots'
           ;; and `next-dot', and start building the chain state.
           orientation [(merge dots next-dot)
                        {:color (:color (last dots))
                         :orientation orientation
                         :length (inc (count dots))
                         :start start :end end}]

           ;; Otherwise, just start over.
           :else [[next-dot] {}])]
      (om/set-state! owner :chain chain-val)
      (recur next-val orientation
             (cond dragging? (if (= :mouse-up event-type) false true)
                   :else (= :mouse-down event-type) true false)))))

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
            top (str (:top state) "px")
            handler (fn [event value state]
                      (go (>! click-chan {:topic value :dot-state state})))]
        (d/div #js {:className className
                    :onMouseDown #(handler % :mouse-down state)
                    :onMouseOver #(handler % :mouse-over state)
                    :onMouseUp #(handler % :mouse-up state)
                    :style #js {:top top :left left}})))))

(defn chain-line [props owner]
  (reify
    om/IRender
    (render [_]
      ;; TODO: Flesh out how the dimensions of this thing are fleshed out.
      (let [top nil
            left nil
            width nil
            height nil
            color nil
            orientation nil]
        (d/div #js {:style {:top top :left left :width width :height height}
                    :className (join " " ["line" color orientation])})))))

(defn chain-view [props owner]
  (reify
    om/IInitState
    (init-state [_] {:chain nil})

    om/IWillMount
    (will-mount [_]
      (let [click-pub-chan (om/get-shared owner :click-pub-chan)
            click-sub-chan (chan)]
        (doseq [e [:mouse-down :mouse-up :mouse-over]]
          (async/sub click-pub-chan e click-sub-chan))
        (dot-trace click-sub-chan owner)))

    om/IRenderState
    (render-state [_ state]
      (let [chain (if (:chain state)
                    (om/build chain-line (:chain state)))]
        (d/div #js {:className "chain-line"} chain)))))

(defn board-area [props owner]
  (reify
    om/IRenderState
    (render-state [this state]
      (let [board-size (get-in props [:ui :board-size])
            dots (for [col (range board-size) row (range board-size)]
                   {:column col :row row
                    :color (first (take 1 (rand-colors nil)))})
            grid (om/build-all dot dots)]
        (d/div
         #js {:className "board-area"}
         (om/build chain-view nil)
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

       ;; TODO: Start the timer via channels to ditch the 'hacky' feel.
       (if (= (get-in props [:style :display]) "inline")
         (om/build header {:ui (get props :ui)
                           :game-state (get props :game-state)}
                   {:init-state
                    {:time (get-in props [:game-state :time])
                     :score (get-in props [:game-state :score])}}))
       (om/build board-area props)))))

(ns dots.components.board
  (:require [cljs.core.async :as async :refer [>! <! chan]]
            [clojure.string :refer [join]]
            [dots.appstate :refer [app-state]]
            [dots.chans :refer [timer-chan click-chan remove-chan
                                transition-chan]]
            [dots.components.screen :refer [rand-colors]]
            [dots.utils :refer [log<- key-or-int]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(def css-transition-group js/React.addons.CSSTransitionGroup)

(defn game-state-cur []
  (om/ref-cursor (:game-state (om/root-cursor app-state))))

(defn columns-state-cur []
  (om/ref-cursor (get-in (om/root-cursor app-state) [:game-state :columns])))

(defn- left-pos [col]
  (+ 23 (* 45 col)))

(defn orient
  "Check for the adjacency of two dots.

  If the dots are adjacent, return `:horizontal' or `:vertical' to
  indicate orientation.

  Otherwise, return `false'."
  ([dot1 dot2] (let [abs #(. js/Math abs %)]
                 (cond
                  (nil? dot1)
                  false

                  ;; if colors aren't equivalent, then for my purposes here, the
                  ;; dots are not adjacent.
                  (not= (:color dot1) (:color dot2))
                  false

                  ;; vertical adjacency: same column, different rows
                  (and (= 1 (abs (- (:row dot1) (:row dot2))))
                       (= (:column dot1) (:column dot2)))
                  :vertical

                  ;; horizontal adjacency: same row, different columns
                  (and (= (:row dot1) (:row dot2))
                       (= 1 (abs (- (:column dot1) (:column dot2)))))
                  :horizontal

                  :default false)))

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
          drag-done? (and dragging? (= :mouse-up event-type))
          valid-chain? (and drag-done? (> (count dots) 1))
          next-val (cond
                    ;; If the last dot in `dots' and `next-dot' are
                    ;; adjacent, conj `dots' and `next-dot', and
                    ;; start building the chain state.
                    orientation (merge dots next-dot)
                    valid-chain? []
                    ;; Otherwise, just start over.
                    :else [next-dot])]
      (if valid-chain?
        (let [owner-col (om/get-state owner :column)]
          (doseq [dot dots :when (= (:column dot) (om/get-state owner :column))]
            (let [props (select-keys dot [:column :row])]
              (>! remove-chan {:topic props})
              (>! transition-chan
                  {:topic :transition
                   :dot-row (:row dot)
                   :event event-type
                   :dot-column owner-col})))))
      (recur next-val
             orientation
             (if (or (= :mouse-down event-type)
                     (and dragging? (not= :mouse-up event-type)))
               true false)))))

(defn remove-dot [channel owner]
  (go-loop []
    (let [val (<! channel)]
      (om/set-state! owner :row -1))
    (recur)))

(defn dot-transition [channel owner]
  (go-loop []
    (let [val (<! channel)]
      (om/update-state! owner :row inc))
    (recur)))

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

(defn- make-className [props]
  (let [color (:color props)
        row (:row props)]
    (str "dot levelish " (name color) " level-" row)))

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
    ;; om/IInitState
    ;; (init-state [_] {:chain nil})

    ;; om/IWillMount
    ;; (will-mount [_]
    ;;   (let [click-pub-chan (om/get-shared owner :click-pub-chan)
    ;;         click-sub-chan (chan)]
    ;;     (doseq [e [:mouse-down :mouse-up :mouse-over]]
    ;;       (async/sub click-pub-chan e click-sub-chan))
    ;;     (dot-trace click-sub-chan owner)))

    om/IRenderState
    (render-state [_ state]
      (let [chain (if (:chain state)
                    (om/build chain-line (:chain state)))]
        (d/div #js {:className "chain-line"} chain)))))

(defn dot
  "Component for an individual dot."
  [props owner]
  (reify
    om/IInitState
    (init-state [_]
      (let [left (left-pos (:column props))]
        (merge (select-keys props [:color :column :row])
               {:top -112 :left left})))

    om/IWillMount
    (will-mount [_]
      (let [remove-pub-chan (om/get-shared owner :remove-pub-chan)
            remove-sub-chan (chan)]
        ;; Each dot subscribes to the remove-pub-chan, listening for a topic
        ;; that matches its own column & row coordinates. This way it can be
        ;; removed from the game area on completion of a successful chain.
        (async/sub remove-pub-chan {:column (om/get-state owner :column)
                                    :row (om/get-state owner :row)}
                   remove-sub-chan)
        (remove-dot remove-sub-chan owner)))

    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      (let [left (left-pos (:column next-props))]
        (om/set-state! owner
                       (merge (select-keys next-props [:color :column :row])
                              {:top -112 :left left}))))

    om/IWillUpdate
    (will-update [_ next-props next-state]
      (let [current-row (om/get-state owner :row)]
        ;; Compare the current row to the potential next row. If they aren't
        ;; equal, that means this dot will be descending down the grid,
        ;; vertically (which means the value of :row will be increasing).
        ;; Therefore, put a message on `remove-chan' destined for
        ;; the dot that is above this one.
        (if (not= current-row (:row next-state))
          (go (>! transition-chan {:topic {:column (om/get-state owner :column)
                                           :row (dec current-row)}})))))

    om/IRenderState
    (render-state [_ state]
      (let [color (:color state)
            col (:column state)
            row (:row state)
            class-name (make-className state)
            left (str (:left state) "px")
            top (str (:top state) "px")
            handler (fn [event value state]
                      (go (>! click-chan {:topic value :dot-state state})))]
        (d/div #js {:className class-name
                    :onMouseDown #(handler % :mouse-down state)
                    :onMouseOver #(handler % :mouse-over state)
                    :onMouseUp #(handler % :mouse-up state)
                    :style #js {:top top :left left}})))))

(defn dot-column [props owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [click-pub-chan (om/get-shared owner :click-pub-chan)
            click-sub-chan (chan)]
        ;; We only want the column component to subscribe to events from its
        ;; own child dots. In other words, the dot-column component for column
        ;; 4 should only subscribe to events from dots in column 4.
        (async/sub click-pub-chan :mouse-down click-sub-chan)
        (async/sub click-pub-chan :mouse-over click-sub-chan)
        (async/sub click-pub-chan :mouse-up click-sub-chan)
        (dot-trace click-sub-chan owner)))

    om/IRenderState
    (render-state [this state]
      (let [get-rows-map (fn []
                           (om/ref-cursor
                            (get-in (columns-state-cur)
                                    [(key-or-int (:column props) "col-")
                                     :rows-map])))
            rows-map (om/observe owner (get-rows-map))
            col (:column props)
            dots (om/build-all dot (for [[row color] rows-map]
                                     (let [r (key-or-int row)]
                                       {:column col :row r :color color})))]
        (apply d/span #js {:className (str "col-" col)} dots)))))

(defn foo [channel owner]
  (go-loop []
    (let [msg (<! channel)
          dot-row (:dot-row msg)
          dot-col (:dot-column msg)]
      (log<- msg))
    (recur)))

(defn board-area [props owner]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [trans-pub-chan (om/get-shared owner :trans-pub-chan)
            trans-sub-chan (chan)]
        (async/sub trans-pub-chan :transition trans-sub-chan)
        (foo trans-sub-chan owner)))

    om/IRenderState
    (render-state [this state]
      (let [board-size (get-in props [:ui :board-size])
            columns (om/observe owner (columns-state-cur))
            grid (om/build-all dot-column (vals columns))]
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

(ns dots.chans
  (:require [cljs.core.async :as async :refer [<! >! chan close! sliding-buffer
                                               put! alts! timeout]])
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]]))

(defn select-chan [pred chans]
  (go (loop []
        (let [[value ch] (alts! chans)]
          (if (pred value) value (recur))))))

(defn mouseevent-chan [rc selector event msg-name]
  (bind ($ selector) event
        #(do
           (put! rc [msg-name {:x (.-pageX %) :y (.-pageY %)}]))))

(defn touchevent-chan [rc selector event msg-name]
  (bind ($ selector) event
        #(let [touch (aget (.-touches (.-originalEvent %)) 0)]
           (put! rc [msg-name {:x (.-pageX touch) :y (.-pageY touch)}]))))

(defn drawstart-chan [ichan selector]
  (mouseevent-chan ichan selector "mousedown" :drawstart)
  (touchevent-chan ichan selector "touchstart" :drawstart))

(defn drawend-chan [ichan selector]
  (mouseevent-chan ichan selector "mouseup" :drawend)
  (mouseevent-chan ichan selector "touchend" :drawend))

(defn drawer-chan [ichan selector]
  (mouseevent-chan ichan selector "mousemove" :draw)
  (touchevent-chan ichan selector "touchmove" :draw))

(defn get-drawing [input-chan out-chan]
  (go (loop [msg (<! input-chan)]
        (put! out-chan msg)
        (if (= (first msg) :draw)
          (recur (<! input-chan))))))

(defn draw-chan [selector]
  (let [input-chan (chan)
        out-chan   (chan)]
    (drawstart-chan input-chan selector)
    (drawend-chan   input-chan selector)
    (drawer-chan    input-chan selector)
    (go (loop [[msg-name _ :as msg] (<! input-chan)]
          (when (= msg-name :drawstart)
            (put! out-chan msg)
            (<! (get-drawing input-chan out-chan)))
          (recur (<! input-chan))))
    out-chan))

(def timer-chan (chan))

(def click-chan (chan))

(def remove-chan (chan))

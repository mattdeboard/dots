(ns dots.components.screen
  (:require [dots.utils :refer [log<-]]
            [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true]))


;; Useful functions
(def dot-colors [:blue :green :yellow :purple :red])

(def number-colors (count dot-colors))

(defn rand-colors [exclude-color]
  (let [colors (if exclude-color
                 (vec (remove (partial = exclude-color) dot-colors))
                 dot-colors)
        number-colors (if exclude-color (dec number-colors) number-colors)]
    (rand-nth colors)
    (map #(get colors (rand-int %))
         (repeat number-colors))))

(defn colorize-word [word]
  (map (fn [x c]
         ;; Exclude spaces from colorization
         {:color (if (not= " " x) (name c) :blank) :letter x})
       word (rand-colors nil)))

;; Components
(defn color-letter [props owner]
  (reify
    om/IRender
    (render [_]
      (let [color (:color props)
            l (:letter props)]
        (d/span #js {:className (name color)} l)))))

(defn marquee [props owner]
  (reify
    om/IRender
    (render [_]
      (let [game-state (:game-state props)
            word (if (:game-complete? game-state)
                   (str "SCORE " (:score game-state))
                   "SCORE")
            cword (om/build-all color-letter (colorize-word word))]
        (log<- game-state)
        (apply d/div #js {:className "marq"} cword)))))

(defn control-area
  "Component for the portion of the DOM containing interactive stuff
  (buttons etc.)."
  [props owner]
  (reify
    om/IRender
    (render [_]
      (d/div
       #js {:className "control-area"}
       (d/a
        #js {:className "start-new-game"
             :onClick (:click-handler (om/get-props owner))
             :href "#"}
        "new game")))))

(defn score-screen [props owner]
  (reify
    om/IDisplayName
    (display-name [_] "score-screen")

    om/IWillReceiveProps
    (will-receive-props [_ next-props]
      (om/set-state! owner :score (:score next-props)))

    om/IRender
    (render [_]
      (d/div
       #js {:className "dots-game" :id "score-screen"
            :style (clj->js (:style props))}
       (d/div
        #js {:className "notice-square"}
        (om/build marquee {:game-state (get props :game-state)})
        (om/build control-area (om/get-props owner)))))))

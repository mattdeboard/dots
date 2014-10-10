(ns dots.components.screen
  (:require [om.core :as om :include-macros true]
            [om.dom :as d :include-macros true]))


(def dot-colors [:blue :green :yellow :purple :red])

(def number-colors (count dot-colors))

(defn rand-colors [exclude-color]
  (. js/console log "getting colors" (prn-str exclude-color))
  (let [colors (if exclude-color
                 (vec (remove (partial = exclude-color) dot-colors))
                 dot-colors)
        number-colors (if exclude-color (dec number-colors) number-colors)]
    (. js/console log "getting colors" (prn-str colors))
    (map #(get colors (rand-int %))
         (repeat number-colors))))

(defn colorize-word [word]
  (map (fn [x c] [:span {:class (name c)} x]) word (rand-colors)))

(defn score-screen [props owner]
  (reify
    om/IInitState
    (init-state [_] {:score nil})

    om/IWillReceiveProps
    (will-receive-props [this next-props next-state]
      (om/set-state! this :score (:score next-props)))

    om/IRender
    (render [_]
      (let [score-text (colorize-word "SCORE")
            marquee (if (:score props)
                      (concat score-text
                              " "
                              (colorize-word (str (:score props))))
                      score-text)]
        (d/div
         #js {:className "dots-game"}
         (d/div
          #js {:className "notice-square"}
          (d/div
           #js {:className "marq"} marquee)
          (d/div
           #js {:className "control-area"}
           (d/a
            #js {:className "start-new-game" :href "#"}
            "new game"))))))))

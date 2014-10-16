[1mdiff --git a/src/cljs/dots/components/board.cljs b/src/cljs/dots/components/board.cljs[m
[1mindex 3e36e6c..fda20ab 100644[m
[1m--- a/src/cljs/dots/components/board.cljs[m
[1m+++ b/src/cljs/dots/components/board.cljs[m
[36m@@ -40,8 +40,7 @@[m
       (if (= (:time prev-state) 0)[m
         (let [timer-id (om/get-state owner :timer-id)][m
           (js/clearInterval timer-id)[m
[31m-          (go (>! timer-chan {:topic :game-complete})))[m
[31m-        (go (>! timer-chan {:topic :test-only}))))[m
[32m+[m[32m          (go (>! timer-chan {:topic :game-complete})))))[m
 [m
     om/IRender[m
     (render [_][m
[1mdiff --git a/src/cljs/dots/components/main.cljs b/src/cljs/dots/components/main.cljs[m
[1mindex f04dbd0..e087904 100644[m
[1m--- a/src/cljs/dots/components/main.cljs[m
[1m+++ b/src/cljs/dots/components/main.cljs[m
[36m@@ -1,6 +1,6 @@[m
 (ns dots.components.main[m
   (:require [cljs.core.async :as async :refer [chan <!]][m
[31m-            ; [dots.chans :refer [timer-chan]][m
[32m+[m[32m            [dots.chans :refer [timer-chan]][m
             [dots.components.board :refer [game-board]][m
             [dots.components.screen :refer [score-screen]][m
             [dots.utils :refer [log<-]][m
[36m@@ -9,23 +9,21 @@[m
   (:require-macros [cljs.core.async.macros :refer [go go-loop]]))[m
 [m
 (defn handle-click [e owner cursor][m
[31m-  (let [active (if (= (om/get-state owner :active-view) "score-screen")[m
[31m-                 "game-board" "score-screen")][m
[31m-    (om/transact![m
[31m-     cursor :ui[m
[31m-     ;; We need to update the value of `(get-in cursor [:ui :active-view])',[m
[31m-     ;; and ths is the only way I know of to do that. Inside the transaction[m
[31m-     ;; handler, we get the current view, switch to the opposite view, then[m
[31m-     ;; return a hashmap that updates `:active-view'.[m
[31m-     (fn [m] (let [current-view (:active-view m)[m
[31m-                   next-view (if (= current-view "score-screen")[m
[31m-                               "game-board" "score-screen")][m
[31m-               (merge m {:active-view next-view}))))[m
[31m-    ;; We need to reset game state back to the proper values when clicking on[m
[31m-    ;; the "new game" button at the end of the game.[m
[31m-    (om/transact![m
[31m-     cursor :game-state[m
[31m-     (fn [m] (merge m {:score 0 :game-complete? false})))))[m
[32m+[m[32m  (go (>! timer-chan {:topic :game-complete})))[m
[32m+[m[32m  ;; (om/transact![m
[32m+[m[32m  ;;  cursor :ui[m
[32m+[m[32m  ;;  ;; We need to update the value of `(get-in cursor [:ui :active-view])',[m
[32m+[m[32m  ;;  ;; and ths is the only way I know of to do that. Inside the transaction[m
[32m+[m[32m  ;;  ;; handler, we get the current view, switch to the opposite view, then[m
[32m+[m[32m  ;;  ;; return a hashmap that updates `:active-view'.[m
[32m+[m[32m  ;;  (fn [m][m
[32m+[m[32m  ;;    (let [next-view (switch-screen owner)][m
[32m+[m[32m  ;;      (merge m {:active-view next-view}))))[m
[32m+[m[32m  ;; ;; We need to reset game state back to the proper values when clicking on[m
[32m+[m[32m  ;; ;; the "new game" button at the end of the game.[m
[32m+[m[32m  ;; (om/transact![m
[32m+[m[32m  ;;  cursor :game-state[m
[32m+[m[32m  ;;  (fn [m] (merge m {:score 0 :game-complete? false}))))[m
 [m
 (defn active?[m
   "Returns a boolean indicating whether or not the named component is[m
[36m@@ -37,14 +35,17 @@[m
   [name cursor][m
   (= name (get-in cursor [:ui :active-view])))[m
 [m
[32m+[m[32m(defn switch-screen [owner][m
[32m+[m[32m  (let [current-view (om/get-state owner :active-view)][m
[32m+[m[32m    (if (= current-view "game-board") "score-screen" "game-board")))[m
[32m+[m
 (defn switch-active-view [channel owner][m
   (go-loop [][m
[31m-    (let [current-view (om/get-state owner :active-view)[m
[31m-          next-view (if (= current-view "game-board")[m
[31m-                      "score-screen" "game-board")[m
[32m+[m[32m    (let [next-view (switch-screen owner)[m
           topic (<! channel)][m
[31m-      (log<- {:current current-view :next next-view})[m
[31m-      (om/set-state! owner {:active-view next-view}))[m
[32m+[m[32m      (om/set-state! owner :active-view next-view)[m
[32m+[m[32m      (log<- (om/get-state owner)))[m
[32m+[m
     (recur)))[m
 [m
 (defn game-container [props owner][m
[36m@@ -61,7 +62,7 @@[m
     (will-receive-props [this next-props][m
       (let [view (get-in next-props [:ui :active-view])][m
         (if (not= view (om/get-state owner :active-view))[m
[31m-          (om/set-state! owner {:active-view view}))))[m
[32m+[m[32m          (om/set-state! owner :active-view view))))[m
 [m
     om/IRender[m
     (render [_][m

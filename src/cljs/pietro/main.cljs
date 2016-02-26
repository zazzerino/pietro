(ns pietro.main)

(def app (js/require "app"))
(def BrowserWindow (js/require "browser-window"))

(goog-define dev? true)

(defn load-page
  [window]
  (if dev?
    (.loadUrl window (str "file://" js/__dirname "/../../index.html"))
    (.loadUrl window (str "file://" js/__dirname "/index.html"))))

(def main-window (atom nil))

(defn make-window
  [w h frame? show?]
  (BrowserWindow. #js {:width w :height h :frame frame? :show show?}))

(defn init-browser []
  (reset! main-window (make-window 800 600 true true))
  (load-page @main-window)
  (if dev? (.openDevTools @main-window))
  (.on @main-window "closed" #(reset! main-window nil)))

(defn init []
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" init-browser)
  (set! *main-cli-fn* (fn [] nil)))

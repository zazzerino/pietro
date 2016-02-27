(ns pietro.gui
  (:import java.io.File
           javax.swing.SwingWorker)
  (:require [pietro.core :refer :all]
            [seesaw.core :as s]
            [seesaw.chooser :refer [choose-file file-filter]])
  (:gen-class))

(s/native!)

(def play-button (s/button :text "play"
                           :listen [:mouse-clicked (fn [_] (play))]))

(def pause-button (s/button :text "pause"
                            :listen [:mouse-clicked (fn [_] (pause))]))

(def stop-button (s/button :text "stop"
                           :listen [:mouse-clicked (fn [_] (stop))]))

(defn max-ticks
  [sequencer]
  (let [midi-seq (.getSequence sequencer)]
    (if midi-seq
      (.getTickLength midi-seq))))

(def midi-progress-bar
  (s/progress-bar :min 0
                  :max (let [ticks (max-ticks sequencer)]
                         (if-not (nil? ticks)
                           ticks
                           0))))


(defn choose-midi-file
  [label]
  (let [file (choose-file)]
    (set-midi-sequence (.getAbsolutePath file))
    (s/config! label :text (.getName file))
    (s/config! midi-progress-bar :max (max-ticks sequencer))))

(def midi-file-label (s/label " "))
(def choose-button (s/button :text "select midi file"
                             :listen [:mouse-clicked
                                      (fn [_]
                                        (choose-midi-file midi-file-label))]))

(def temperaments-listbox (s/listbox :model ["equal"
                                             "pythagorean"
                                             "third-comma"
                                             "quarter-comma"
                                             "fifth-comma"
                                             "sixth-comma"]))

(s/listen temperaments-listbox
          :selection (fn [e]
                       (let [temperament (s/selection e)]
                         (retune (condp = temperament
                                   "equal" equal-freqs
                                   "pythagorean" pythagorean-freqs
                                   "third-comma" third-comma-freqs
                                   "quarter-comma" quarter-comma-freqs
                                   "fifth-comma" fifth-comma-freqs
                                   "sixth-comma" sixth-comma-freqs)))))

(def midi-progress-timer
  (s/timer (fn [_]
             (s/config! midi-progress-bar :value (.getTickPosition sequencer)))))

(def midi-playback-buttons (s/horizontal-panel
                            :items [play-button pause-button stop-button
                                    midi-progress-bar]))

(def midi-file-panel (s/horizontal-panel :items [choose-button midi-file-label]))

(def instrument-spinner (s/spinner :model (s/spinner-model 1
                                                           :from 1
                                                           :to 128)))

(s/listen instrument-spinner :selection (fn [e]
                                          (let [instrument (s/selection e)]
                                            (change-instrument instrument))))

(def panel (s/border-panel :vgap 5 :hgap 5
                           :north midi-file-panel
                           :center (s/horizontal-panel
                                    :items [temperaments-listbox
                                            instrument-spinner])
                           :south midi-playback-buttons))

(def frame (s/frame :title "pietro"
                    :size [500 :by 300]
                    :content panel
                    :on-close :dispose))

(defn -main [& args]
  (-> frame s/pack! s/show!))

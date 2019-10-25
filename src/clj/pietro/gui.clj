(ns pietro.gui
  (:import java.io.File
           javax.swing.SwingWorker)
  (:require [pietro.tuning :refer :all]
            [pietro.midi :refer :all]
            [seesaw.core :as s]
            [seesaw.chooser :refer [choose-file file-filter]]
            [seesaw.font :refer :all]
            [seesaw.mig :refer :all])
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

(def instrument-label (s/label "instrument:"))
(def instrument-spinner (s/spinner :model (s/spinner-model 1
                                                           :from 1
                                                           :to 128)))

(s/listen instrument-spinner :selection (fn [e]
                                          (let [instrument (s/selection e)]
                                            (change-instrument instrument))))

(def bpm-label (s/label "bpm:"))
(def bpm-spinner (s/spinner :model (s/spinner-model 1
                                                    :from 1
                                                    :to 500)))
(s/listen bpm-spinner :selection (fn [e]
                                   (let [bpm (s/selection e)]
                                     (.setTempoInBPM sequencer bpm))))

(defn choose-midi-file
  [label instrument-spinner bpm-spinner]
  (let [file (choose-file)]
    (set-midi-sequence (.getAbsolutePath file))
    (s/config! label :text (.getName file))
    (s/config! midi-progress-bar :max (max-ticks sequencer))
    (s/config! instrument-spinner :model (s/spinner-model (get-instrument)
                                                          :from 1
                                                          :to 128))
    (s/config! bpm-spinner :model (s/spinner-model
                                   (int (.getTempoInBPM sequencer))
                                   :from 1 :to 500))))

(def midi-file-label (s/label :text "no file selected"
                              :h-text-position :center))

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
             (s/config! midi-progress-bar :value (.getTickPosition sequencer)))
           :delay 100))

;; (def midi-file-panel (s/horizontal-panel :items [choose-button midi-file-label]))
(def midi-playback-buttons (s/horizontal-panel
                            :items [play-button pause-button stop-button]))

(def spinners (s/grid-panel :columns 2
                            :items [instrument-label instrument-spinner
                                    bpm-label bpm-spinner]))

(def menubar
  (s/menubar :items
             [(s/menu :text "File"
                      :items
                      [(s/menu-item :text "Open"
                                    :listen
                                    [:action
                                     (fn [_]
                                       (choose-midi-file midi-file-label
                                                         instrument-spinner
                                                         bpm-spinner))])])]))

;; (def panel (s/border-panel
;;             :center (s/horizontal-panel
;;                      :items [temperaments-listbox
;;                              spinners
;;                              midi-file-label])
;;             :south midi-playback-buttons))

;; (def panel (s/border-panel ;:east temperaments-listbox
;;                            :south (s/vertical-panel
;;                                    :items [midi-file-label
;;                                            midi-progress-bar
;;                                            midi-playback-buttons])))

(def panel (mig-panel 
            :items [[instrument-label]
                    [instrument-spinner "wrap"]
                    [bpm-label] [bpm-spinner "wrap"]
                    [temperaments-listbox "wrap"]
                    [midi-file-label "wrap"]
                    [midi-progress-bar "span"]
                    [play-button "split 3"]
                    [pause-button]
                    [stop-button]]))
            
(def frame (s/frame :title "pietro"
                    ;; :size [500 :by 300]
                    :content panel
                    :menubar menubar
                    :on-close :dispose))

(defn show-gui! []
  (-> frame s/pack! s/show!))

(defn- reload []
  (s/config! frame :content panel))

(ns pietro.gui
  (:import java.io.File)
  (:require [pietro.core :refer :all]
            [seesaw.core :as s]
            [seesaw.chooser :refer [choose-file file-filter]])
  (:gen-class))

(s/native!)

(def play-button (s/button :text "play"
                           :listen [:mouse-clicked (fn [_] (start))]))

(def pause-button (s/button :text "pause"
                            :listen [:mouse-clicked (fn [_] (stop))]))

(defn choose-midi-file
  [label]
  (let [file (choose-file)]
    (set-midi-sequence (.getAbsolutePath file))
    (s/config! label :text (.getName file))))

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
                                   "equal" eq-freqs
                                   "pythagorean" py-freqs
                                   "third-comma" tcmt-freqs
                                   "quarter-comma" qcmt-freqs
                                   "fifth-comma" fcmt-freqs
                                   "sixth-comma" scmt-freqs)))))

(def midi-playback-buttons (s/horizontal-panel
                            :items [play-button pause-button]))

(def midi-file-panel (s/horizontal-panel :items [choose-button midi-file-label]))

(def panel (s/border-panel :vgap 5 :hgap 5
                           :north midi-file-panel
                           :center temperaments-listbox
                           :south midi-playback-buttons))

(def frame (s/frame :title "pietro"
                    :size [500 :by 300]
                    :content panel
                    :on-close :dispose))

(defn -main [& args]
  (-> frame s/pack! s/show!))

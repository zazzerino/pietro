(ns pietro.core
  (:import (javax.sound.midi MidiSystem Sequencer ShortMessage SysexMessage)
           (java.io File ByteArrayOutputStream))
  (:gen-class))

(def synthesizer (doto (MidiSystem/getSynthesizer) (.open)))
(def receiver (.getReceiver synthesizer))

(def sequencer (doto (MidiSystem/getSequencer false)
                 (.open)
                 (.setLoopCount Sequencer/LOOP_CONTINUOUSLY)))
(.setReceiver (.getTransmitter sequencer) receiver)

(def midi-sequence (atom nil))
(def sounding-notes (atom #{}))

(defn play-note
  ([midi-key velocity]
   (.noteOn (first (.getChannels synthesizer)) midi-key velocity)
   (swap! sounding-notes conj {:midi-key midi-key}))
  ([midi-key]
   (play-note midi-key 127)))

(defn stop-note
  [midi-key]
  (.noteOff (first (.getChannels synthesizer)) midi-key)
  (swap! sounding-notes disj {:midi-key midi-key}))

(defn silence
  []
  (doseq [note sounding-notes]
    (stop-note note)))

(defn cleanup []
  (.close sequencer)
  (.close synthesizer))

(defn set-midi-sequence
  [file-name]
  (reset! midi-sequence (MidiSystem/getSequence (File. file-name)))
  (.setSequence sequencer @midi-sequence))

(defn change-instrument
  [instrument]
  (dotimes [channel 16]
    (.programChange (nth (.getChannels synthesizer) channel) (dec instrument))))

;; tuning

(defn- log2 [n]
  (/ (Math/log n)
     (Math/log 2)))

(defn cents-between
  [freq-1 freq-2]
  (* (log2 (/ freq-2 freq-1)) 1200))

(defn- normalize-interval
  "Given a musical interval, adjusts the octave so that the ratio is between 1 
  and 2, inclusive."
  [ratio]
  (cond (< ratio 1) (recur (* ratio 2))
        (> ratio 2) (recur (/ ratio 2))
        :else ratio))

(defn syntonic-temperament
  [fifth]
  (let [sharps (take 7 (iterate (partial * fifth) 1))
        flats  (take 6 (iterate (partial * (/ 1 fifth)) 1))]
    (->> (concat sharps (rest flats))
         (map normalize-interval)
         sort)))

(defn meantone-fifth
  [fraction-of-syntonic-comma]
  (* 3/2 (/ 1 (Math/pow 81/80 fraction-of-syntonic-comma))))

(def pythagorean-ratios (syntonic-temperament 3/2))
(def third-comma-meantone-ratios (syntonic-temperament (meantone-fifth 1/3)))
(def quarter-comma-meantone-ratios (syntonic-temperament (meantone-fifth 1/4)))
(def fifth-comma-meantone-ratios (syntonic-temperament (meantone-fifth 1/5)))
(def sixth-comma-meantone-ratios (syntonic-temperament (meantone-fifth 1/6)))
(def equal-ratios (syntonic-temperament (Math/pow 2 7/12)))

(defn freq
  [root-freq half-steps ratios]
  (let [interval (nth ratios (mod half-steps 12))
        octave   (if (or (>= half-steps 0)
                         (zero? (rem half-steps 12)))
                   (quot half-steps 12)
                   (dec (quot half-steps 12)))]
    (* root-freq interval (Math/pow 2 octave))))

(defn standard-freq
  "Equal tempered frequency of midi-key with A4=440hz."
  [midi-key]
  (let [a4-key 69]
    (* 440 (Math/pow 2 (/ (- midi-key a4-key) 12)))))

(defn a4->d4
  "Takes a frequency for A4 and a seq of temperament ratios and returns the
  frequency of D4."
  [a4-freq ratios]
  (let [fifth (nth ratios 7)] ; 7 half-steps in a fifth
    (* a4-freq (/ 1 fifth))))

(defn midi-key->freq
  [midi-key a4-freq ratios]
  (let [d4 (a4->d4 a4-freq ratios)
        distance-from-d4 (- midi-key 62)]
    (freq d4 distance-from-d4 ratios)))

(defn midi-freqs
  [a4-freq ratios]
  (for [key (range 128)]
    (midi-key->freq key a4-freq ratios)))

(def standard-freqs (midi-freqs 440 equal-ratios))
(def eq-freqs (midi-freqs 440 equal-ratios))
(def py-freqs (midi-freqs 440 pythagorean-ratios))
(def tcmt-freqs (midi-freqs 440 third-comma-meantone-ratios))
(def qcmt-freqs (midi-freqs 440 quarter-comma-meantone-ratios))
(def fcmt-freqs (midi-freqs 440 fifth-comma-meantone-ratios))
(def scmt-freqs (midi-freqs 440 sixth-comma-meantone-ratios))
(def rand-freqs (for [key (range 128)]
                  (* (nth eq-freqs key) (rand))))

(defn send-tuning-change
  [channel]
  (doseq [msg [(ShortMessage. ShortMessage/CONTROL_CHANGE channel 0x64 04)
               (ShortMessage. ShortMessage/CONTROL_CHANGE channel 0x65 00)
               (ShortMessage. ShortMessage/CONTROL_CHANGE channel 0x06 00)
               (ShortMessage. ShortMessage/CONTROL_CHANGE channel 0x60 0x7f)
               (ShortMessage. ShortMessage/CONTROL_CHANGE channel 0x61 0x7f)
               (ShortMessage. ShortMessage/CONTROL_CHANGE channel 0x64 03)
               (ShortMessage. ShortMessage/CONTROL_CHANGE channel 0x65 00)
               (ShortMessage. ShortMessage/CONTROL_CHANGE channel 0x06 00)
               (ShortMessage. ShortMessage/CONTROL_CHANGE channel 0x60 0x7f)
               (ShortMessage. ShortMessage/CONTROL_CHANGE channel 0x61 0x7f)]]
    (.send receiver msg -1)))

(defn base-key
  [freq]
  (->> (map #(- freq %) standard-freqs)
       (take-while (comp not neg?))
       count
       dec))

(defn cents->byte-vec
  [cents]
  (let [tuning (int (/ (* cents 16384) 100))
        byte-1 (bit-and (bit-shift-right tuning 7) 0x7f)
        byte-2 (bit-and tuning 0x7f)]
    [byte-1 byte-2]))

(defn single-note-tuning-change-msg
  [freqs]
  (let [base-keys (map base-key freqs)
        tunings   (map #(cents-between (standard-freq (nth base-keys %))
                                       (nth freqs %))
                       (range 128))
        stream    (doto (ByteArrayOutputStream.)
                    (.write 0xf0)
                    (.write 0x7f)
                    (.write 0x7f)
                    (.write 0x08)
                    (.write 0x07)
                    (.write 0)
                    (.write 0)
                    (.write 128)
                    (#(dotimes [n 128]
                        (let [[byte-1 byte-2] (cents->byte-vec (nth tunings n))]
                          (.write % n)
                          (.write % (nth base-keys n))
                          (.write % byte-1)
                          (.write % byte-2))))
                    (.write 0xf7))
        data      (.toByteArray stream)]
    (SysexMessage. data (alength data))))

(defn retune
  [freqs]
  (dotimes [channel 16]
    (send-tuning-change channel))
  (.send receiver (single-note-tuning-change-msg freqs) -1))

(set-midi-sequence "/home/kdp/share/midi/wtc/bwv848.mid")

(defn start []
  (.start sequencer))

(defn stop []
  (.stop sequencer))

(defn -main
  [& args]
  (retune qcmt-freqs)
  (dotimes [n 20]
    (play-note (* 2 (+ n 30)) 127)
  (Thread/sleep 1000))
  (cleanup))

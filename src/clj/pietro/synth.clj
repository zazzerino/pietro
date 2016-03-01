(ns pietro.synth
  (:import [com.jsyn JSyn Synthesizer]
           [com.jsyn.unitgen LineOut SawtoothOscillatorBL
            SineOscillator]))

(def line-out (LineOut.))
(def synth (doto (JSyn/createSynthesizer)
             (.start)
             (.add line-out)))
(.start line-out)

(def running-ugens (atom #{}))

(defn play-freq [freq]
  (let [osc (SineOscillator. freq)]
    (swap! running-ugens conj {:freq freq :osc osc})
    (.add synth osc)
    (.connect (.output osc) 0 (.input line-out) 0)))

(defn stop-freq [freq]
  (let [osc (:osc (first (filter #(= (:freq %) freq) @running-ugens)))]
    (.disconnect (.input line-out) (.output osc))
    (.remove synth osc)
    (swap! running-ugens disj {:freq freq :osc osc})))

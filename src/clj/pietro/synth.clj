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

(defn play-freq
  ([osc-type freq ampl]
   (let [osc (case osc-type
               :sin (SineOscillator.)
               :saw (SawtoothOscillatorBL.))]
     (.set (.frequency osc) freq)
     (.set (.amplitude osc) ampl)
     (.add synth osc)
     (.connect (.output osc) 0 (.input line-out) 0)
     (.connect (.output osc) 0 (.input line-out) 1)
     (swap! running-ugens conj {:freq freq :osc-type osc-type :osc osc})))
  ([osc-type freq]
   (play-freq osc-type freq 0.2)))

(defn stop-freq [osc-type freq]
  (let [osc (:osc (first (filter #(and (= (:freq %) freq)
                                       (= (:osc-type %) osc-type))
                                 @running-ugens)))]
    (if osc
      (do (.disconnect (.output osc) 0 (.input line-out) 0)
          (.disconnect (.output osc) 0 (.input line-out) 1)
          (.remove synth osc)
          (swap! running-ugens disj {:freq freq :osc-type osc-type :osc osc})))))

(defn stop-all []
  (doseq [ugen @running-ugens]
    (stop-freq (:osc-type ugen) (:freq ugen))))

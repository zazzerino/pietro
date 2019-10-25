(ns pietro.tuning)

(defn log2 [n]
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
(def third-comma-ratios (syntonic-temperament (meantone-fifth 1/3)))
(def quarter-comma-ratios (syntonic-temperament (meantone-fifth 1/4)))
(def fifth-comma-ratios (syntonic-temperament (meantone-fifth 1/5)))
(def sixth-comma-ratios (syntonic-temperament (meantone-fifth 1/6)))
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
(def equal-freqs (midi-freqs 440 equal-ratios))
(def pythagorean-freqs (midi-freqs 440 pythagorean-ratios))
(def third-comma-freqs (midi-freqs 440 third-comma-ratios))
(def quarter-comma-freqs (midi-freqs 440 quarter-comma-ratios))
(def fifth-comma-freqs (midi-freqs 440 fifth-comma-ratios))
(def sixth-comma-freqs (midi-freqs 440 sixth-comma-ratios))
(def rand-freqs (for [key (range 128)]
                  (* (nth equal-freqs key) (rand))))


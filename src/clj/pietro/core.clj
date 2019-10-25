(ns pietro.core
  (:require [pietro.tuning :refer :all]
            [pietro.midi :refer :all]
            [pietro.synth :refer :all]
            [pietro.gui :refer :all])
  (:gen-class))

(defn -main [& args]
  (show-gui!))

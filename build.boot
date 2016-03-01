(set-env!
 :source-paths #{"src/clj"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.7.0"]
                 [seesaw "1.4.5"]
                 [com.jsyn/jsyn "16.7.5"]])
 
(task-options!
 pom {:project 'pietro
      :version "0.1.0"}
 aot {:all true}
 jar {:main 'pietro.gui
      :file "pietro.jar"}
 target {:dir #{"target"}})

(deftask build []
  (comp (aot) (pom) (uber) (jar) (target)))

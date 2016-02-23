(set-env!
 :resource-paths #{"src/"}
 :dependencies '[[org.clojure/clojure "1.7.0"]])

(task-options!
 pom {:project 'pietro
      :version "0.1.0"}
 aot {:all true}
 jar {:main 'pietro.core
      :file "pietro.jar"}
 target {:dir #{"target"}})

(deftask build []
  (comp (aot) (pom) (uber) (jar) (target)))

(set-env!
 :resource-paths #{"src/"}
 :dependencies '[[org.clojure/clojure "1.7.0"]])

(task-options!
 aot {:all true}
 pom {:project 'pietro
      :version "0.1.0"}
 jar {:main 'pietro.core})

(deftask build []
  (comp (aot) (pom) (uber) (jar)))

(set-env!
 :resource-paths #{"src/clj"}
 :dependencies '[[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.nrepl "0.2.11"]])

(task-options!
 pom {:project 'pietro
      :version "0.1.0"}
 aot {:all true}
 jar {:main 'pietro.core
      :file "pietro.jar"}
 target {:dir #{"target"}})

(deftask build []
  (comp (aot) (pom) (uber) (jar) (target)))

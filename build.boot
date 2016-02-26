(set-env!
 :source-paths #{"src/cljs"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [adzerk/boot-cljs "1.7.228-1"]])

(require '[adzerk.boot-cljs :refer [cljs]])

(task-options!
 pom {:project 'pietro
      :version "0.1.0"}
 aot {:all true}
 jar {:main 'pietro.core
      :file "pietro.jar"}
 target {:dir #{"target"}})

(deftask build-cljs []
  (cljs :ids #{"main"}
        :optimizations :simple))

(deftask build-jar []
  (comp (aot) (pom) (uber) (jar) (target)))

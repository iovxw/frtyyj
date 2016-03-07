(defproject frtyyj "0.1.0"
  :description ""
  :url ""
  :license {:name "The MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "2.0.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [net.iovxw/esab16 "0.1.1"]]
  :main ^:skip-aot frtyyj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

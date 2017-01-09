(defproject datomic-schema "1.3.1-SNAPSHOT"
  :description "Schema generator for Datomic that won't set your boots alight"
  :url "http://www.github.com/Yuppiechef/datomic-schema"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]]
  :plugins [[lein-shell "0.5.0"]] ;; to launch node for JavaScript tests

  :aliases
  {"build-js" ["with-profile" "clojurescript,datascript" "do" "cljsbuild" "once"]
   "test-js" ["do" "build-js," "shell" "node" "test_node.js"]}

  ;; lein with-profile can be used to select any (sub)set:
  ;;  $ lein with-profile +clojurescript,-datomic do repl
  :profiles
  {:dev [:datomic :datascript :clojurescript]
   :default [:datomic]
   :shared {:source-paths ["src"]}
   :datomic [:shared
             {:source-paths ["src-datomic"]
              :test-paths ["test-datomic"]
              :dependencies
              [[com.datomic/datomic-free "0.9.5153"]]}]
   :datascript [:shared
                {:source-paths ["src-datascript"]
                 :test-paths ["test-datascript"]
                 :dependencies
                 [[datascript "0.15.5"]]}]
   :clojurescript {:dependencies
                   [[org.clojure/clojurescript "1.7.228" :scope "provided"]]
                   :plugins [[lein-cljsbuild "1.1.4"]]
                   :cljsbuild {:builds [{:id "none"
                                         :source-paths ["src" "src-datascript" "test"]
                                         :compiler {:main "datomic-schema.datascript-test"
                                                    :output-to "target/datomic-schema.js"
                                                    :output-dir "target/none"
                                                    :optimizations :none}}]}}})

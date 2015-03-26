(defproject datomic-schema "1.3.0"
  :description "Schema generator for Datomic that won't set your boots alight"
  :url "http://www.github.com/Yuppiechef/datomic-schema"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.0"]]
  :profiles
  {:dev
   {:dependencies
    [[com.datomic/datomic-free "0.9.5153"]]}})

(ns datomic-schema.datascript-test
  (:require
   #?(:cljs [cljs.test    :as t :refer-macros [is are deftest testing]]
      :clj  [clojure.test :as t :refer        [is are deftest testing]])
   [datascript.core :as d]
   [datomic-schema.datascript :as ds]))

(def db-schema
  [(ds/schema
    "base"
    (ds/fields
     ["uuid" :uuid :unique-identity]
     ["type" :keyword]
     ["dateadded" :instant]))
   (ds/schema
    "asset"
    (ds/fields
     ["serial" :string]
     ["tag" :enum [:computer :furniture :expensive :cheap :consumable] :many]
     ["datepurchased" :instant]
     ["value" :long]
     ["deflationrate" :float]
     ["info" :uri]
     ["owner" :ref]
     ["group" :ref :many]
     ["active" :boolean]))
   (ds/schema
    "auth"
    (ds/fields
     ["login" :string]
     ["pwd" :bytes]))
   (ds/schema
    "person"
    (ds/fields
     ["name" :string]))
   (ds/schema
    "group"
    (ds/fields
     ["name" :string]))])

(defn attrs [db-schema]
  (->> db-schema
       (mapcat
        (fn [{:keys [namespace fields]}]
          (map (juxt (constantly namespace) first) fields)))
       (map (partial apply keyword))))


(defn uuid-ent [c ent]
  (d/entity (d/db c) [:base/uuid (:base/uuid ent)]))



(deftest schema-commit []
  (try
    (let [datomic-schema (ds/generate-schema db-schema)
          [ds-schema schema-inserts] (ds/datascript-schema datomic-schema)
          c (d/create-conn ds-schema)]
      ;; datascript schema is not queryable
      ;;(doseq [a (attrs db-schema)]
      ;;  (assert (not (nil? (d/entity (d/db c) a)))))
      @(d/transact c schema-inserts)

      (let [groups
            [{:db/id (d/tempid :db.part/user) :base/type :group :group/name "IT"}
             {:db/id (d/tempid :db.part/user) :base/type :group :group/name "Dev"}]
            owner
            {:db/id (d/tempid :db.part/user)
             :base/type :user
             :person/name "Bob"
             ;; too lazy to pull in a JavaScript implementation of sha
             ; :auth/pwd (sha "bobby2015") ;; use bcrypt for real please:
             ;; https://github.com/xsc/pandect or https://github.com/weavejester/crypto-password
             :auth/login "bob@example.com"}
            asset
            {:db/id (d/tempid :db.part/user)
             :base/type :asset
             :base/uuid (d/squuid)
             :base/dateadded #?(:clj (java.util.Date.) :cljs (js/Date.))
             :asset/serial "1234"
             ;; see https://github.com/tonsky/datascript/wiki/Tips-&-tricks#referencing-entities-via-ident-codes
             :asset/tag [[:db/ident :asset.tag/computer]
                         [:db/ident :asset.tag/expensive]]
             :asset/datepurchased #inst "2014-12-25"
             :asset/value 10200
             :asset/deflationrate 12.445
             :asset/info #?(:clj (java.net.URI. "http://www.github.com") :cljs "http://www.github.com")
             :asset/owner (:db/id owner)
             :asset/group (map :db/id groups)
             :asset/active true}]
        ;; Test all the different attribute types
        @(d/transact c (concat groups [owner asset]))
        (assert (= 4 (count (d/q '[:find ?e ?t :in $ [?types ...] :where [?e :base/type ?t] [?e :base/type ?types]] (d/db c) [:group :user :asset]))))

        (assert (= 10200 (:asset/value (uuid-ent c asset))))))))

(defn without-ids [s] (map #(dissoc % :db/id) s))

(deftest fields-behavior []
  (let [test-fields (ds/fields ["name" :string :indexed])]
    (is (= test-fields
           {:fields {"name" [:string #{:indexed}]}}))))

(deftest field-action []
  (let [common-opts {:db/noHistory false, :db/cardinality :db.cardinality/one
                     :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false}
        opts #(merge common-opts %)]
    (is (= (set (without-ids (ds/generate-schema [(ds/schema "user" (ds/fields [:name :string] [:irrelevant/address :string]
                                                                               ["id" :string] ["age" :long :alter!]))])))
           #{(opts {:db/valueType :db.type/string, :db/ident :user/name, :db.install/_attribute :db.part/db})
             (opts {:db/valueType :db.type/string, :db/ident :user/address, :db.install/_attribute :db.part/db})
             (opts {:db/valueType :db.type/string, :db/ident :user/id, :db.install/_attribute :db.part/db})
             (opts {:db/valueType :db.type/long, :db/ident :user/age, :db.alter/_attribute :db.part/db})}))))

(defn ^:export run-tests []
  #?(:cljs (do (enable-console-print!)
               (let [result-atom (atom)]
                 (defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
                   (reset! result-atom (cljs.test/successful? m)))
                 (t/run-tests)
                 (if @result-atom 0 1)))
     :clj (t/run-tests)))

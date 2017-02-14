(ns datomic-schema.schematest
  (:require
   [datomic.api :as d]
   [datomic-schema.schema :refer :all]
   [clojure.test :refer :all]))

(defdbfn dbinc [db e a qty] :db.part/user
  [[:db/add e a (+ qty (or (get (d/entity db e) a) 0))]])

(def db-schema
  [(dbfn
    dbdec [db e a qty] :db.part/user
    [[:db/add e a (- (or (get (d/entity db e) a) 0) qty)]])
   (schema
    base
    (fields
     [uuid :uuid :unique-identity]
     [type :keyword]
     [dateadded :instant]))
   (schema
    asset
    (fields
     [serial :string]
     [tag :enum [:computer :furniture :expensive :cheap :consumable] :many]
     [ns-tag :enum [:the/computer :very/expensive]]
     [datepurchased :instant]
     [value :long]
     [deflationrate :float]
     [info :uri]
     [owner :ref]
     [group :ref :many]
     [active :boolean]))
   (schema
    auth
    (fields
     [login :string]
     [pwd :bytes]))
   (schema
    person
    (fields
     [name :string]))
   (schema
    group
    (fields
     [name :string]))])

(defn attrs [db-schema]
  (->> db-schema
       (mapcat
        (fn [{:keys [namespace fields]}]
          (map (juxt (constantly namespace) first) fields)))
       (map (partial apply keyword))))

(def duri "datomic:mem://test")

(defn sha [x]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (doto digest
      (.reset)
      (.update (.getBytes x)))
    (.getBytes
     (apply str (map (partial format "%02x") (.digest digest))))))

(defn uuid-ent [c ent]
  (d/entity (d/db c) [:base/uuid (:base/uuid ent)]))



(deftest schema-commit []
  (d/create-database duri)
  (try
    (let [c (d/connect duri)]
      @(d/transact c (generate-schema (concat db-schema (dbfns->datomic dbinc))))
      (doseq [a (attrs db-schema)]
        (assert (not (nil? (d/entity (d/db c) a)))))

      (let [groups
            [{:db/id (d/tempid :db.part/user) :base/type :group :group/name "IT"}
             {:db/id (d/tempid :db.part/user) :base/type :group :group/name "Dev"}]
            owner
            {:db/id (d/tempid :db.part/user)
             :base/type :user
             :person/name "Bob"
             :auth/pwd (sha "bobby2015") ;; use bcrypt for real please:
             ;; https://github.com/xsc/pandect or https://github.com/weavejester/crypto-password
             :auth/login "bob@example.com"}
            asset
            {:db/id (d/tempid :db.part/user)
             :base/type :asset
             :base/uuid (d/squuid)
             :base/dateadded (java.util.Date.)
             :asset/serial "1234"
             :asset/tag [:asset.tag/computer :asset.tag/expensive]
             :asset/ns-tag [:the/computer :very/expensive]
             :asset/datepurchased #inst "2014-12-25"
             :asset/value 10200
             :asset/deflationrate 12.445
             :asset/info (java.net.URI. "http://www.github.com")
             :asset/owner (:db/id owner)
             :asset/group (map :db/id groups)
             :asset/active true}]
        ;; Test all the different attribute types
        @(d/transact c (concat groups [owner asset]))
        (assert (= 4 (count (d/q '[:find ?e ?t :in $ [?types ...] :where [?e :base/type ?t] [?e :base/type ?types]] (d/db c) [:group :user :asset]))))

        (assert (= 10200 (:asset/value (uuid-ent c asset))))

        ;; Try out a local database function
        (assert (= 11000 (last (last (dbinc (d/db c) [:base/uuid (:base/uuid asset)] :asset/value 800)))))

        ;; Then use it in a transaction
        @(d/transact c [[:dbinc [:base/uuid (:base/uuid asset)] :asset/value 512]])
        (assert (= 10712 (:asset/value (uuid-ent c asset))))

        ;; And use one that was specified inline in the db-schema
        @(d/transact c [[:dbdec [:base/uuid (:base/uuid asset)] :asset/value 206]])
        (assert (= 10506 (:asset/value (uuid-ent c asset))))))
    (finally
      (d/delete-database duri))))

(defdbfn with-assertion [db txs] :db.part/user
  (assert txs "first argument must be provided")
  txs)

(deftest database-functions []
  (d/create-database duri)
  (let [c (d/connect duri)]
    @(d/transact c (dbfns->datomic with-assertion))
    @(d/transact c [[:with-assertion [{:db/id (d/tempid :db.part/user)
                                       :db/ident :foo}]]])
    (assert (d/entid (d/db c) :foo) ":foo must be transacted")
    ))

(deftest field-action []
  (let [common-opts {:db/noHistory false, :db/cardinality :db.cardinality/one
                     :db/index false, :db/fulltext false, :db/doc "", :db/isComponent false}
        opts #(merge common-opts %)
        without-ids (fn [s] (map #(dissoc % :db/id) s))]
    (is (= (set (without-ids (generate-schema [(schema user (fields [:name :string] [:irrelevant/address :string]
                                                                    [id :string] [age :long :alter!]))])))
           #{(opts {:db/valueType :db.type/string, :db/ident :user/name, :db.install/_attribute :db.part/db})
             (opts {:db/valueType :db.type/string, :db/ident :user/address, :db.install/_attribute :db.part/db})
             (opts {:db/valueType :db.type/string, :db/ident :user/id, :db.install/_attribute :db.part/db})
             (opts {:db/valueType :db.type/long, :db/ident :user/age, :db.alter/_attribute :db.part/db})}))))

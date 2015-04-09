(ns example.core
  (:use
   [datomic-schema.schema :only [fields part schema]])
  (:require
   [datomic.api :as d]
   [datomic-schema.schema :as s])
  (:gen-class))
  
(defonce db-url "datomic:mem://testdb")

(defn dbparts []
  [(part "app")])

(defn dbschema []
  [(schema user
    (fields
     [username :string :indexed]
     [pwd :string "Hashed password string"]
     [email :string :indexed]
     [status :enum [:pending :active :inactive :cancelled]]
     [group :ref :many]))
   
   (schema group
    (fields
     [name :string]
     [permission :string :many]))])

(defn setup-db [url]
  (d/create-database url)
  (d/transact
   (d/connect url)
   (concat
    (s/generate-parts (dbparts))
    (s/generate-schema (dbschema)))))

(defn -main [& args]
  (setup-db db-url)
  (println "Attributes defined in db:"
           (map (comp :db/ident (partial d/entity (d/db (d/connect db-url))) first)
                (d/q '[:find ?e :where [_ :db.install/attribute ?e]] (d/db (d/connect db-url))))))



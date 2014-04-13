(ns example.core
  (:use
   [datomic-schema.schema :only [defpart defschema fields part schema]])
  (:require
   [datomic.api :as d]
   [datomic-schema.schema :as s])
  (:gen-class))
  
(defonce db-url "datomic:mem://testdb")

(defpart app)

(defschema user
  (fields
   [username :string :indexed]
   [pwd :string "Hashed password string"]
   [email :string :indexed]
   [status :enum [:pending :active :inactive :cancelled]]
   [group :ref :many]))

(defschema group
  (fields
   [name :string]
   [permission :string :many]))

(defn -main [& args]
  (d/create-database db-url)
  (d/transact
   (d/connect db-url)
   (concat
    (s/build-parts d/tempid [app])
    (s/build-schema d/tempid [user group]))))

;; Alternatively, you can skip out the defschema and manage the datastructures yourself using schema:

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
    (s/build-parts d/tempid (dbparts))
    (s/build-schema d/tempid (dbschema)))))

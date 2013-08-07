# datomic-schema

datomic-schema makes it easier to see your datomic schema without sacrificing any features of Datomic

A simple example :

```clojure
(defpart app)

(defschema user
  (part app)
  (fields
   [username :string :indexed]
   [pwd :string "Hashed password string"]
   [email :string :indexed]
   [status :enum [:pending :active :inactive :cancelled]]
   [group :ref :many]))

(defschema group
  (part app)
  (fields
   [name :string]
   [permission :string :many]))
```

This will define the attributes:

```clojure
:user/username, :db.type/string, indexed
:user/pwd, :db.type/string, :db/doc "Hashed password string"
:user/email, :db.type/string, indexed
:user/status, :db.type/ref
:user.status/pending - in :db.user space
:user.status/active - in :db.user space
:user.status/inactive - in :db.user space
:user.status/cancelled - in :db.user space
:user/group, :db.type/ref, :db.cardinality/many
:group/name, :db.type/string
:group/permission, :db.type/string, :db.cardinality/many
```

You get the idea..

## Usage

In leiningen, simply add this to your dependencies

```clojure
[datomic-schema "1.0.0"]
```

Or maven:
```xml
<dependency>
  <groupId>datomic-schema</groupId>
  <artifactId>datomic-schema</artifactId>
  <version>1.0.0</version>
</dependency>
```

A picture speaks a thousand words. I don't have a picture, but here's some code:

```clojure
(ns myapp
  (:use [datomic-schema.schema :only [defpart defschema fields part]])
  (:require [datomic.api :as d])
  (:require [datomic-schema.schema :as s])
  (:gen-class))
  
(defonce db-url "datomic:mem://testdb")

(defpart app)

(defschema user
  (part app)
  (fields
   [username :string :indexed]
   [pwd :string "Hashed password string"]
   [email :string :indexed]
   [status :enum [:pending :active :inactive :cancelled]]
   [group :ref :many]))

(defschema group
  (part app)
  (fields
   [name :string]
   [permission :string :many]))

(defn -main [& args]
  (d/create-database db-url)
  (d/transact (d/connect db-url) (s/build-parts d/tempid))
  (d/transact (d/connect db-url) (s/build-schema d/tempid)))
```

The crux of this is in the (s/build-parts) and (s/build-schema), which turns your defparts and defschemas into a nice long list of datomic schema transactions.

You can build specific schema's by calling (s/generate-schema user group) - which will return a list of schema transactions just for those specific schemas, if you prefer to be verbose about it.

Also notice that :enum resolves to a :ref type, the vector can be a list of strings: ["Pending" "Active" "Inactive" "cancelled"] or a list of keywords as shown. String will be converted to keywords by lowercasing and converting spaces to dashes, so "Bad User" will convert to :user.status/bad-user.

## Why pass in the d/tempid?

Because I really didn't want to create a dependency on anything else for this library. Not even datomic. Heck, I'm so pedantic about not wanting deps that I don't even depend on Clojure.

## Possible keys to put on a field:

Just a list of keys you'd be interested to use on fields - look at http://docs.datomic.com/schema.html for more detailed info

```clojure
;; Types
:keyword :string :boolean :long :bigint :float :double :bigdec :ref :instant :uuid :uri :bytes :enum

;; Options
:indexed :many :fulltext :component :nohistory "Some doc string" [:arbitrary "Enum" :values]
```

## License

Copyright © 2013 Yuppiechef Online (Pty) Ltd.

Distributed under the Eclipse Public License, the same as Clojure.

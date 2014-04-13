# datomic-schema

datomic-schema makes it easier to see your datomic schema without sacrificing any features of Datomic

## API Changes: v1.1.0

This is a bit of an API breaking version, as I have removed the stateful nature of the defschema and defpart - You are now required to add the defined vars to the build-parts and build-schema functions. This is for simplicity - you don't expect a 'def' to also maintain some global state.

If you prefer to use a 'global' state, see the second half of the example below - wrap your current defschema's into a function which returns a vector and rename all the (defschema) calls to just (schema)

## Example

A 2 second example :

```clojure
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

(concat
  (s/build-parts d/tempid [app])
  (s/build-schema d/tempid [user group])) 
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
[datomic-schema "1.1.0"]
```

Or maven:
```xml
<dependency>
  <groupId>datomic-schema</groupId>
  <artifactId>datomic-schema</artifactId>
  <version>1.1.0</version>
</dependency>
```

A picture speaks a thousand words. I don't have a picture, but here's some code:

```clojure
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
  [(schema
    "user"
    (fields
     [username :string :indexed]
     [pwd :string "Hashed password string"]
     [email :string :indexed]
     [status :enum [:pending :active :inactive :cancelled]]
     [group :ref :many]))
   
   (schema
    "group"
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
```

You can play around with the example project if you want to see this in action.

The crux of this is in the (s/build-parts) and (s/build-schema), which turns your defparts and defschemas into a nice long list of datomic schema transactions.

Also notice that :enum resolves to a :ref type, the vector can be a list of strings: ["Pending" "Active" "Inactive" "cancelled"] or a list of keywords as shown. String will be converted to keywords by lowercasing and converting spaces to dashes, so "Bad User" will convert to :user.status/bad-user.

Last, but not least, the schemas that you define attaches themselves to their named vars, as you'd expect a def to do (not true for the defpart, unfortunately), so you can open up the structure and look at it.

## Why pass in the d/tempid?

Because I really didn't want to create a dependency on anything else for this library. Not even datomic. Heck, I'm so pedantic about not wanting deps that I don't even depend on Clojure.

## Possible keys to put on a field:

Just a list of keys you'd be interested to use on fields - look at http://docs.datomic.com/schema.html for more detailed info

```clojure
;; Types
:keyword :string :boolean :long :bigint :float :double :bigdec :ref :instant :uuid :uri :bytes :enum

;; Options
:unique-value :unique-identity :indexed :many :fulltext :component :nohistory "Some doc string" [:arbitrary "Enum" :values]
```

## License

Copyright © 2013 Yuppiechef Online (Pty) Ltd.

Distributed under the Eclipse Public License, the same as Clojure.

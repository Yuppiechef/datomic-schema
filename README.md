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
  (:use [datomic.schema :only [defpart defschema fields part]])
  (:require [datomic.api :as d])
  (:require [datomic.schema :as s]))
  
(defonce db-url "datomic:mem://inhouse")

(defpart app)

(defschema user
  (part app)
  (fields
   [username :string :indexed]
   [pwd :string "Hashed password string"]
   [email :string :indexed]
   [group :ref :many]))

(defschema group
  (part app)
  (fields
   [name :string]
   [permission :string :many]))

(defn -main [& args]
  (d/transact (d/connect db-url) (s/build-parts d/tempid))
  (d/transact (d/connect db-url) (s/build-schema d/tempid)))
```

The crux of this is in the (s/build-parts) and (s/build-schema), which turns your defparts and defschemas into a nice long list of datomic schema transactions.

You can build specific schema's by calling (s/generate-schema user group) - which will return a list of schema transactions just for those specific schemas, if you prefer to be verbose about it.

## Why pass in the d/tempid?

Because I really didn't want to create a dependency on anything else for this library. Not even datomic. Heck, I'm so pedantic about not wanting deps that I don't even depend on Clojure.

## License

Copyright © 2013 Yuppiechef (Pty) Ltd.

Distributed under the Eclipse Public License, the same as Clojure.

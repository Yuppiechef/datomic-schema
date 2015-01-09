# datomic-schema

datomic-schema makes it easier to see your datomic schema without sacrificing any features of Datomic

[![Clojars Project](http://clojars.org/datomic-schema/latest-version.svg)](http://clojars.org/datomic-schema)

See the current [Changelog](https://github.com/Yuppiechef/datomic-schema/wiki/Changelog)

## Example

A 2 second example :

```clojure
(def parts [(part "app")])

(def schema
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

(concat
  (s/generate-parts d/tempid parts)
  (s/generate-schema d/tempid schema)) 
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
[datomic-schema "1.2.0"]
```

Or maven:
```xml
<dependency>
  <groupId>datomic-schema</groupId>
  <artifactId>datomic-schema</artifactId>
  <version>1.2.0</version>
</dependency>
```

A picture speaks a thousand words. I don't have a picture, but here's some code:

```clojure
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
    (s/generate-parts d/tempid (dbparts))
    (s/generate-schema d/tempid (dbschema)))))

(defn -main [& args]
  (setup-db db-url)
  (let [gid (d/tempid :db.part/user)]
    (d/transact
     db-url
     [{:db/id gid
       :group/name "Staff"
       :group/permission "Admin"}
      {:db/id (d/tempid :db.part/user)
       :user/username "bob"
       :user/email "bob@example.com"
       :user/group gid
       :user/status :user.status/pending}])))
```

You can play around with the example project if you want to see this in action.

The crux of this is in the (s/generate-parts) and (s/generate-schema), which turns your parts and schemas into a nice long list of datomic schema transactions.

Also notice that :enum resolves to a :ref type, the vector can be a list of strings: ["Pending" "Active" "Inactive" "cancelled"] or a list of keywords as shown. String will be converted to keywords by lowercasing and converting spaces to dashes, so "Bad User" will convert to :user.status/bad-user.

Lastly, the result of (s/schema) and (s/part) are simply just datastructures - you can build them up yourself, add your own metadata or store them off. Your call.

## Why pass in the d/tempid?

Because I really didn't want to create a dependency on anything else for this library. Not even datomic. Heck, I'm so pedantic about not wanting deps that I don't even depend on Clojure.

## Possible keys to put on a field:

Just a list of keys you'd be interested to use on fields - look at http://docs.datomic.com/schema.html for more detailed info

```clojure
;; Types
:keyword :string :boolean :long :bigint :float :double :bigdec :ref :instant
:uuid :uri :bytes :enum

;; Options
:unique-value :unique-identity :indexed :many :fulltext :component
:nohistory "Some doc string" [:arbitrary "Enum" :values]
```

## Datomic defaults:
Datomic has defaults for:

```
:db/index <false>
:db/fulltext <false>
:db/noHistory <false>
:db/component <false>
:db/doc <"">
```
The default behavior of `generate-schema` is to explicitly generate these defaults.

This behavior can be overridden by passing in `false` as the last argument:

`(s/generate-schema d/tempid schema false)`.

Passing in `false` will elide those Datomic default keys, unless of course your `schema`
defines non-default values.

Note, that Datomic requires that `:db/cardinality` be explicitly set for each attribute installed. `generate-schema` will default to `:db.cardinality/one` unless the `schema` passed in specifies otherwise.


## License

Copyright © 2013 Yuppiechef Online (Pty) Ltd.

Distributed under the Eclipse Public License, the same as Clojure.

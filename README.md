# datomic-schema

datomic-schema makes it easier to see your datomic schema without sacrificing any features of Datomic

[![Clojars Project](http://clojars.org/datomic-schema/latest-version.svg)](http://clojars.org/datomic-schema)

See the current [Changelog](https://github.com/Yuppiechef/datomic-schema/wiki/Changelog)

## 1.3.0 API Breaking change

It's subtle, but the `(generate-schema)` optionally takes an option map instead of a boolean for `gen-all?`

This is to arbitrarily support extra generating options, including the new `index-all?` option, which flags every attribute in the schema for indexing (in line with Stuart Halloway's recommendation that you simply turn indexing on for every attribute by default).

The `defschema` and `defpart` macro's have been removed along with their `build-parts` and `build-schema` counterparts. These do not lead to good code design and it is encouraged that you remove them from your code at any rate.

Lastly, the `field-to-datomic`, `schema-to-datomic` and `part-to-datomic` functions have all been renamed to `field->datomic`, `schema->datomic` and `part->datomic` respectively. This is really just an implementation detail, so it shouldn't have much impact.

## Example

A 2 second example :

```clojure
(require '[datomic-schema.schema :as s])

(def parts [(s/part "app")])

(def schema
  [(s/schema user
    (s/fields
     [username :string :indexed]
     [pwd :string "Hashed password string"]
     [email :string :indexed]
     [status :enum [:pending :active :inactive :cancelled]]
     [group :ref :many]))
   
   (s/schema group
    (s/fields
     [name :string]
     [permission :string :many]))])

(concat
  (s/generate-parts parts)
  (s/generate-schema schema)) 
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

Also, as of 1.3.0, you can define database functions either as `defdbfn`, which creates a namespaced var so that you can use it inside your current process, or using `dbfn` which emits a map that you can directly transact:

```
(defdbfn dbinc [db e a qty] :db.part/user
  [[:db/add e a (+ qty (or (get (d/entity db e) a) 0))]])

(def db-schema
  (concat
   [(dbfn
     dbdec [db e a qty] :db.part/user
     [[:db/add e a (- (or (get (d/entity db e) a) 0) qty)]])]
   (dbfns->datomic dbinc)))
```

See the [more exhaustive example](https://github.com/Yuppiechef/datomic-schema/blob/master/test/datomic_schema/schematest.clj)

You get the idea..

## Usage

In leiningen, simply add this to your dependencies

```clojure
[datomic-schema "1.3.0"]
```

Or maven:
```xml
<dependency>
  <groupId>datomic-schema</groupId>
  <artifactId>datomic-schema</artifactId>
  <version>1.3.0</version>
</dependency>
```

A picture speaks a thousand words. I don't have a picture, but here's some code:

```clojure
(defonce db-url "datomic:mem://testdb")

(defdbfn dbinc [db e a qty] :db.part/user
  [[:db/add e a (+ qty (or (get (d/entity db e) a) 0))]])

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
    (s/generate-schema (dbschema))
    (s/dbfns->datomic dbinc)))))

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

## Possible keys to put on a field:

Just a list of keys you'd be interested to use on fields - look at http://docs.datomic.com/schema.html for more detailed info

```clojure
;; Types
:keyword :string :boolean :long :bigint :float :double :bigdec :ref :instant
:uuid :uri :bytes :enum

;; Options
:unique-value :unique-identity :indexed :many :fulltext :component
:nohistory "Some doc string" [:arbitrary "Enum" :values]
:alter!
```

### Altering schema

If you need to update an option of an existing field - add an `:alter!` option
key. This way a `:db.alter/_attribute` will be generated instead of a default
`:db.install/_attribute`.

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

This behavior can be overridden by passing in `:gen-all?` as `false`:

```
(s/generate-schema schema {:gen-all? false})
```

Passing `:gen-all` as `false` will elide those Datomic default keys, unless of course your `schema`
defines non-default values.

Note, that Datomic requires that `:db/cardinality` be explicitly set for each attribute installed. `generate-schema` will default to `:db.cardinality/one` unless the `schema` passed in specifies otherwise.

## Indexing

By default, attributes have `:db/index false`. If you would like every attribute in your schema to have `:db/index true` then simply include `:index-all? true` in your `generate-schema` call:

```
(s/generate-schema schema {:index-all? true})
```

## License

Copyright © 2019 Yuppiechef Online (Pty) Ltd.

Distributed under The MIT License (MIT) - See LICENSE.txt

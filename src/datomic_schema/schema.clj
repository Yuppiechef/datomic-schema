(ns datomic-schema.schema
  "Datomic, vs DataScript, helpers"
  (require [datomic.api]))

(def ^{:private true} tempid-fn datomic.api/tempid)
(def constant-schema [])

(load "-shared")

; not putting these earlier in ns to ensure that references from common are errors
(require '[datomic.api :as d])
(require '[datomic.function :as df])

;; Datomic (not Datascript) definitions below here

(defmacro dbfn
  [name params partition & code]
  (let [code-in-do `(do ~@code)]
    `{:db/id (datomic.api/tempid ~partition)
      :db/ident ~(keyword name)
      :db/fn (df/construct
              {:lang "clojure"
               :params '~params
               :code '~code-in-do})}))

(defmacro defdbfn
  "Define a datomic database function. All calls to datomic api's should be namespaced with datomic.api/ and you cannot use your own namespaces (since the function runs inside datomic)

  This defines a locally namespaced function as well - which is useful for testing.

  Your first parameter needs to always be 'db'.

  You'll need to commit the actual function's meta into your datomic instance by calling (d/transact (meta myfn))"
  [name params partition & code]
  `(def ~name
     (with-meta
       (fn ~name [~@params]
         ~@code)
       {:tx (dbfn ~name ~params ~partition ~@code)})))

(defn dbfns->datomic [& dbfn]
  (map (comp :tx meta) dbfn))


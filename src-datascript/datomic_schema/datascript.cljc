(ns datomic-schema.datascript
  (:require [datascript.core :as d]
            [datomic-schema.shared :as ds]))

(defn schema*         [& args] (apply ds/schema* args))
(defn part            [& args] (apply ds/part args))
(def  get-enums       (partial ds/-get-enums d/tempid))
(def  generate-schema (partial ds/-generate-schema d/tempid))

(defn fields
  "Simply a helper for converting (fields [\"name\" :string :indexed]) into {:fields {\"name\" [:string #{:indexed}]}}"
  [& fielddefs]
  (let [defs (reduce (fn [a [nm tp & opts]] (assoc a (name nm) [tp (set opts)])) {} fielddefs)]
    {:fields defs}))

(defn schema [nm & maps]
  (apply schema* (str nm) maps))

(defn datascript-schema [generated-schema]
  (loop [generated-map {:db/ident {:db/unique :db.unique/identity}}
         insert-operations []
         pending-items generated-schema]
    (if (not (empty? pending-items))
      (let [next-item (first pending-items)
            remaining-items (rest pending-items)]
        (if (map? next-item)
          (let [ident (:db/ident next-item)
                next-item (dissoc next-item
                                   :db/ident :db/id :db.install/_attribute)]
            (recur (assoc generated-map
                          ident
                          (if (= (:db/valueType next-item) :db.type/ref)
                            next-item
                            (dissoc next-item :db/valueType)))
                   insert-operations
                   remaining-items))
          (recur generated-map
                 (conj insert-operations next-item)
                 remaining-items)))
      [generated-map insert-operations])))

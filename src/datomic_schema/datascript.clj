(ns datomic-schema.datascript
  (require [datascript.core :as d]))

(load "-shared")

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

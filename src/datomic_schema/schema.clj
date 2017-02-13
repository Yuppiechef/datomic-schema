(ns datomic-schema.schema
  (:require
   [datomic.api :as d]
   [datomic.function :as df]))

;; The main schema functions
(defmacro fields
  "Simply a helper for converting (fields [name :string :indexed]) into {:fields {\"name\" [:string #{:indexed}]}}"
  [& fielddefs]
  (let [defs (reduce (fn [a [nm tp & opts]] (assoc a (name nm) [tp (set opts)])) {} fielddefs)]
    `{:fields ~defs}))

(defn schema*
  "Simply merges several maps into a single schema definition and add one or two helper properties"
  [name maps]
  (apply merge
         {:name name :basetype (keyword name) :namespace name}
         maps))

(defmacro schema
  [nm & maps]
  `(schema* ~(name nm) [~@maps]))

(defn part
  [nm]
  (keyword "db.part" nm))

;; The datomic schema conversion functions
(defn get-enums [basens part enums]
  (map (fn [n]
         (let [nm (if (string? n) (.replaceAll (.toLowerCase ^String n) " " "-") (name n))]
           [:db/add (d/tempid part) :db/ident (keyword basens nm)])) enums))

(def unique-mapping
  {:db.unique/value :db.unique/value
   :db.unique/identity :db.unique/identity
   :unique-value :db.unique/value
   :unique-identity :db.unique/identity})

(defn field->datomic [basename part {:keys [gen-all? index-all?]} acc [fieldname [type opts]]]
  (let [uniq (first (remove nil? (map #(unique-mapping %) opts)))
        dbtype (keyword "db.type" (if (= type :enum) "ref" (name type)))
        result
        (cond->
            {(if (:alter! opts) :db.alter/_attribute :db.install/_attribute) :db.part/db
             :db/id (d/tempid :db.part/db)
             :db/ident (if basename (keyword basename fieldname)
                          (keyword fieldname))
             :db/valueType dbtype
             :db/cardinality (if (opts :many) :db.cardinality/many :db.cardinality/one)}
          (or index-all? gen-all? (opts :indexed))
          (assoc :db/index (boolean (or index-all? (opts :indexed))))

          (or gen-all? (seq (filter string? opts)))
          (assoc :db/doc (or (first (filter string? opts)) ""))

          (or gen-all? (opts :fulltext)) (assoc :db/fulltext (boolean (opts :fulltext)))
          (or gen-all? (opts :component)) (assoc :db/isComponent (boolean (opts :component)))
          (or gen-all? (opts :nohistory)) (assoc :db/noHistory (boolean (opts :nohistory))))]
    (concat
     acc
     [(if uniq (assoc result :db/unique uniq) result)]
     (if (= type :enum) (get-enums (if basename (str basename "." fieldname) fieldname) part (first (filter vector? opts)))))))

(defn schema->datomic [opts acc schema]
  (if (or (:db/id schema) (vector? schema))
    (conj acc schema) ;; This must be a raw schema definition
    (let [key (:namespace schema)
          part (or (:part schema) :db.part/user)]
      (reduce (partial field->datomic key part opts) acc (:fields schema)))))

(defn part->datomic [acc part]
  (conj acc
        {:db/id (d/tempid :db.part/db),
         :db/ident part
         :db.install/_partition :db.part/db}))

(defn generate-parts [partlist]
  (reduce (partial part->datomic) [] partlist))

(defn generate-schema
  ([schema] (generate-schema schema {:gen-all? true}))
  ([schema {:keys [gen-all? index-all?] :as opts}]
   (reduce (partial schema->datomic opts) [] schema)))

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

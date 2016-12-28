;;; NOT A STANDALONE NAMESPACE!
;;;
;;; this should be included into a namespace with tempid-fn already defined,
;;; and either datomic.api or datascript.core aliased as "d"

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
           [:db/add (tempid-fn part) :db/ident (keyword basens nm)])) enums))

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
             :db/id (tempid-fn :db.part/db)
             :db/ident (keyword basename fieldname)
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
        {:db/id (tempid-fn :db.part/db),
         :db/ident part
         :db.install/_partition :db.part/db}))

(defn generate-parts [partlist]
  (reduce (partial part->datomic) [] partlist))

(defn generate-schema
  ([schema] (generate-schema schema {:gen-all? true}))
  ([schema {:keys [gen-all? index-all?] :as opts}]
   (reduce (partial schema->datomic opts) [] schema)))

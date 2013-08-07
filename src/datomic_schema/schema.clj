(ns datomic-schema.schema)

;; The main schema functions
(defmacro fields [& fielddefs]
  (let [defs (reduce (fn [a [nm tp & opts]] (assoc a (name nm) [tp (set opts)])) {} fielddefs)]
    `{:fields ~defs}))

(defonce schemalist (atom #{}))
(defonce partlist (atom #{}))

(defmacro defschema [nm & body]
  `(do
     (def ~nm (merge ~{:name (name nm) :basetype (keyword (name nm)) :namespace (name nm)} ~@body))
     (swap! schemalist conj (var ~nm))))

(defmacro defpart [nm]
  `(swap! partlist conj ~(keyword (name nm))))

(defmacro part [nm]
  `{:part ~(keyword "db.part" (name nm))})

;; The datomic schema conversion functions
(defn get-enums [tempid-fn basens part enums]
  (map (fn [n]
         (let [nm (if (string? n) (.replaceAll (.toLowerCase n) " " "-") (name n))]
           [:db/add (tempid-fn part) :db/ident (keyword basens nm)])) enums))

(defn field-to-datomic [tempid-fn basename part acc [fieldname [type opts]]]
  (let [uniq (or (opts :db.unique/value) (opts :db.unique/identity))
        dbtype (keyword "db.type" (if (= type :enum) "ref" (name type)))
        result
        {:db/id (tempid-fn :db.part/db)
         :db/ident (keyword basename fieldname)
         :db/valueType dbtype
         :db/index (boolean (opts :indexed))
         :db/cardinality (if (opts :many) :db.cardinality/many :db.cardinality/one)
         :db/doc (or (first (filter string? opts)) "")
         :db/fulltext (boolean (opts :fulltext))
         :db/isComponent (boolean (opts :component))
         :db/noHistory (boolean (opts :nohistory))
         :db.install/_attribute :db.part/db}]
    (concat
     acc
     [(if uniq (assoc result :db/unique uniq) result)]
     (if (= type :enum) (get-enums tempid-fn (str basename "." fieldname) part (first (filter vector? opts)))))))

(defn schema-to-datomic [tempid-fn acc schema]
  (let [key (:namespace schema)
        part (or (:part schema) :db.part/user)]
    (reduce (partial field-to-datomic tempid-fn key part) acc (:fields schema))))

(defn generate-schema [tempid-fn & schema]
  (reduce (partial schema-to-datomic tempid-fn) [] schema))

(defn part-to-datomic [tempid-fn acc part]
  (conj acc
        {:db/id (tempid-fn :db.part/db),
         :db/ident part
         :db.install/_partition :db.part/db}))

(defn build-parts [tempid-fn]
  (reduce (partial part-to-datomic tempid-fn) [] @partlist))

(defn build-schema [tempid-fn]
  (apply (partial generate-schema tempid-fn) (map #(deref %) @schemalist)))

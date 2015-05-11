(ns datomic-schema.migrate
  (:require
   [datomic.api :as d]))

;; This should take an existing schema and new schema and calculate the differences - then output a
;; transaction that can be submitted to get the current schema to the new state.

;; Optionally, provide a helper function that will describe the schema change and flag possible
;; things that might go wrong if your code isn't adjusted to cope.
;;  - Cardinality changes require code adjustments
;;  - Component changes may affect d/touch
;;  - Unique may fail if non-unique values.


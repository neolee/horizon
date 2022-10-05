(ns net.paradigmx.common.mysql
  (:require [net.paradigmx.common.db :as db]))

(defn schema-tables [ds]
  ((db/exec! ds) ["SHOW TABLES"]))

(defn table-exists? [ds dbname tname]
  (seq (for [x (schema-tables ds)
             :let [k (keyword (str "tables_in_" dbname))
                   nm (k x)]
             :when (= nm tname)]
         nm)))

(ns net.paradigmx.common.mysql
  (:require [net.paradigmx.common.db :as db]))

;; TODO :host, :user, :password should be loaded from config file
(defn db-spec-by-dbname [dbname]
  {:dbtype "mysql"
   :host "localhost"
   :user "paradigmx"
   :password "test"
   :dbname dbname}
  )

(defn schema-tables [ds]
  ((db/exec! ds) ["SHOW TABLES"]))

(defn table-exists? [ds dbname tname]
  (seq (for [x (schema-tables ds)
             :let [k (keyword (str "tables_in_" dbname))
                   nm (k x)]
             :when (= nm tname)]
         nm)))

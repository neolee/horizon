(ns net.paradigmx.common.mysql
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(def ds-opts {:builder-fn rs/as-unqualified-lower-maps})

;; TODO :host, :user, :password should be loaded from config file
(defn db-spec-by-dbname [dbname]
  {:dbtype "mysql"
   :host "localhost"
   :user "paradigmx"
   :password "test"
   :dbname dbname}
  )

(defn info-schema [ds]
  (jdbc/execute! ds ["SHOW TABLES"] ds-opts))

(defn table-exists? [ds dbname tname]
  (seq (for [x (info-schema ds)
             :let [k (keyword (str "tables_in_" dbname))
                   nm (k x)]
             :when (= nm tname)]
         nm)))

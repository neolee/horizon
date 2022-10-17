(ns net.paradigmx.common.db
  (:require [next.jdbc.result-set :as rs]
            [next.jdbc :as jdbc]))

(defn db-spec-from-config [config dbname]
  (if config
    (get-in config [:db])
    {:dbtype "mysql"
     :host "localhost"
     :port 3306
     :user "paradigmx"
     :password "test"
     :dbname dbname}))

(def rs-opts {:builder-fn rs/as-unqualified-lower-maps})

(defn- conn-with-opts [conn]
  (jdbc/with-options conn rs-opts))

(defn exec! [conn]
  (let [ds-opts (conn-with-opts conn)]
    (partial jdbc/execute! ds-opts)))

(defn exec-one! [conn]
  (let [ds-opts (conn-with-opts conn)]
    (partial jdbc/execute-one! ds-opts)))

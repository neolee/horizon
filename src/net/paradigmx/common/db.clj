(ns net.paradigmx.common.db
  (:require [honey.sql :as sql]
            [next.jdbc.result-set :as rs]
            [next.jdbc :as jdbc]))

(def ^:private rs-opts {:builder-fn rs/as-unqualified-lower-maps})

(defn- conn-with-opts [conn]
  (jdbc/with-options conn rs-opts))

(defn exec! [conn]
  (let [ds-opts (conn-with-opts conn)]
    (partial jdbc/execute! ds-opts)))

(defn exec-one! [conn]
  (let [ds-opts (conn-with-opts conn)]
    (partial jdbc/execute-one! ds-opts)))

(defn table-exists? [ds table-name]
  (-> (sql/format {:select :table_name :from :information_schema.tables
                            :where [:and
                                    [:like :table_type "BASE TABLE"]
                                    [:= :table_name table-name]]})
      ((exec-one! ds))))

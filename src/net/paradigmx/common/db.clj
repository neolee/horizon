(ns net.paradigmx.common.db
  (:require [next.jdbc.result-set :as rs]
            [next.jdbc :as jdbc]))

(def rs-opts {:builder-fn rs/as-unqualified-lower-maps})

(defn- conn-withopts [conn]
  (jdbc/with-options conn rs-opts))

(defn exec! [conn]
  (let [ds-opts (conn-withopts conn)]
    (partial jdbc/execute! ds-opts)))

(defn exec-one! [conn]
  (let [ds-opts (conn-withopts conn)]
    (partial jdbc/execute-one! ds-opts)))

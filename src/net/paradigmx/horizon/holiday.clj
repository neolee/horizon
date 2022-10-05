(ns net.paradigmx.horizon.holiday
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as h :refer [select from where]]
            [tick.core :as t]
            [net.paradigmx.common.db :as db]
            [net.paradigmx.horizon.meta :as meta]))

(def ds (jdbc/get-datasource (db/db-spec-from-config (meta/load-config) meta/dbname)))

(defn db-query-holiday
  "query holiday db for a given day"
  [d]
  (let [date (t/format (t/formatter "yyyy-MM-dd") d)]
    (-> (select :h.date :h.name :h.is_off)
        (from [:holiday :h])
        (where [:= date :h.date])
        (sql/format)
        ((db/exec-one! ds)))))

;; query holiday api interceptor, query string format 'yyyyMMdd'
(def holiday-query
  {:name :holiday-query
   :enter
   (fn [context]
     (let [date-str (get-in context [:request :path-params :date])
           d (t/parse-date date-str (t/formatter "yyyyMMdd"))]
       (assoc context :result (db-query-holiday d))))})

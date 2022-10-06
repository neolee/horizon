(ns net.paradigmx.horizon.holiday
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as h :refer [select from where]]
            [tick.core :as t]
            [net.paradigmx.common.db :as db]
            [net.paradigmx.horizon.meta :as meta]))

;; date-time helpers
(defn format-date [d]
  (t/format (t/formatter "yyyy-MM-dd") d))

(defn parse-date [s]
  (t/parse-date s (t/formatter "yyyyMMdd"))
  )

(defn day-of-week [d]
  (str (t/day-of-week d)))

(defn weekend? [d]
  (contains? (set (list (day-of-week 6) (day-of-week 7))) (day-of-week d)))

(defn- normal-day-result [d]
  (let [date (format-date d)
        weekend (weekend? d)
        nm (if weekend "周末" "")]
    {:date date :name nm :is_off weekend}))

;; db helpers
(def ds (jdbc/get-datasource (db/db-spec-from-config (meta/load-config) meta/dbname)))

(defn db-query-holiday
  "query holiday db for a given day"
  [d]
  (let [date (format-date d)]
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
     (let [s (get-in context [:request :path-params :date])
           d (parse-date s)
           r1 (db-query-holiday d)
           r2 (if (nil? r1) (normal-day-result d) r1)]
       (assoc context :result r2)))})

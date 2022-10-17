(ns net.paradigmx.horizon.holiday
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as h :refer [select from where order-by]]
            [tick.core :as t]
            [net.paradigmx.common.core :refer [if-let*]]
            [net.paradigmx.common.date :refer [format-date parse-date weekend? days-in-month]]
            [net.paradigmx.common.db :as db]
            [net.paradigmx.horizon.meta :as meta]))

;; date helpers
(defn- data-for-normal-day [d]
  (let [date (format-date d)
        weekend (weekend? d)
        nm (if weekend "周末" "")]
    {:date date :name nm :is_off weekend}))

(defn- data-for-day
  "build data for given date `d`, use data in `holidays` vector if it's in the vector,
  call `data-for-normal-day` otherwise"
  [holidays d]
  (let [s (format-date d)
        filtered-seq (filter #(= s (:date %)) holidays)
        query-result (first filtered-seq)]
    (if query-result query-result (data-for-normal-day d))))

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

(defn db-query-range
  "query holiday db for a given range of days"
  [d1 d2]
  (let [beginning (format-date d1)
        end (format-date d2)]
    (-> (select :h.date :h.name :h.is_off)
        (from [:holiday :h])
        (where [:between :h.date beginning end])
        (order-by :h.date)
        (sql/format)
        ((db/exec! ds)))))

;; api interceptors
;; query for a day's holiday info, query string format 'yyyyMMdd'
(def holiday-query
  {:name :holiday-query
   :enter
   (fn [context]
     (let [s (get-in context [:request :path-params :date])
           d (parse-date s)
           r1 (db-query-holiday d)
           r2 (if (nil? r1) (data-for-normal-day d) r1)]
       (assoc context :result r2)))})

;; query for a monthly calendar, with holiday info, query string format 'yyyyMMdd'
(def calendar-monthly-query
  {:name :calendar-monthly-query
   :enter
   (fn [context]
     (if-let* [s (get-in context [:request :path-params :date])
               d (parse-date s)
               days (days-in-month d)
               holidays (db-query-range (t/first-day-of-month d) (t/last-day-of-month d))]
       (assoc context :result (mapv (partial data-for-day holidays) days))
       context)
     )})

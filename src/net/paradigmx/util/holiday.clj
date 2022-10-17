(ns net.paradigmx.util.holiday
  (:gen-class)
  (:refer-clojure :exclude [partition-by])
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]
            [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as h :refer [create-table with-columns drop-table]]
            [tick.core :as t]
            [net.paradigmx.common.db :as db]
            [net.paradigmx.horizon.meta :refer [db-spec postgres? holiday-table]]
            [net.paradigmx.common.core :refer [parse-int-arg]]))

;; utility `holiday`
;; init the local Chinese holiday db through these steps:
;; 1. download yearly json data from (year from 2007 til the next)
;; https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/{year}.json
;; 2. write data to local h2 database, table `holiday` (id, date, name, is_off)
;;    constrain on `date`(YYYY-MM-dd) is unique key and major query criteria

(def url-template "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/%d.json")

(defn url-for-year [year]
  (format url-template year))

(defn data-for-year [year]
  (-> (client/get (url-for-year year))
      (:body)
      (json/read-str :key-fn keyword)
      (:days)))

(def ds (jdbc/get-datasource db-spec))

(defn drop-schema! []
  (print "dropping schema...")
  (-> (drop-table :if-exists holiday-table)
      (sql/format)
      ((db/exec! ds)))
  (println "done"))

;; HACK different implementations for `postgresql` and `mysql/mariadb`
(def ^:private create-table-id-clause
  (if postgres?
    [:id :int [:not nil] :generated :always :as :identity :primary :key]
    [:id :int [:not nil] :auto-increment :primary :key]))

(defn create-schema! []
  (print "creating schema...")
  (with-open [conn (jdbc/get-connection ds)]
    (jdbc/with-transaction [tx conn]
      (-> (create-table holiday-table :if-not-exists)
          (with-columns [create-table-id-clause
                         [:date [:char 10] [:not nil]]
                         [:name [:varchar 40] [:not nil]]
                         [:is_off :boolean [:not nil]]])
          (sql/format)
          ((db/exec! tx)))
      (let [nm (name holiday-table)
            sql-template "CREATE UNIQUE INDEX %s_unique_date ON %s (date)"
            sql (format sql-template nm nm)]
        ((db/exec! tx) [sql]))))
  (println "done"))

;; HACK different implementations for `postgresql` and `mysql/mariadb`
(defn- insert-clause [date name is-off]
  (let [base {:insert-into holiday-table
              :values [{:date date :name name :is-off is-off}]}
        mysql (assoc base
                     :on-duplicate-key-update {:name name :is-off is-off})
        pgsql (assoc base
                     :on-conflict :date
                     :do-update-set {:name name :is-off is-off})]
    (if postgres? pgsql mysql)))

(defn insert-row! [date name is-off]
  (-> (sql/format (insert-clause date name is-off))
      ((db/exec! ds)))
  (print "."))

(defn load-data-of-year! [year]
  (print (format "[%d]" year))
  (doseq [day (data-for-year year)]
    (let [{date :date name :name is-off :isOffDay} day]
      (insert-row! date name is-off)))
  (println))

(defn load-data! [begin end]
  (if (db/table-exists? ds (name holiday-table))
    (println "schema found")
    (create-schema!))
  (println (format "loading holiday data from year %d to %d" begin end))
  (doseq [x (range begin (+ end 2))]
    (load-data-of-year! x))
  (println "done."))

(defn -main
  "takes 0, 1 or 2 integer args which (if applicable) present the begin and end year to be processed,
  default to 2007 and the current year; drop data schema if the first arg is 0"
  [& args]
  (if (= 0 (parse-int-arg (first args)))
    (drop-schema!)
    (let [begin (or (parse-int-arg (first args)) 2007)
          end (or (parse-int-arg (second args)) (t/int (t/year)))]
      (load-data! begin end))))

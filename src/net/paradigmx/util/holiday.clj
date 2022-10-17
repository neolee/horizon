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
            [net.paradigmx.horizon.meta :refer [db-spec holiday-table]]
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

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn drop-schema! []
  (-> (drop-table holiday-table)
      (sql/format)
      ((db/exec! ds))))

(defn create-schema! []
  (with-open [conn (jdbc/get-connection ds)]
    (jdbc/with-transaction [tx conn]
      (-> (create-table holiday-table :if-not-exists)
          (with-columns [[:id :int [:not nil] :auto-increment :primary :key]
                         [:date [:char 10] [:not nil]]
                         [:name [:varchar 40] [:not nil]]
                         [:is_off :boolean [:not nil]]])
          (sql/format)
          ((db/exec! tx)))
      (-> (sql/format {:alter-table holiday-table
                       :add-index [:unique nil :date]})
          ((db/exec! tx))))))

(defn insert-row! [date name is-off]
  (-> (sql/format {:insert-into holiday-table
                   :values [{:date date :name name :is-off is-off}]
                   :on-duplicate-key-update {:name name :is-off is-off}})
      ((db/exec! ds)))
  (print ".")
  )

(defn do-data-of-year! [year]
  (print (format "[%d]" year))
  (doseq [day (data-for-year year)]
    (let [{date :date name :name is-off :isOffDay} day
          is-off (if is-off 1 0)]
      (insert-row! date name is-off)))
  (println))

(defn do-data! [begin end]
  (println (format "pulling and processing holiday data between year %d-%d" begin end))
  (doseq [x (range begin (+ end 2))]
    (do-data-of-year! x))
  (println "done."))

#_{:clj-kondo/ignore [:missing-else-branch]}
(defn -main
  "takes 0, 1 or 2 integer args which (if applicable) present the begin and end year to be processed,
  default to 2007 and the current year"
  [& args]
  (if (not (db/table-exists? ds (name holiday-table)))
    (create-schema!))
  (let [begin (or (parse-int-arg (first args)) 2007)
        end (or (parse-int-arg (second args)) (t/int (t/year)))]
    (do-data! begin end)))

(ns net.paradigmx.util.holiday
  (:gen-class)
  (:refer-clojure :exclude [partition-by])
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]
            [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as h :refer [create-table with-columns drop-table]]
            [net.paradigmx.common.mysql :as mysql]))

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

(def dbname "horizon")
(def tname "holiday")

(def ds (jdbc/get-datasource (mysql/db-spec-by-dbname dbname)))

(defn exec! [tx]
  (partial jdbc/execute! tx))

(defn drop-schema [ds]
  (-> (drop-table (keyword tname))
      (sql/format)
      ((exec! ds))))

(defn init-schema [ds]
  (with-open [conn (jdbc/get-connection ds)]
    (jdbc/with-transaction [tx conn]
      (-> (create-table (keyword tname) :if-not-exists)
          (with-columns [[:id :int [:not nil] :auto-increment :primary :key]
                         [:date [:char 10] [:not nil]]
                         [:name [:varchar 20] [:not nil]]
                         [:is_off :boolean [:not nil]]])
          (sql/format)
          ((exec! tx)))
      (-> (sql/format {:alter-table (keyword tname)
                       :add-index [:unique nil :date]})
          ((exec! tx))))))

(defn populate-data []
  )

#_{:clj-kondo/ignore [:missing-else-branch]}
(defn sync-data []
  (if (not (mysql/table-exists? ds dbname tname))
    (init-schema ds))
  (populate-data)
  )

(defn -main [& args]
  (apply println args))

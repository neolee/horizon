(ns net.paradigmx.util.holiday
  (:gen-class)
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]
            [tick.core :as t]
            [honey.sql :as sql]
            [honey.sql.helpers :as h :refer [select from join-by left-join join where order-by]]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

;; utility `holiday`
;; init the local Chinese holiday db through these steps:
;; 1. download yearly json data from (year from 2007 til the next)
;; https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/{year}.json
;; 2. write data to local h2 database, table `holiday` (id, year, month, day, desc, is_off)
;;    constrain on [year,month,day] is unique key and major query criteria

(def url-template "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/%d.json")

(defn url-for-year [year]
  (format url-template year))

(defn data-for-year [year]
  (-> (client/get (url-for-year year))
      (:body)
      (json/read-str :key-fn keyword)
      (:days)))

(def db-holiday "../../data/holiday")

(defn ds [dbtype dbname]
  (let [db {:dbtype dbtype :dbname dbname}]
    (jdbc/get-datasource db)))

(def ds-holiday (ds "h2" db-holiday))

(defn init-db []

  )

(defn -main [& args]
  (apply println args))

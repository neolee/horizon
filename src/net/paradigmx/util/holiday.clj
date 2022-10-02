(ns net.paradigmx.util.holiday
  (:gen-class)
  (:refer-clojure :exclude [partition-by])
  (:require [clojure.data.json :as json]
            [clj-http.client :as client]
            [next.jdbc :as jdbc]
            [honey.sql :as sql]
            [honey.sql.helpers :as h :refer [create-table with-columns drop-table]]
            [tick.core :as t]
            [net.paradigmx.common.mysql :as mysql]
            [net.paradigmx.common.db :as db]))

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

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn drop-schema! []
  (-> (drop-table (keyword tname))
      (sql/format)
      ((db/exec! ds))))

(defn init-schema! []
  (with-open [conn (jdbc/get-connection ds)]
    (jdbc/with-transaction [tx conn]
      (-> (create-table (keyword tname) :if-not-exists)
          (with-columns [[:id :int [:not nil] :auto-increment :primary :key]
                         [:date [:char 10] [:not nil]]
                         [:name [:varchar 40] [:not nil]]
                         [:is_off :boolean [:not nil]]])
          (sql/format)
          ((db/exec! tx)))
      (-> (sql/format {:alter-table (keyword tname)
                       :add-index [:unique nil :date]})
          ((db/exec! tx))))))

(defn insert-or-update! [date name is-off]
  (-> (sql/format {:insert-into (keyword tname)
                   :values [{:date date :name name :is-off is-off}]
                   :on-duplicate-key-update {:name name :is-off is-off}})
      ((db/exec! ds)))
  (print ".")
  )

(defn do-data-for-year! [year]
  (print (format "[%d]" year))
  (doseq [day (data-for-year year)]
    (let [{date :date name :name is-off :isOffDay} day
          is-off (if is-off 1 0)]
      (insert-or-update! date name is-off)))
  (println))

(defn pull-data! [begin end]
  (println (format "pulling and processing holiday data between year %d-%d" begin end))
  (doseq [x (range begin (+ end 2))]
    (do-data-for-year! x))
  (println "done."))

(defn- parse-arg [x]
  (if (= java.lang.String (type x))
    (Integer/parseInt x)
    x))

#_{:clj-kondo/ignore [:missing-else-branch]}
(defn -main
  "takes 0, 1 or 2 integer args which (if applicable) present the begin and end year to be processed,
  default to 2007 and the current year"
  [& args]
  (if (not (mysql/table-exists? ds dbname tname))
    (init-schema!))
  (let [begin (or (parse-arg (first args)) 2007)
        end (or (parse-arg (second args)) (t/int (t/year)))]
    (pull-data! begin end)))

(ns net.paradigmx.common.date
  (:require [tick.core :as t]
            [tick.alpha.interval :as ti]))

(defn format-date [d]
  (t/format (t/formatter "yyyy-MM-dd") d))

(defn parse-date [s]
  (t/parse-date s (t/formatter "yyyyMMdd"))
  )

(defn- day-of-week [d]
  (str (t/day-of-week d)))

(defn weekend? [d]
  (contains? (set (list (day-of-week 6) (day-of-week 7))) (day-of-week d)))

(defn days-in-month [d]
  (let [i (ti/bounds (t/year-month d))
        r (t/range (t/beginning i)
                   (t/end i)
                   (t/new-period 1 :days))]
    (map t/date r)))

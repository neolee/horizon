(ns build
  (:require [clojure.tools.build.api :as b]))

(def class-dir "target/classes")

(def basis (b/create-basis {:project "deps.edn"}))

(defn clean [_]
  (b/delete {:path "target"}))

(def jar-holiday "target/holiday.jar")

(defn uberjar-holiday [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "config"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :uber-file jar-holiday
           :basis basis
           :main 'net.paradigmx.util.holiday}))

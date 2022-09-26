(ns net.paradigmx.horizon.service
  (:require [hashp.core :include-macros true]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]))

(defn about-page
  [request]
  (ring-resp/response (format "Paradigm X Horizon (engine:clojure-%s)"
                              (clojure-version))))

(defn home-page
  [request]
  (ring-resp/response "Welcome to the REAL world!"))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def accepted (partial response 202))

;; database
(defonce database (atom {}))

(defn make-list [nm]
  {:name nm
   :items {}})

(defn make-list-item [nm]
  {:name nm
   :done? false})

(defn find-list-by-id  [dbval db-id]
  (get dbval db-id))

(defn find-list-item-by-ids [dbval list-id item-id]
  (get-in dbval [list-id :items item-id] nil)
  )

(defn list-item-add [dbval list-id item-id new-item]
  (if (contains? dbval list-id)
    (assoc-in dbval [list-id :items item-id] new-item)
    dbval))

;; interceptors
(def common-interceptors [(body-params/body-params) http/html-body])

(def echo
  {:name :echo
   :enter
   (fn [context]
     (let [request (:request context)
           response (ok request)]
       (assoc context :response response)))})

(def entity-reader
  {:name :entity-reader
   :leave
   (fn [context]
     (if-let [item (:result context)]
       (assoc context :response (ok item))
       context))})

(def db-interceptor
  {:name :database-interceptor
   :enter
   (fn [context]
     (update context :request assoc :database @database))
   :leave
   (fn [context]
     (if-let [[op & args] (:tx-data context)]
       (do
         (apply swap! database op args)
         (assoc-in context [:request :database] @database))
       context ))})

(def list-create
  {:name :list-create
   :enter
   (fn [context]
     (let [nm (get-in context [:request :query-params :name] "Unnamed List")
           new-list (make-list nm)
           db-id (str (gensym "lst"))
           url (route/url-for :list-view :params {:list-id db-id})]
       (assoc context
              :response (created new-list "Location" url)
              :tx-data [assoc db-id new-list])))})

(def list-view
  {:name :list-view
   :enter
   (fn [context]
     (if-let [db-id (get-in context [:request :path-params :list-id])]
       (if-let [the-list (find-list-by-id (get-in context [:request :database]) db-id)]
         (assoc context :result the-list)
         context)
       context))})

(def list-item-view
  {:name :list-item-view
   :leave
   (fn [context]
     (if-let [list-id (get-in context [:request :path-params :list-id])]
       (if-let [item-id (get-in context [:request :path-params :item-id])]
         (if-let [item (find-list-item-by-ids (get-in context [:request :database]) list-id item-id)]
           (assoc context :result item)
           context)
         context)
       context))})

(def list-item-create
  {:name :list-item-create
   :enter
   (fn [context]
     (if-let [list-id (get-in context [:request :path-params :list-id])]
       (let [nm (get-in context [:request :query-params :name] "Unnamed Item")
             new-item (make-list-item nm)
             item-id (str (gensym "item"))]
         (-> context
             (assoc :tx-data [list-item-add list-id item-id new-item])
             (assoc-in [:request :path-params :item-id] item-id)))
       context))})

;; the routes
(defn routes []
  #{["/" :get (conj common-interceptors `home-page)]
    ["/about" :get (conj common-interceptors `about-page)]
    ["/todo" :post [db-interceptor list-create]]
    ["/todo" :get echo :route-name :list-query-form]
    ["/todo/:list-id" :get [entity-reader db-interceptor list-view]]
    ["/todo/:list-id" :post [entity-reader list-item-view db-interceptor list-item-create]]
    ["/todo/:list-id/:item-id" :get [entity-reader list-item-view db-interceptor]]
    ["/todo/:list-id/:item-id" :put echo :route-name :list-item-update]
    ["/todo/:list-id/:item-id" :delete echo :route-name :list-item-delete]
    })

(def service {:env :prod
              ::http/routes routes

              ::http/resource-path "/public"

              ::http/type :jetty
              ::http/port 8000

              ::http/container-options {:h2c? true
                                        :h2? false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false
                                        ;:io.pedestal.http.jetty/http-configuration (org.eclipse.jetty.server.HttpConfiguration.)
                                        }})

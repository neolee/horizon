(ns net.paradigmx.horizon.service
  (:require [hashp.core :include-macros true]
            [clojure.data.json :as json]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring]))

(defn about-page
  [request]
  (ring/response (format "Paradigm X Horizon (engine:clojure-%s, uri:'%s')"
                         (clojure-version) (get-in request [:uri]))))

(defn home-page
  [request]
  (let [greeting (if-let [user (get-in request [:params :user])]
                   (str "Hi " user ", ")
                   "Hi, ")]
    (ring/response (str greeting "welcome to the REAL world!"))))

;; http helpers
(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))
(def created (partial response 201))
(def no-content (partial response 204))

(def supported-types ["text/html" "application/edn" "application/json" "text/plain"])

(defn accepted-type [context]
  (get-in context [:request :accept :field] "text/plain"))

(defn transform-content [body content-type]
  (case content-type
    "text/html" body
    "text/plain" body
    "application/edn" (pr-str body)
    "application/json" (json/write-str body)))

(defn coerce-to [response content-type]
  (-> response
      (update :body transform-content content-type)
      (assoc-in [:headers "Content-Type"] content-type)))

;; database
(defonce database (atom {}))

(defn db-new-list [nm]
  {:name nm
   :items {}})

(defn db-new-list-item [nm]
  {:name nm
   :done? false})

(defn db-query-list-by-id [dbval list-id]
  (get dbval list-id))

(defn db-query-list-item-by-ids [dbval list-id item-id]
  (get-in dbval [list-id :items item-id])
  )

(defn db-create-list-item [dbval list-id item-id new-item]
  (if (contains? dbval list-id)
    (assoc-in dbval [list-id :items item-id] new-item)
    dbval))

(defn db-update-list-item [dbval list-id item-id new-item]
  (cond-> dbval
    (some? (get-in dbval [list-id :items item-id]))
    (assoc-in [list-id :items item-id] new-item)))

(defn db-delete-list-item [dbval list-id item-id]
  (cond-> dbval
    (some? (get-in dbval [list-id :items item-id]))
    (update-in [list-id :items] dissoc item-id)))

;; common interceptors
(def html-body [(body-params/body-params) http/html-body])

(def echo
  {:name :echo
   :enter
   (fn [context]
     (let [request (:request context)
           response (ok request)]
       (assoc context :response response)))})

(def content-negotiator (content-negotiation/negotiate-content supported-types))

(def coerce-body
  {:name :coerce-body
   :leave
   (fn [context]
     (cond-> context
       (nil? (get-in context [:response :headers "Content-Type"]))
       (update-in [:response] coerce-to (accepted-type context))))})

(def entity-reader
  {:name :entity-reader
   :leave
   (fn [context]
     (if-let [object (:result context)]
       (assoc context :response (ok object))
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

;; application interceptor - todo
(defn list-with-id [context list-id]
  (db-query-list-by-id (get-in context [:request :database]) list-id)
  )

(defn list-item-with-id [context list-id item-id]
  (db-query-list-item-by-ids (get-in context [:request :database]) list-id item-id)
  )

(def list-create
  {:name :list-create
   :enter
   (fn [context]
     (let [nm (get-in context [:request :query-params :name] "Unnamed List")
           new-list (db-new-list nm)
           db-id (str (gensym "lst"))
           url (route/url-for :list-view :params {:list-id db-id})]
       (assoc context
              :response (created new-list "Location" url)
              :tx-data [assoc db-id new-list])))})

(def list-view
  {:name :list-view
   :enter
   (fn [context]
     (if-let [list-id (get-in context [:request :path-params :list-id])]
       (if-let [this-list (list-with-id context list-id)]
         (assoc context :result this-list)
         context)
       context))})

(def list-item-view
  {:name :list-item-view
   :leave
   (fn [context]
     (if-let [list-id (get-in context [:request :path-params :list-id])]
       (if-let [item-id (get-in context [:request :path-params :item-id])]
         (if-let [this-item (list-item-with-id context list-id item-id)]
           (assoc context :result this-item)
           context)
         context)
       context))})

(def list-item-create
  {:name :list-item-create
   :enter
   (fn [context]
     (if-let [list-id (get-in context [:request :path-params :list-id])]
       (if (list-with-id context list-id)
         (let [nm (get-in context [:request :query-params :name] "Unnamed Item")
               new-item (db-new-list-item nm)
               item-id (str (gensym "item"))
               url (route/url-for :list-item-view :params {:list-id list-id :item-id item-id})]
           (-> context
               (assoc-in [:request :path-params :item-id] item-id)
               (assoc :response (created new-item "Location" url)
                      :tx-data [db-create-list-item list-id item-id new-item])))
         context)
       context))})

;; PUT data should be JSON data with node name "new-item"
(def list-item-update
  {:name :list-item-update
   :enter
   (fn [context]
     (if-let [list-id (get-in context [:request :path-params :list-id])]
       (if-let [item-id (get-in context [:request :path-params :item-id])]
         (if-let [new-item (get-in context [:request :json-params :new-item])]
           (if (list-item-with-id context list-id item-id)
             (let [url (route/url-for :list-item-view :params {:list-id list-id :item-id item-id})]
               (-> context
                   (assoc :response (ok new-item "Location" url)
                          :tx-data [db-update-list-item list-id item-id new-item])))
             context)
           context)
         context)
       context))})

(def list-item-delete
  {:name :list-item-delete
   :enter
   (fn [context]
     (if-let [list-id (get-in context [:request :path-params :list-id])]
       (if-let [item-id (get-in context [:request :path-params :item-id])]
         (if (list-item-with-id context list-id item-id)
           (let [url (route/url-for :list-item-view :params {:list-id list-id :item-id item-id})]
             (-> context
                 (assoc :response (no-content (str url " deleted"))
                        :tx-data [db-delete-list-item list-id item-id])))
           context)
         context)
       context))})

;; the routes
(defn routes []
  #{["/" :get (conj html-body `home-page)]
    ["/about" :get (conj html-body `about-page)]
    ["/todo" :get echo :route-name :list-query-form]
    ["/todo" :post [db-interceptor list-create]]
    ["/todo/:list-id" :get [coerce-body content-negotiator entity-reader db-interceptor list-view]]
    ["/todo/:list-id" :post [list-item-view db-interceptor list-item-create]]
    ["/todo/:list-id/:item-id" :get [coerce-body content-negotiator entity-reader db-interceptor list-item-view]]
    ["/todo/:list-id/:item-id" :put [(body-params/body-params) list-item-view db-interceptor list-item-update]]
    ["/todo/:list-id/:item-id" :delete [db-interceptor list-item-delete]]
    })

;; the service
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

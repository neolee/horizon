(ns net.paradigmx.horizon.todo
  (:require [io.pedestal.http.route :as route]
            [ring.util.response :as ring]
            [hiccup.page :as hiccup]
            [net.paradigmx.horizon.common :as common]
            [net.paradigmx.common.core :refer [if-let*]]))

;; database
(defonce database (atom {}))

(defn db-new-list [nm]
  {:name nm
   :items {}})

(defn db-new-list-item [nm]
  {:name nm
   :done? false})

(defn db-query-list-by-id [db list-id]
  (get db list-id))

(defn db-query-list-item-by-ids [db list-id item-id]
  (get-in db [list-id :items item-id])
  )

(defn db-create-list-item [db list-id item-id new-item]
  (if (contains? db list-id)
    (assoc-in db [list-id :items item-id] new-item)
    db))

#_{:clj-kondo/ignore [:missing-else-branch]}
(defn db-update-list-item [db list-id item-id new-item]
  (if-let [old-item (get-in db [list-id :items item-id])]
    (assoc-in db [list-id :items item-id] (merge old-item new-item))))

(defn db-delete-list-item [db list-id item-id]
  (cond-> db
    (some? (get-in db [list-id :items item-id]))
    (update-in [list-id :items] dissoc item-id)))

;; page(s)
(defn- page-css []
  (str ".checklist {width: 12em; overflow: auto;}"
       ".checklist p {width: 7em; text-align: right;}"
       ".checklist label {width: 10em; float: right;}")
  )

(defn todos-page [_request]
  (ring/response
   (hiccup/html5
    [:head
     [:title "TODO Lists"]
     [:style (page-css)]]
    [:body
     [:h1#todo-title "Todos"]
     (for [list-id (keys @database)]
       (let [this-list (db-query-list-by-id @database list-id)
             list-name (:name this-list)]
         [:div {:id list-id}
          [:h3 list-name]
          [:fieldset {:class "checklist"}
           (for [item-id (keys (:items this-list))]
             (let [this-item (db-query-list-item-by-ids @database list-id item-id)
                   item-name (:name this-item)
                   item-done (:done? this-item)]
               [:div
                [:input {:type "checkbox" :id item-id :checked item-done}]
                [:label {:for item-id} item-name]]
               ))]
          ]))
     ])))

;; interceptor helpers
(defn list-with-id [ctx list-id]
  (db-query-list-by-id (get-in ctx [:request :database]) list-id)
  )

(defn list-item-with-id [ctx list-id item-id]
  (db-query-list-item-by-ids (get-in ctx [:request :database]) list-id item-id)
  )

;; db interceptor, the only interceptor that r/w the database
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

;; interceptors
(def list-create
  {:name :list-create
   :enter
   (fn [context]
     (let [nm (get-in context [:request :query-params :name] "Unnamed List")
           new-list (db-new-list nm)
           db-id (str (gensym "lst"))
           url (route/url-for :list-view :params {:list-id db-id})]
       (assoc context
              :response (common/created new-list "Location" url)
              :tx-data [assoc db-id new-list])))})

(def list-view
  {:name :list-view
   :enter
   (fn [context]
     (if-let* [list-id (get-in context [:request :path-params :list-id])
               this-list (list-with-id context list-id)]
       (assoc context :result this-list)
       context))})

(def list-item-view
  {:name :list-item-view
   :leave
   (fn [context]
     (if-let* [list-id (get-in context [:request :path-params :list-id])
               item-id (get-in context [:request :path-params :item-id])
               this-item (list-item-with-id context list-id item-id)]
       (assoc context :result this-item)
       context))})

(def list-item-create
  {:name :list-item-create
   :enter
   (fn [context]
     (if-let* [list-id (get-in context [:request :path-params :list-id])
               _ (list-with-id context list-id)]
       (let [nm (get-in context [:request :query-params :name] "Unnamed Item")
             new-item (db-new-list-item nm)
             item-id (str (gensym "item"))
             url (route/url-for :list-item-view :params {:list-id list-id :item-id item-id})]
         (-> context
             (assoc-in [:request :path-params :item-id] item-id)
             (assoc :response (common/created new-item "Location" url)
                    :tx-data [db-create-list-item list-id item-id new-item])))
       context ))})

;; PUT data should be JSON data with node name "new-item"
(def list-item-update
  {:name :list-item-update
   :enter
   (fn [context]
     (if-let* [list-id (get-in context [:request :path-params :list-id])
               item-id (get-in context [:request :path-params :item-id])
               new-item (get-in context [:request :json-params :new-item])
               _ (list-item-with-id context list-id item-id)]
       (let [url (route/url-for :list-item-view :params {:list-id list-id :item-id item-id})]
         (-> context
             (assoc :response (common/ok new-item "Location" url)
                    :tx-data [db-update-list-item list-id item-id new-item])))
       context))})

(def list-item-delete
  {:name :list-item-delete
   :enter
   (fn [context]
     (if-let* [list-id (get-in context [:request :path-params :list-id])
               item-id (get-in context [:request :path-params :item-id])
               _ (list-item-with-id context list-id item-id)]
       (let [url (route/url-for :list-item-view :params {:list-id list-id :item-id item-id})]
         (-> context
             (assoc :response (common/no-content (str url " deleted"))
                    :tx-data [db-delete-list-item list-id item-id])))
       context))})

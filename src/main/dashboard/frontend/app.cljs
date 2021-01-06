(ns dashboard.frontend.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]

            [reitit.frontend :as rf]
            [reitit.frontend.easy :as rfe]
            [reitit.coercion.spec :as rss]

            ;; [spec-tools.data-spec :as ds]
            ;; [fipp.edn :as fedn]

            ))

(defonce state (r/atom {}))

(defn set-api-key [key]
  (swap! state assoc :api-key key))

(defn with-api-key [url]
  (str url "&key=" (:api-key @state)))

(defn get-request [url]
  (go (let [response (<! (http/get url
                                    {:with-credentials? false
                                     ;;  :query-params {"since" 135}
                                     }
                                   ))]
        (prn "status " (:status response))
        (prn "body " (:body response)))))

(defn login []
  (let [url (with-api-key "https://api.torn.com/user/?selections=bars")
        response (get-request url)]))

(defn dashboard-component []
  [:p "Dahsboard"])

(defn login-component []
  [:div

   [:div.field
    [:label.label "API Key"]]

   [:div.field
    [:div.control
     [:input.input {:type "text"
                    :on-change #(set-api-key (.. % -target -value))}]]]

   [:div.field
    [:div.control
     [:a.button.is-primary
      {:on-click login}
      "Login"]]]])

(defonce match (r/atom nil))

(defn layout []
  [:div.container
   (if @match
     (let [view (:view (:data @match))]
       [view @match]))])

(def routes
  [["/"
    {:name ::login
     :view login-component}]

    ["/dashboard"
     {:name ::dashboard
      :view dashboard-component}]

   ;; ["/item/:id"
   ;;  {:name ::item
   ;;   :view item-page
   ;;   :parameters {:path {:id int?}
   ;;                :query {(ds/opt :foo) keyword?}}}]

   ])



(defn init! []
  (rfe/start!
   (rf/router routes {:data {:coercion rss/coercion}})
   (fn [m] (reset! match m))
   ;; set to false to enable HistoryAPI
   {:use-fragment true})
  (rd/render [layout] (.getElementById js/document "app")))

(defn init []
  (init!)
  #_(rd/render [layout] (.getElementById js/document "app")))

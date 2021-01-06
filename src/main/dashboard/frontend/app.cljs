(ns dashboard.frontend.app
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.core :as r]
            [reagent.dom :as rd]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]]))

(defonce api-key (r/atom ""))

(defn set-api-key [key]
  (reset! api-key key))

(defn with-api-key [url api-key]
  (str url "&key=" api-key))

(defn get-request [url]
  (go (let [response (<! (http/get url
                                    {:with-credentials? false
                                     ;;  :query-params {"since" 135}
                                     }
                                   ))]
        (prn "status " (:status response))
        (prn "body " (:body response)))))

(defn login []
  (let [url (with-api-key "https://api.torn.com/user/?selections=bars" @api-key)
        response (get-request url)]))

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

(defn layout []
  [:div.container
   [login-component]])

(defn init []
  (rd/render [layout] (.getElementById js/document "app")))

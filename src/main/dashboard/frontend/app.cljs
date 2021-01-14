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

            [dashboard.frontend.constants :as constants]

            ))

(defonce state (r/atom {}))

(defn set-api-key [key]
  (swap! state assoc :api-key key)
  (.setItem js/localStorage "api-key" key))

(defn ok? [response]
  (= 200 (:status response)))

(defn get-request [url on-success on-failure]
  (go (let [response (<! (http/get url {:with-credentials? false
                                        :query-params {"key" (:api-key @state)}}))]
        (if (ok? response)
          (on-success (:body response))
          (on-failure)))))

(defn login []
  (let [url "https://api.torn.com/user/?selections=bars,profile"
        login-successful (fn [body]
                           (swap! state assoc :dashboard body)
                           (swap! state assoc :login-error nil)
                           (rfe/replace-state ::dashboard))
        login-failed (fn []
                       (swap! state assoc :login-error "API Key invalid")
                       (set-api-key nil))]
    (get-request url login-successful login-failed)))

(defn find-cheapest-item-in-list [list]
  (->> list
       (sort-by :cost)
       (first)))

(defn alcohol []
  (let [alcohol-item-id 180
        url (str "https://api.torn.com/market/" alcohol-item-id "?selections=bazaar,itemmarket")]
    (get-request url
                 (fn [body]
                   (swap! state assoc-in [:alcohol-prices alcohol-item-id] body)
                   (prn body))
                 (fn []
                   (prn "fail"))))
  )

(comment
  (alcohol)

  (let [alcohol-item-id 180
        cheapest-in-bazaar (-> (get-in @state [:alcohol-prices alcohol-item-id])
                               :bazaar
                               (find-cheapest-item-in-list))
        cheapest-on-item-market (-> (get-in @state [:alcohol-prices alcohol-item-id])
                                    :itemmarket
                                    (find-cheapest-item-in-list))]

    (find-cheapest-item-in-list [cheapest-in-bazaar cheapest-on-item-market]))

  (let [url "https://api.torn.com/market/180?selections=bazaar,itemmarket"]
    (get-request url
                 (fn [body]
                   (prn body))
                 (fn []
                   (prn "fail"))))

  )

(defn dashboard-component []
  ;; {:server_time 1609940746, :happy {:current 4745, :maximum 5025, :increment 5, :interval 900, :ticktime 854, :fulltime 50354}, :life {:current 4235, :maximum 4235, :increment 254, :interval 300, :ticktime 254, :fulltime 0}, :energy {:current 70, :maximum 150, :increment 5, :interval 600, :ticktime 254, :fulltime 9254}, :nerve {:current 33, :maximum 85, :increment 1, :interval 300, :ticktime 254, :fulltime 15554}, :chain {:current 0, :maximum 25000, :timeout 0, :modifier 1, :cooldown 0}}
  (let [dashboard (:dashboard @state)
        name (:name dashboard)]
      [:p (str "Welcome, " name)]))

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
      "Login"]]]

   (when (:login-error @state)
     [:p "Please check your API key, couldn't log in"])])

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
  (let [api-key (.getItem js/localStorage "api-key")]
    (init!)
    (when api-key
      (set-api-key api-key)
      (login))))

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

(defn cheapest-in-bazaar-or-item-market-by-type-and-id [type id]
  (let [listings (get-in @state [type id])
        cheapest-in-bazaar (-> listings
                               :bazaar
                               (find-cheapest-item-in-list))
        cheapest-on-item-market (-> listings
                                    :itemmarket
                                    (find-cheapest-item-in-list))]

    (find-cheapest-item-in-list [cheapest-in-bazaar cheapest-on-item-market])))

(def alcohol-prices :alcohol-prices)

(defn alcohol [alcohol-item-id]
  (let [url (str "https://api.torn.com/market/" alcohol-item-id "?selections=bazaar,itemmarket")]
    (get-request url
                 (fn [body]
                   (swap! state assoc-in [alcohol-prices alcohol-item-id] (merge body {:id alcohol-item-id}))
                   ;;(prn body)
                   )
                 (fn []
                   (prn "fail")))))

(defn get-all-alcohol-prices []
  (->> constants/alcohol
   (map (fn [{id :id}]
          (alcohol id)))))

(defn get-all-cheapest-alcohol-prices []
  (->> constants/alcohol
       (filter (fn [item] (not (true? (:cannot-be-sold item)))))
       (map (fn [item]
              (merge
               {:id (:id item)
                :name (:name item)
                :nerve (:nerve item)
                :cooldown-in-minutes (:cooldown-in-minutes item)}
               (cheapest-in-bazaar-or-item-market-by-type-and-id alcohol-prices (:id item)))))))

(defn alcohol-ratios []
  (->> (get-all-cheapest-alcohol-prices)
       (map (fn [item]
              (let [nerve-to-dollar-ratio (/ (:cost item ) (:nerve item))
                    nerve-to-cd-ratio     (/ (:cooldown-in-minutes item) (:nerve item))]
                (merge item
                       {:nerve-to-dollar-ratio nerve-to-dollar-ratio
                        :nerve-to-cd-ratio nerve-to-cd-ratio
                        :nerve-to-dollar-to-nerve-to-cd-ratio (/ nerve-to-dollar-ratio nerve-to-cd-ratio)}))))
       (sort-by :nerve-to-cd-ratio)))

(comment
  ;; (alcohol)
  ;; (get-all-alcohol-prices)

  (let [alcohol-item-id 180
        cheapest-in-bazaar (-> (get-in @state [alcohol-prices alcohol-item-id])
                               :bazaar
                               (find-cheapest-item-in-list))
        cheapest-on-item-market (-> (get-in @state [alcohol-prices alcohol-item-id])
                                    :itemmarket
                                    (find-cheapest-item-in-list))]

    (find-cheapest-item-in-list [cheapest-in-bazaar cheapest-on-item-market])
    (cheapest-in-bazaar-or-item-market-by-type-and-id alcohol-prices alcohol-item-id))

  (->> (get-all-cheapest-alcohol-prices)
       (map (fn [item]
              (let [nerve-to-dollar-ratio (/ (:cost item ) (:nerve item))
                    nerve-to-cd-ratio     (/ (:cooldown-in-minutes item) (:nerve item))]
                (merge item
                       {:nerve-to-dollar-ratio nerve-to-dollar-ratio
                        :nerve-to-cd-ratio nerve-to-cd-ratio
                        :nerve-to-dollar-to-nerve-to-cd-ratio (/ nerve-to-dollar-ratio nerve-to-cd-ratio)}))))
       (sort-by :nerve-to-cd-ratio))


  (->> constants/alcohol
       (filter (fn [item] (not (true? (:cannot-be-sold item)))))
       (map (fn [item]
              (merge
               {:name (:name item)
                :nerve (:nerve item)
                :cooldown-in-minutes (:cooldown-in-minutes item)}
               (cheapest-in-bazaar-or-item-market-by-type-and-id alcohol-prices (:id item)))))))

(defn dashboard-component []
  (let [dashboard (:dashboard @state)
        name (:name dashboard)]
    [:div
     [:p (str "Welcome, " name)]
     [:br]
     [:h1 "Ratios for alchohol"]
     [:table.table

      [:thead
       [:tr
        [:th "Name"]
        [:th "nerve"]
        [:th "cooldown-in-minutes"]
        [:th "nerve-to-dollar-ratio"]
        [:th "nerve-to-cd-ratio"]
        [:th "nerve-to-dollar-to-nerve-to-cd-ratio"]]]

      [:tbody
       (map
        (fn [{:keys [id name nerve cooldown-in-minutes
                     nerve-to-dollar-ratio nerve-to-cd-ratio
                     nerve-to-dollar-to-nerve-to-cd-ratio]}]
          [:tr {:key id}
           [:td name]
           [:td nerve]
           [:td cooldown-in-minutes]
           [:td nerve-to-dollar-ratio]
           [:td nerve-to-cd-ratio]
           [:td nerve-to-dollar-to-nerve-to-cd-ratio]])
        (alcohol-ratios))]]]))



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

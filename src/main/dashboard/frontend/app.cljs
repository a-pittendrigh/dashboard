(ns dashboard.frontend.app
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

(defn login []
  [:div

   [:div.field
    [:label.label "API Key"]]

   [:div.field
    [:div.control
    [:input.input {:type "text"}]]]

   [:div.field
    [:div.control
     [:a.button.is-primary "Login"]]]])

(defn layout []
  [:div.container
   [login]])

(defn init []
  (rd/render [layout] (.getElementById js/document "app")))

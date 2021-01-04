(ns dashboard.frontend.app
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

(defn child [name]
  [:p "Hi, I am " name])

(defn childcaller []
  [child "Hello World ss"])

(defn init []
  (rd/render [childcaller]
             (.getElementById js/document "app")))

(ns dashboard.frontend.app
  (:require [reagent.core :as r]
            [reagent.dom :as rd]))

(defn child [name]
  [:p "Hi, I am " name])

(defn childcaller []
  [child "Foo Bar"])

(defn init []
  (println "Hello World")

  (rd/render [childcaller]
             (.-body js/document)))

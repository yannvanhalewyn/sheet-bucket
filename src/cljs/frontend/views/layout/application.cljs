(ns frontend.views.layout.application
  (:require [frontend.views.sheet :as sheet]
            [re-frame.core :refer [subscribe]]))

(defn index-view []
  [:div
   [:h1 "Home"]
   [:a {:href "#sheets/"} "Sheets"]])

(defn sheet-list-view []
  [:div
   [:h1 "Sheets"]
   [:a {:href "#"} "Home"]])

(defn active-panel []
  (let [route (subscribe [:sub/active-route])]
    (case (:route/handler @route)
      :route/index [index-view]
      :route/sheets [sheet-list-view]
      :route/sheet [sheet/component {:sheet-id (get-in route [:route/params :sheet/id])}])))

(defn component [props]
  [:div.u-max-height
   [:div.navbar
    [:div.navbar__home
     [:a.navbar__item--icon.u-block {:href "/#"}
      [:i.material-icons "home"]]]
    [:div.navbar__breadcrumbs]
    [:div.navbar__search
     [:div.typeahead
      [:div.typeahead__icon
       [:i.material-icons "search"]]
      [:input.typeahead__input {:type "text" :placeholder "Zoek..."}]]]
    [:div.navbar__right
     [:div.navbar__item.navbar__item--icon
      [:i.material-icons "person"]]]]
   [:div.l-app.l-content
    [active-panel]]])

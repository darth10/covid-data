(ns covid-data.core
  (:require [cheshire.core :as json]
            [clj-time.format :as f]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as s]))

(defonce covid-data-url "https://pomber.github.io/covid19/timeseries.json")
;; (def x (get-covid-data-map covid-data-url))

(defn get-covid-data-map [url]
  (with-open [in (io/input-stream url)]
    (-> in io/reader json/parse-stream)))

(defn get-sanitized-data-key [data-key]
  (-> data-key
      (s/replace #"[,\*\(\)]" "")
      (s/replace " " "-")
      keyword))

(defn is-date-before [until-date data-value]
  (.isBefore (f/parse-local (get data-value "date")) until-date))

(defn transform-covid-data [until-date [data-key data-value]]
  (let [data-until-date (filter (partial is-date-before until-date)
                                data-value)
        total-cases (->> data-until-date
                         (map #(get % "confirmed" 0))
                         (reduce +))]
    {(get-sanitized-data-key data-key)
     (if-not (and (seq data-until-date) (> total-cases 0))
       0
       (float (/ (->> data-until-date
                      (map #(get % "recovered" 0))
                      (filter #(not (nil? %)))
                      (reduce +))
                 total-cases)))}))

;; (transform-covid-data (f/parse-local "2020-03-19") ["Portugal" (x "Portugal")])

(defn -main [& args]
  (if-let [date-arg (f/parse-local (first args))]
    (let [data (get-covid-data-map covid-data-url)
          stats (into [] (map (partial transform-covid-data date-arg) data))]
      (pp/pprint stats))
    (println "Invalid date format: " (first args))))

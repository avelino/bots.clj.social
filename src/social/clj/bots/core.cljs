(ns social.clj.bots.core
  (:require ["yaml$default" :as yaml]
            ["fs" :as fs]
            [social.clj.bots.db :as db]
            [social.clj.bots.feed :as feed]
            [clojure.walk :as walk]
            [promesa.core :as p]))

(defn config-reader
  "reads the configuration file (yaml) and calls the function to process the feed"
  [client yaml-name]
  (p/doseq [[k yml] (get (walk/keywordize-keys
                          (js->clj (yaml/parse
                                    (fs/readFileSync yaml-name "utf8")))) :bots)]
    (let [clients {:client client
                   :token (aget js/process.env (:env yml))
                   :hashtags (:hashtags yml)
                   :matcher (:matcher yml)}]
      (feed/feed-process clients (:feed yml)
                         :xml (or (:xml yml) false)))))

(defn -main
  "initial software here"
  []
  (p/let [client db/redis-conn]
    (p/do
      (.connect client)
      (.ping client)
      (config-reader client (or js/process.env.CONFIG_BOTS
                                "./bots.yml"))
      ;; TODO: connection is closing earlier than expected,
      ;; causing the save to database process to fail.
      ;; I put a time limit on gh actions
      (js/setTimeout
       (fn [] (-> (.quit client)
                  (println :redis :quit-connect))) 3000))))

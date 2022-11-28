(ns social.clj.bots.core
  (:require ["yaml$default" :as yaml]
            ["fs" :as fs]
            [social.clj.bots.db :as db]
            [social.clj.bots.feed :as feed]
            [clojure.walk :as walk]
            [nbb.core :as nbb]
            [promesa.core :as p]))

(defn config-reader [client yaml-name]
  "reads the configuration file (yaml) and calls the function to process the feed"
  (p/doseq [[k yml] (get (walk/keywordize-keys
                          (js->clj (yaml/parse
                                    (fs/readFileSync yaml-name "utf8")))) :bots)]
    (let [clients {:client client
                   :token (aget js/process.env (:env yml))
                   :hashtags (:hashtags yml)}]
      (feed/feed-process clients (:feed yml)))))

(defn -main []
  "initial software here"
  (p/let [client db/redis-conn]
    (p/do
      (.connect client)
      (config-reader client (or js/process.env.CONFIG_BOTS
                                "./bots.yml"))
      (nbb/await (.quit client)))))

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
  (println "Reading configuration from:" yaml-name)
  (p/let [config (walk/keywordize-keys
                  (js->clj (yaml/parse
                            (fs/readFileSync yaml-name "utf8"))))
          bots (:bots config)]
    (println "Found" (count bots) "bots to process")
    (p/all (for [[k yml] bots]
             (let [clients {:client client
                            :token (aget js/process.env (:env yml))
                            :hashtags (:hashtags yml)
                            :matcher (:matcher yml)}]
               (println "Processing bot:" k "with feed:" (:feed yml))
               (feed/feed-process clients (:feed yml)
                                  :xml (or (:xml yml) false)))))))

(defn -main
  "initial software here"
  []
  (println "Starting bots.clj.social feed processor...")
  (p/catch
   (p/let [client db/redis-conn]
     (p/do
       (println "Connecting to Redis...")
       (.connect client)
       (println "Testing Redis connection...")
       (.ping client)
       (println "Redis connected successfully")
       (p/let [_ (config-reader client (or js/process.env.CONFIG_BOTS
                                           "./bots.yml"))]
         (println "All feeds processed successfully")
         (println "Disconnecting from Redis...")
         (.quit client)
         (println "Redis disconnected - process complete"))))
   (fn [error]
     (println "Error in main process:" error)
     (js/process.exit 1))))

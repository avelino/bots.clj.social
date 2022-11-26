(ns social.clj.bots.db
  (:require ["redis$default" :as redis]
            [nbb.core :as nbb]))

(def redis-conn
  "redis client init"
  (redis/createClient
   #js{:url js/process.env.DATABASE_URL}))

(defn save [client obj]
  "save data in the storer"
  (let [key (:key obj)]
    (prn :save key)
    (nbb/await (.set client key (js/JSON.stringify (clj->js obj))))))

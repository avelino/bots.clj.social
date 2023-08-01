(ns social.clj.bots.mastodon
  (:require ["masto$createRestAPIClient" :as login]
            [nbb.core :as nbb]
            [promesa.core :as p]))

(defn masto [token]
  "instantiate mastodon client, using token from env var"
  (login #js{:url "https://clj.social"
             :accessToken token}))

(defn toot
  "public toot"
  [status visibility key token]
  (nbb/await
   (p/let [body (.create (.. (masto token) -v1 -statuses)
                         #js {:status status
                              :visibility visibility})
           obj (js->clj body)
           id (get obj "id")
           createdat (get obj "createdAt")]
     {:toot-createdat createdat
      :toot-id id
      :key key})))

(defn remove
  "remove toot"
  [toot-id token because]
  (prn :toot-remove :toot-id toot-id :because because)
  (nbb/await
   (p/let [cli (masto token)]
     (.remove (.-statuses cli) toot-id))))

(defn toot-text
  "formats the text that will be published"
  [obj hashtags]
  (str (:title obj) "\n\n"
       (:link obj) "\n\n"
       (:description obj) "\n\n"
       hashtags))

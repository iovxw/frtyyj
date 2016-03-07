(ns frtyyj.core
  (:require [frtyyj.tgapi :as tgapi]
            [esab16.core :as esab16]
            [clojure.string :as string]
            [clojure.tools.logging :as log])
  (:gen-class))

(def code [\u200B \u200C \u200D \uFEFF \u2060 \u2061 \u2062 \u2063
           \u2064 \u2069 \u206A \u206B \u206C \u206D \u206E \u206F])

; For Telegram
(defn str-encode [string]
  (string/replace (esab16/str-encode string) \u202c \u2069))
(defn str-decode [string]
  (esab16/str-decode (string/replace string \u2069 \u202c)))

(defn updates-seq
  ([bot] (updates-seq bot 0))
  ([bot offset]
   (Thread/sleep 500)
   (let [updates (try (tgapi/get-updates bot offset)
                      (catch Exception e (log/error e "") []))
         new-offset (-> updates (last) (get :update_id -1) (+ 1))]
     (lazy-cat updates (updates-seq bot new-offset)))))

(defn encode-msg [msg]
  (let [buf (StringBuffer.)]
    (loop [msg msg, block nil, in-quote false]
      (case (first msg)
        \( (recur (next msg) nil true)
        \) (do (.append buf (str-encode block))
                      (recur (next msg) nil false))
        nil (do (when in-quote
                  (.append buf (str-encode block)))
                (.toString buf)) ; DONE!
        ; default
        (do (if in-quote
              (recur (next msg) (str block (first msg)) true)
              (do (.append buf (first msg))
                  (recur (next msg) nil false))))))))

(defn decode-msg [msg]
  (let [buf (StringBuffer.)]
    (loop [msg msg, block nil, in-quote false]
      (case (first msg)
        (\u200B \u200C \u200D \uFEFF \u2060 \u2061 \u2062 \u2063
         \u2064 \u2069 \u206A \u206B \u206C \u206D \u206E \u206F)
        (recur (next msg) (str block (first msg)) true)
        nil (do (when in-quote
                  (.append buf (format " (%s)" (str-decode block))))
                (.toString buf)) ; DONE!
        ; default
        (do (if in-quote
              (do (.append buf (format " (%s) " (str-decode block)))
                  (.append buf (first msg))
                  (recur (next msg) nil false))
              (do (.append buf (first msg))
                  (recur (next msg) nil false))))))))

(defn gen-answer [query]
  (let [result (encode-msg query)]
    [{"type" "article"
      "id" "0"
      "title" "预览: "
      "description" result
      "message_text" result}]))

(defn is-encoded-msg [msg]
  (some (fn [c] (some #(= % c) code))
        msg))

(defn -main [& vars]
  (let [bot (tgapi/new-bot (if-not (first vars)
                             (do (println "请将 Bot Key 作为参数")
                                 (System/exit 1))
                             (first vars)))]
    (loop [updates (updates-seq bot)]
      (println (first updates))
      (try
        (if-let [query ((first updates) :inline_query)]
          (tgapi/answer-inline-query bot (query :id)
                                     (gen-answer (query :query)))
          (if-let [message ((first updates) :message)]
            (if-let [r-message (message :reply_to_message)]
              (if (and (string/starts-with? (message :text) (str "@" (bot :username)))
                       (is-encoded-msg (r-message :text)))
                (tgapi/send-message bot ((message :chat) :id)
                                    (decode-msg (r-message :text)))))))
        (catch Exception e (log/error e "")))
      (recur (rest updates)))))

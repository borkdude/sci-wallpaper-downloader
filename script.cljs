;; This code is based on this gist: https://gist.github.com/yogthos/d9d2324016f62d151c9843bdac3c0f23

(require '[clojure.string :as string])

;; provided by bindings:
(declare parseJSON, saveFile, fileExists, getUrl, promise, thenP, catchP,
         promiseAll, on, error)

(defn page [gallery-url page-number]
  (string/replace gallery-url #"PageNumber=\d+&" (str "PageNumber=" page-number "&")))

(defn parse-image-url
  [{:keys [GalleryUrl Index UrlSignature URLFilename Sizes]}]
  (let [size      (->> [:O :5K :4K :X5 :X4 :X3]
                       (filter #(Sizes %))
                       (first))
        ext       (-> size Sizes :ext)
        file-name (str URLFilename "-" (name size) "." ext)]
    (when size
      {:url       (string/join "/" [GalleryUrl Index UrlSignature (name size) file-name])
       :file-name file-name})))

(defn follow-redirects [url cb]
  (println "follow redirects" url)
  (getUrl
   url
   (fn [res]
     (let [location (aget (aget res "headers") "location")]
       (if location
         (follow-redirects location cb)
         (cb res))))))

(defn save-file [{:keys [url file-name]} cb]
  (follow-redirects
   url
   (fn [res]
     (println "saving" url)
     (saveFile file-name res cb))))

(defn save-links [[link & links]]
  (when link
    (if (fileExists (:file-name link))
      (do
        (println "skipping" (:file-name link))
        (save-links links))
      (save-file link #(save-links links)))))

(defn parse-page [url]
  (fn [resolve _reject]
    (getUrl url
            (fn [res]
              (let [body (atom "")]
                (-> res
                    (on "data" #(swap! body str (str %)))
                    (on "end" (fn end []
                                (-> @body
                                    parseJSON
                                    (js->clj :keywordize-keys true)
                                    resolve)))))))))

(defn parse-image-links [pages]
  (-> (map #(promise (parse-page %)) pages)
      (promiseAll)
      (thenP #(->> %
                   (map :Images)
                   (apply concat)
                   (map parse-image-url)
                   set
                   save-links))
      (catchP error)))

(defn parse-gallery [url {:keys [TotalItems TotalPages]}]
  (println "gallery contains" TotalPages "pages with" TotalItems "images")
  (parse-image-links (map (partial page url) (range TotalPages))))

(defn parse-url [url]
  (println "parsing" url)
  (-> (promise (parse-page url))
      (thenP #(parse-gallery url (:Pagination %)))
      (catchP error)))

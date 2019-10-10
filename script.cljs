;; This code is based on this gist: https://gist.github.com/yogthos/d9d2324016f62d151c9843bdac3c0f23

(require '[clojure.string :as string])
(require '[node.interop :as i])

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
  (i/log "follow redirects" url)
  (i/getUrl
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
     (i/log "saving" url)
     (i/saveFile file-name res cb))))

(defn save-links [[link & links]]
  (when link
    (if (i/fileExists (:file-name link))
      (do
        (i/log "skipping" (:file-name link))
        (save-links links))
      (save-file link #(save-links links)))))

(defn parse-page [url]
  (fn [resolve _reject]
    (i/getUrl url
              (fn [res]
                (let [body (atom "")]
                  (-> res
                      (i/on "data" #(swap! body str (str %)))
                      (i/on "end" (fn end []
                                    (-> @body
                                        i/parseJSON
                                        (js->clj :keywordize-keys true)
                                        resolve)))))))))

(defn parse-image-links [pages]
  (-> (map #(i/promise (parse-page %)) pages)
      (i/promiseAll)
      (i/thenP #(->> %
                     (map :Images)
                     (apply concat)
                     (map parse-image-url)
                     set
                     save-links))
      (i/catchP i/error)))

(defn parse-gallery [url {:keys [TotalItems TotalPages]}]
  (i/log "gallery contains" TotalPages "pages with" TotalItems "images")
  (parse-image-links (map (partial page url) (range TotalPages))))

(defn parse-url [url]
  (i/log "parsing" url)
  (-> (i/promise (parse-page url))
      (i/thenP #(parse-gallery url (:Pagination %)))
      (i/catchP i/error)))

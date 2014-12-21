(defproject stream-crawler "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [twitter-api "0.7.7"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [postgresql "9.3-1102.jdbc41"]
                 [clj-time "0.8.0"]
                 [korma "0.4.0"]
                 [environ "1.0.0"]]
  :plugins [[lein-environ "1.0.0"]]
  :main ^:skip-aot stream-crawler.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :jvm-opts ["-Duser.timezone=UTC"])

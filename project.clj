(defproject stream-crawler "0.1a"
  :description "Stream Crawler for semant.io"
  :url "http://dash.semant.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [twitter-api "0.7.7"]
                 [org.clojure/java.jdbc "0.3.2"]
                 [postgresql "9.3-1102.jdbc41"]
                 [clj-time "0.8.0"]
                 [korma "0.4.0"]
                 [environ "1.0.0"]
                 [org.clojure/data.json "0.2.5"]
                 [joda-time "2.2"]
                 [ch.qos.logback/logback-classic "1.0.11"]
                 [org.slf4j/slf4j-api "1.7.5"]
                 [org.slf4j/jcl-over-slf4j "1.7.5"]
                 [org.slf4j/log4j-over-slf4j "1.7.5"]
                 [org.slf4j/jul-to-slf4j "1.7.5"]
                 [org.clojure/core.incubator "0.1.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/tools.macro "0.1.2"]
                 [org.clojure/data.json "0.2.2"]
                 [org.clojars.gjahad/debug-repl "0.3.3"]]
  :plugins [[lein-environ "1.0.0"]]
  :main stream-crawler.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}}
  :jvm-opts ["-Duser.timezone=UTC"]
  :aliases {"all" ["with-profile" "dev:1.4,dev:1.5,dev:1.6"]})


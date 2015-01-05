# stream-crawler

A simple Twitter Stream API bot that checks for keywords and saves tweets to a
database. Specifically designed to be used in conjunction with [the semant.io
dashboard](http://dash.semant.io).

## Usage

You will need leiningen to build the project. On OS X you can install using
`brew install leiningen`.

## Deployment

You can deploy the code using the built-in `deploy.sh` script. It is currently
very simplistic and simply builds then uploads the jar to the server.

## Configuration and running

All configuration is handled by ENV variables. You must specify the correct
environment variables or the crawler will fail to run. An example invocation
might look like this:

```
DB_PORT='5432' DB_URL='my.database.com' DB_NAME='my_db_name' DB_USER='my_user' DB_PASS='my_pass' TWITTER_CONSUMER_KEY="my_key" TWITTER_CONSUMER_SECRET="my_secret" USER_ACCESS_TOKEN="my_token" USER_ACCESS_TOKEN_SECRET="my_token_secret" java -jar /path/tostream-crawler-0.1a-standalone.jar
```

All of the environment variables specified above must be present for the crawler
to run.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

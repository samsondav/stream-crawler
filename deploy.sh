#!/bin/sh
lein uberjar && scp target/uberjar/stream-crawler-0.1a-standalone.jar DonJuan:~/apps/

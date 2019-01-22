#!/bin/sh
NOVELANG_VERSION=0.56.0
NOVELANG_HOME=~/.m2/repository/org/novelang/Novelang-distribution 
LISTEN_PORT=8083
java -Djava.awt.headless=true -jar ${NOVELANG_HOME}/${NOVELANG_VERSION}/Novelang-distribution-${NOVELANG_VERSION}/lib/Novelang-bootstrap-${NOVELANG_VERSION}.jar httpdaemon --port ${LISTEN_PORT} --log-dir /tmp --temporary-dir /tmp

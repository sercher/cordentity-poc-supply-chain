#!/usr/bin/env bash

docker-compose down --rmi local --volumes
./gradlew killCordaProcesses clean assemble
./gradlew deployNodes
docker-compose up -d agent94 agent95 agent96 agentInitiator
docker-compose build tccorda mfcorda notary tcweb mfweb
docker-compose up -d tccorda mfcorda notary
sleep 60
docker-compose up -d tcweb mfweb

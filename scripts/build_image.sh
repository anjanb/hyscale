#!/bin/bash
set -euo pipefail
project_version=`cat pom.xml | grep "<version>.*</version>" | head -1 |awk -F'[><]' '{print $3}'`
artifactory_version=$project_version.$GITHUB_RUN_NUMBER
echo ::set-env name=IMAGE_VERSION::$(echo $artifactory_version)
echo ::set-env name=PROJECT_VERSION::$(echo $project_version)
#echo ::set-env name=DOCKER_CONTENT_TRUST::1
docker build -t $DOCKER_REPO/hyscale:$artifactory_version .
docker tag $DOCKER_REPO/hyscale:$artifactory_version hyscale:latest

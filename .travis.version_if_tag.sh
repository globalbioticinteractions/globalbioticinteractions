#!/bin/bash
if [ -n "$TRAVIS_TAG" ]; then
  mvn -pl eol-globi-parent versions:set -DnewVersion=$TRAVIS_TAG.$TRAVIS_BUILD_NUMBER
fi

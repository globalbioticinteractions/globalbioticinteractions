#!/bin/bash
if [ -n "$TRAVIS_TAG" ]; then
  VERSION=${TRAVIS_TAG//[^0-9.]/}
  mvn -pl eol-globi-parent versions:set -DnewVersion=$VERSION
fi

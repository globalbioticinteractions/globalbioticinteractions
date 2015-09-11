#!/bin/bash
if [ -n "$TRAVIS_TAG" ]; then
  mvn -s .travis.maven.settings.xml -DskipTests clean deploy
fi

#!/bin/bash
if [ -n "$TRAVIS_TAG" ]; then
  mvn clean deploy --settings .travis.maven.settings.xml -DskipTests
fi

#!/bin/bash
if [ -n "$TRAVIS_TAG" ]; then
  mvn clean deploy -DskipTests scm:tag
fi

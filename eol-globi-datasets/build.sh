#!/bin/bash
#
# compile, links, and packages GloBI data products for elton datasets.
#
# usage:
#   build.sh [elton dataset dir (default: /var/cache/elton/datsets)]
#

ELTON_DATASET_DIR=${1:-/var/cache/elton/datasets}

function run_step {
  mvn --settings /etc/globi/.m2/settings.xml -Ddataset.dir="${ELTON_DATASET_DIR}" -P$1 clean install
}

run_step compile
run_step link
run_step package

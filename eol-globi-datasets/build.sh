#!/bin/bash
#
# compile, links, and packages GloBI data products for elton datasets.
#

ELTON_DATASET_DIR=${1:-/var/cache/elton/datasets}

# compile
mvn --settings /etc/globi/.m2/settings.xml -Ddataset.dir="${ELTON_DATASET_DIR}" -Pcompile clean install

# link
mvn --settings /etc/globi/.m2/settings.xml -Ddataset.dir="${ELTON_DATASET_DIR}" -Plink clean install

# package

mvn --settings /etc/globi/.m2/settings.xml -Ddataset.dir="${ELTON_DATASET_DIR}" -Pexport-all clean install

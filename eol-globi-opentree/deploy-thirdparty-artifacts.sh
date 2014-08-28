# example script to stage tnc / marineregions datasets from http://maps.tnc.org/gis_data.html in local maven repo
SRC_DIR=~/Desktop
TARGET_DIR=~/.m2/repository

#curl http://files.opentreeoflife.org/trees/draftversion1.tre.gz > $SRC_DIR/draftversion1.tre.gz
# tar -czf ot0.1.tgz draftversion1.tre
#curl http://files.opentreeoflife.org/ott/ott2.8.tgz > $SRC_DIR/ott2.8.tgz

mvn deploy:deploy-file -Dfile=$SRC_DIR/ot0.1.tgz -Durl=file:///$TARGET_DIR -DartifactId=opentree-trees -DgroupId=org.eol.globi.opentree -Dversion=0.1 -Dpackaging=tar.gz -DgeneratePom=true -DgeneratePom.description="see http://files.opentreeoflife.org/trees/?"

mvn deploy:deploy-file -Dfile=$SRC_DIR/ott2.8.tgz -Durl=file:///$TARGET_DIR -DartifactId=opentree-taxonomy -DgroupId=org.eol.globi.opentree -Dversion=2.8 -Dpackaging=tar.gz -DgeneratePom=true -DgeneratePom.description="see http://files.opentreeoflife.org/ott/"

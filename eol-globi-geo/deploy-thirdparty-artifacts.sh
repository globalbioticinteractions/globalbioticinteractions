# example script to stage tnc datasets from http://maps.tnc.org/gis_data.html in local maven repo
SRC_DIR=~/Desktop
TARGET_DIR=~/.m2/repository
VERSION=20140115

curl http://maps.tnc.org/files/shp/MEOW-TNC.zip > $SRC_DIR/MEOW-TNC.zip
curl http://maps.tnc.org/files/shp/FEOW-TNC.zip > $SRC_DIR/FEOW-TNC.zip
curl http://maps.tnc.org/files/shp/terr-ecoregions-TNC.zip > $SRC_DIR/terr-ecoregions-TNC.zip

mvn deploy:deploy-file -Dfile=$SRC_DIR/FEOW-TNC.zip -Durl=file:///$TARGET_DIR -DartifactId=feow-tnc -DgroupId=org.eol.globi.geo -Dversion=$VERSION -Dpackaging=zip -DgeneratePom=true -DgeneratePom.description="see http://maps.tnc.org/gis_data.html"
mvn deploy:deploy-file -Dfile=$SRC_DIR/terr-ecoregions-TNC.zip -Durl=file:///$TARGET_DIR -DartifactId=teow-tnc -DgroupId=org.eol.globi.geo -Dversion=$VERSION -Dpackaging=zip -DgeneratePom=true -DgeneratePom.description="see http://maps.tnc.org/gis_data.html"
mvn deploy:deploy-file -Dfile=$SRC_DIR/MEOW-TNC.zip -Durl=file:///$TARGET_DIR -DartifactId=meow-tnc -DgroupId=org.eol.globi.geo -Dversion=$VERSION -Dpackaging=zip -DgeneratePom=true -DgeneratePom.description="see http://maps.tnc.org/gis_data.html"

eol-globi-rest
==============

Standalone server that provides RESTful interface to EOL's Global Biotic Interactions datasets.

# Prequisites
java, maven

# Install / Run 

git clone git://github.com/jhpoelen/eol-globi-rest.git

cd eol-globi-rest

mvn clean install 

java -jar target/dependency/jetty-runner.jar --port 8080 target/*.war

# Want to try it?
See [Our wiki](http://github.com/jhpoelen/eol-globi-rest/wiki) for examples.

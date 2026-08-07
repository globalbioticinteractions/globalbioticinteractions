package org.eol.globi.data;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.DatasetNode;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.EnvironmentNode;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationConstant;
import org.eol.globi.domain.LocationNode;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.Term;
import org.globalbioticinteractions.dataset.Dataset;
import org.globalbioticinteractions.dataset.DatasetConstant;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;

import java.util.List;
import java.util.stream.Collectors;

public class NodeFactoryNeo4j3 extends NodeFactoryNeo4j {

    public NodeFactoryNeo4j3(GraphDatabaseService graphDb) {
        super(graphDb);
    }




}


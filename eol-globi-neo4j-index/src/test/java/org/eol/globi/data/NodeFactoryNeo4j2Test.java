package org.eol.globi.data;

public class NodeFactoryNeo4j2Test extends NodeFactoryNeo4jTest {

    @Override
    protected NodeFactoryNeo4j createNodeFactory() {
        NodeFactoryNeo4j2 nodeFactoryNeo4j = new NodeFactoryNeo4j2(getGraphDb());
        nodeFactoryNeo4j.setEnvoLookupService(getEnvoLookupService());
        nodeFactoryNeo4j.setTermLookupService(getTermLookupService());
        return nodeFactoryNeo4j;
    }


}
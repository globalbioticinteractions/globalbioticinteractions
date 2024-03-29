package org.eol.globi.export;

import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.eol.globi.util.RelationshipListener;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Fun;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExporterAggregateUtil {
    public static void exportDistinctInteractionsByStudy(ExportUtil.Appender writer, GraphDatabaseService graphDatabase, RowWriter rowWriter) throws IOException {
        DB db = DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make();
        final Map<Fun.Tuple3<Long, String, String>, List<String>> studyOccAggregate = db.createTreeMap("studyOccAggregate").make();

        NodeUtil.findStudies(graphDatabase, node -> collectDistinctInteractions(studyOccAggregate, node));

        for (Map.Entry<Fun.Tuple3<Long, String, String>, List<String>> distinctInteractions : studyOccAggregate.entrySet()) {
            rowWriter.writeRow(
                    writer,
                    new StudyNode(graphDatabase.getNodeById(distinctInteractions.getKey().a)),
                    distinctInteractions.getKey().b,
                    distinctInteractions.getKey().c,
                    distinctInteractions.getValue()
            );

        }
        db.close();
    }

    public static void collectDistinctInteractions(final Map<Fun.Tuple3<Long, String, String>, List<String>> studyOccAggregate, final Node studyNode) {
        {

            RelationshipListener handler = new RelationshipListener() {

                @Override
                public void on(Relationship specimen) {
                    final Iterable<Relationship> interactions = specimen.getEndNode().getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j());
                    for (Relationship interaction : interactions) {
                        if (!interaction.hasProperty(PropertyAndValueDictionary.INVERTED)) {
                            final Node targetSpecimen = interaction.getEndNode();
                            final Node sourceSpecimen = interaction.getStartNode();
                            final String sourceTaxonExternalId = getExternalIdForTaxonOf(sourceSpecimen);
                            final String targetTaxonExternalId = getExternalIdForTaxonOf(targetSpecimen);
                            if (sourceTaxonExternalId != null && targetTaxonExternalId != null) {
                                final Fun.Tuple3<Long, String, String> key = new Fun.Tuple3<Long, String, String>(studyNode.getId(), sourceTaxonExternalId, interaction.getType().name());
                                List<String> targetTaxonExternalIds = studyOccAggregate.get(key);
                                if (targetTaxonExternalIds == null) {
                                    targetTaxonExternalIds = new ArrayList<>();
                                }
                                if (!targetTaxonExternalIds.contains(targetTaxonExternalId)) {
                                    targetTaxonExternalIds.add(targetTaxonExternalId);
                                }
                                studyOccAggregate.put(key, targetTaxonExternalIds);
                            }
                        }
                    }

                }
            };

            NodeUtil.handleCollectedRelationships(new NodeTypeDirection(studyNode), handler);
        }
    }

    public static String getExternalIdForTaxonOf(Node targetSpecimen) {
        final Iterable<Relationship> classifiedAs = targetSpecimen.getRelationships(Direction.OUTGOING, NodeUtil.asNeo4j(RelTypes.CLASSIFIED_AS));
        for (Relationship classifiedA : classifiedAs) {
            final TaxonNode taxonNode = new TaxonNode(classifiedA.getEndNode());
            if (!StringUtils.equals(taxonNode.getExternalId(), PropertyAndValueDictionary.NO_MATCH)
                    && !StringUtils.equals(taxonNode.getName(), PropertyAndValueDictionary.NO_MATCH)
                    && !StringUtils.equals(taxonNode.getName(), PropertyAndValueDictionary.NO_NAME)
            ) {
                return taxonNode.getExternalId();
            }
        }
        return null;
    }

    public interface RowWriter {
        void writeRow(ExportUtil.Appender writer, StudyNode studyId, String sourceTaxonId, String relationshipType, List<String> targetTaxonIds) throws IOException;
    }
}

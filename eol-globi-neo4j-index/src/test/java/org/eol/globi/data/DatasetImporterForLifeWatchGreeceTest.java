package org.eol.globi.data;


import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.SpecimenNode;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.domain.TaxonNode;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.NodeTypeDirection;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForLifeWatchGreeceTest extends GraphDBTestCase {

    @Test
    public void importCoupleOfLines() throws StudyImporterException {
        DatasetImporterForLifeWatchGreece importer = new DatasetImporterForLifeWatchGreece(new TestParserFactory("122,\"Fauchald, K., Jumars, P.A. (1979) The Diet of Worms: A StudyNode of Polychaete Feeding Guilds. <i>Oceanography and Marine Biology: Annual Review</i>, 17:193-284.\",\"Schistomeringos rudolphii\",\"http://www.owl-ontologies.com/unnamed.owl#Algae\",\"Algae as food source.\",\"http://eol.org/schema/terms/preysUpon\",\"The type of food an organism prefers.\",1,1\n" +
                "429,\"Simpson, M. (1962) Reproduction of the Polychaete Glycera Dibranchiata at Solomons, Maryland. <i>The Biological Bulletin</i>, 123:396-411.\",\"Glycera alba\",\"http://polytraits.lifewatchgreece.eu/terms/EPKY_YES\",\"The organism undergoes epitokous metamorphosis.\",\"http://polytraits.lifewatchgreece.eu/terms/EPKY\",\"Form of reproduction of marine polychates in which the new individual arises by modification and separation from the posterior end of the worm in order to leave the bottom and reproduce [1292].\",2,1\n" +
                "543,\"Rouse, G.W., Pleijel, F. (2001) Polychaetes. Oxford University Press,Oxford.354pp.\",\"Glycera alba\",\"http://polytraits.lifewatchgreece.eu/terms/EPKY_YES\",\"The organism undergoes epitokous metamorphosis.\",\"http://polytraits.lifewatchgreece.eu/terms/EPKY\",\"Form of reproduction of marine polychates in which the new individual arises by modification and separation from the posterior end of the worm in order to leave the bottom and reproduce [1292].\",2,1\n" +
                "429,\"Simpson, M. (1962) Reproduction of the Polychaete Glycera Dibranchiata at Solomons, Maryland. <i>The Biological Bulletin</i>, 123:396-411.\",\"Glycera rouxi\",\"http://polytraits.lifewatchgreece.eu/terms/SM_YES\",\"Organisms that undergo sexual metamorphosis\",\"http://polytraits.lifewatchgreece.eu/terms/SM\",\"Conspicuous change in the organism's body structure prior to reproduction.\",2,1\n" +
                "778,\"Hartmann-Schröder, G. (1996) Annelida, Borstenwürmer, Polychaeta. Gustav Fischer Verlag, Jena. 648pp.\",\"Glycera rouxi\",\"http://polytraits.lifewatchgreece.eu/terms/SM_YES\",\"Organisms that undergo sexual metamorphosis\",\"http://polytraits.lifewatchgreece.eu/terms/SM\",\"Conspicuous change in the organism's body structure prior to reproduction.\",2,1\n" +
                "429,\"Simpson, M. (1962) Reproduction of the Polychaete Glycera Dibranchiata at Solomons, Maryland. <i>The Biological Bulletin</i>, 123:396-411.\",\"Glycera tesselata\",\"http://polytraits.lifewatchgreece.eu/terms/SM_YES\",\"Organisms that undergo sexual metamorphosis\",\"http://polytraits.lifewatchgreece.eu/terms/SM\",\"Conspicuous change in the organism's body structure prior to reproduction.\",2,1\n"), nodeFactory);
        importer.setDataset(new DatasetLocal(inStream -> inStream));
        importStudy(importer);
    }


    @Test
    public void readAssociations() throws StudyImporterException {
        List<StudyNode> studies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(studies.size(), is(0));

        ParserFactoryLocal parserFactory = new ParserFactoryLocal();
        DatasetImporterForLifeWatchGreece importer = new DatasetImporterForLifeWatchGreece(parserFactory, nodeFactory);
        importer.setDataset(new DatasetLocal(inStream -> inStream));
        importStudy(importer);

        studies = NodeUtil.findAllStudies(getGraphDb());
        assertThat(studies.size(), is(146));

        Set<String> taxa = new HashSet<String>();

        final AtomicInteger totalPredatorPreyRelationships = new AtomicInteger(0);

        for (StudyNode study : studies) {
            assertThat(study.getCitation(), is(notNullValue()));
            assertThat(study.getTitle(), containsString("greece"));
            NodeUtil.RelationshipListener handler = relationship -> {
                addTaxonNameForSpecimenNode(taxa, relationship.getEndNode());
                Specimen predatorSpecimen = new SpecimenNode(relationship.getEndNode());
                Iterable<Relationship> prey = NodeUtil.getStomachContents(predatorSpecimen);
                for (Relationship ateRel : prey) {
                    totalPredatorPreyRelationships.incrementAndGet();
                    addTaxonNameForSpecimenNode(taxa, ateRel.getEndNode());
                }
            };

            NodeUtil.handleCollectedRelationships(new NodeTypeDirection(study.getUnderlyingNode()), handler);
        }
        assertThat(taxa.contains("Aves"), is(true));
        assertThat(totalPredatorPreyRelationships.get(), is(793));
    }

    private void addTaxonNameForSpecimenNode(Set<String> taxa, Node startNode) {
        Specimen predatorSpecimen = new SpecimenNode(startNode);
        Iterable<Relationship> classifications = NodeUtil.getClassifications(predatorSpecimen);
        for (Relationship classification : classifications) {
            taxa.add(new TaxonNode(classification.getEndNode()).getName());
        }
    }


}
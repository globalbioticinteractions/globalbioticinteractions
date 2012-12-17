package org.trophic.graph.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.trophic.graph.domain.Specimen;
import org.trophic.graph.domain.Study;
import org.trophic.graph.domain.Taxon;

import java.io.IOException;

public class StudyImporterForJRFerrerParis extends BaseStudyImporter {

    public StudyImporterForJRFerrerParis(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.createStudy("JRFerrisParisButterflies");
        String studyResource = "/org/trophic/graph/data/jr_ferrer_paris/ExampleAssociations.csv";
        try {
            LabeledCSVParser parser = parserFactory.createParser(studyResource);

            while (parser.getLine() != null) {
                Taxon instigatorTaxon = createTaxon(parser, "Lepidoptera Name");
                Specimen instigatorSpecimen = nodeFactory.createSpecimen();
                instigatorSpecimen.classifyAs(instigatorTaxon);
                study.collected(instigatorSpecimen);

                Taxon targetTaxon = createTaxon(parser, "Hostplant Name");
                Specimen targetSpecimen = nodeFactory.createSpecimen();
                study.collected(targetSpecimen);
                instigatorSpecimen.classifyAs(targetTaxon);
                instigatorSpecimen.ate(targetSpecimen);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to access resource [" + studyResource + "]");
        }

        return study;
    }

    private Taxon createTaxon(LabeledCSVParser parser, String taxonLabel) throws StudyImporterException {
        String instigatorScientificName = parser.getValueByLabel(taxonLabel);
        if (StringUtils.isBlank(instigatorScientificName)) {
            throw new StudyImporterException("found missing instigator scientific name at line [" + parser.getLastLineNumber() + "]");
        } else {
            try {
                return nodeFactory.getOrCreateTaxon(StringUtils.trim(instigatorScientificName));
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to find/create taxon with name [" + instigatorScientificName + "]");
            }
        }
    }
}

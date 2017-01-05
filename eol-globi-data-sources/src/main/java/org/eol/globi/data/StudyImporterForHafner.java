package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;

import java.io.IOException;

public class StudyImporterForHafner extends BaseStudyImporter {

    public static final String RESOURCE = "hafner/gopher_lice_int.csv";

    public StudyImporterForHafner(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            LabeledCSVParser parser = parserFactory.createParser(RESOURCE, "UTF-8");
            while (parser.getLine() != null) {
                String sourceCitation = "Mark S. Hafner, Philip D. Sudman, Francis X. Villablanca, Theresa A. Spradling, James W. Demastes, Steven A. Nadler. (1994). Disparate Rates of Molecular Evolution in Cospeciating Hosts and Parasites. Science 265: 1087-1090. doi:10.1126/science.8066445";
                Study study = nodeFactory.getOrCreateStudy(new StudyImpl("hafner1994", "Shan Kothari, Pers. Comm. 2014.", "doi:10.1126/science.8066445", sourceCitation));

                String hostName = parser.getValueByLabel("Host");
                String parasiteName = parser.getValueByLabel("Parasite");
                Specimen host = nodeFactory.createSpecimen(study, new TaxonImpl(hostName, null));
                Specimen parasite = nodeFactory.createSpecimen(study, new TaxonImpl(parasiteName, null));
                parasite.interactsWith(host, InteractType.PARASITE_OF);
            }
        } catch (IOException | NodeFactoryException e) {
            throw new StudyImporterException("failed to import [" + RESOURCE + "]", e);
        }
    }

}

package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Term;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class StudyImporterForHafner extends BaseStudyImporter {

    public static final String RESOURCE = "hafner/gopher_lice_int.csv";

    public StudyImporterForHafner(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            LabeledCSVParser parser = parserFactory.createParser(RESOURCE, "UTF-8");
            while (parser.getLine() != null) {
                String sourceCitation = "Mark S. Hafner, Philip D. Sudman, Francis X. Villablanca, Theresa A. Spradling, James W. Demastes, Steven A. Nadler. (1994). Disparate Rates of Molecular Evolution in Cospeciating Hosts and Parasites. Science 265: 1087-1090.";
                Study study = nodeFactory.getOrCreateStudy("hafner1994", null, null, null, sourceCitation, null, "Shan Kothari, Pers. Comm. 2014.", null);
                study.setCitationWithTx(sourceCitation);

                String hostName = parser.getValueByLabel("Host");
                String parasiteName = parser.getValueByLabel("Parasite");
                Specimen host = nodeFactory.createSpecimen(hostName);
                Specimen parasite = nodeFactory.createSpecimen(parasiteName);
                parasite.interactsWith(host, InteractType.PARASITE_OF);
                study.collected(parasite);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to import [" + RESOURCE + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import [" + RESOURCE + "]", e);
        }

        return null;
    }

}

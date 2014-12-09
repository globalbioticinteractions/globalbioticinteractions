package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class StudyImporterForGemina extends BaseStudyImporter {

    public static final String RESOURCE = "http://sourceforge.net/projects/gemina/files/gemina%20database/gemina_complete_infection_dump_2008-01-03/gemina_search_2008-01-03.txt/download";

    public StudyImporterForGemina(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        try {
            String source = "Schriml, L. M., Arze, C., Nadendla, S., Ganapathy, A., Felix, V., Mahurkar, A., … Hall, N. (2009). GeMInA, Genomic Metadata for Infectious Agents, a geospatial surveillance pathogen database. Nucleic Acids Research, 38(Database), D754–D764. doi:10.1093/nar/gkp832";
            Study study = nodeFactory.getOrCreateStudy(source, source, "doi:10.1093/nar/gkp832");
            study.setCitationWithTx(source);

            LabeledCSVParser parser = parserFactory.createParser(RESOURCE, "UTF-8");
            parser.changeDelimiter('\t');
            String line[];
            while ((line = parser.getLine()) != null) {
                if (line.length > 7) {
                    String pathogenId = parser.getValueByLabel("Pathogen Taxonomy");
                    String pathogenExternalId = StringUtils.isBlank(pathogenId) ? null : TaxonomyProvider.NCBI.getIdPrefix() + pathogenId;
                    Specimen pathogen = nodeFactory.createSpecimen(parser.getValueByLabel("Pathogen"), pathogenExternalId);
                    String hostId = line[7];
                    String hostReservoirExternalId = StringUtils.isBlank(hostId) ? null : TaxonomyProvider.NCBI.getIdPrefix() + hostId;
                    Specimen host = nodeFactory.createSpecimen(parser.getValueByLabel("Host/Reservoir"), hostReservoirExternalId);
                    pathogen.interactsWith(host, InteractType.PATHOGEN_OF);
                    study.collected(pathogen);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to import [" + RESOURCE + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import [" + RESOURCE + "]", e);
        }

        return null;
    }

}

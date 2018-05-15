package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForKelpForest extends BaseStudyImporter {

    private static final Log LOG = LogFactory.getLog(StudyImporterForKelpForest.class);

    public static final String NODES = "http://kelpforest.ucsc.edu/exports/ExportNodesWithFunctionalGroupsCsv.php";
    public static final String LINKS = "http://kelpforest.ucsc.edu/exports/exportCitedLinks2csv.php";

    public StudyImporterForKelpForest(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            String source = "Beas-Luna, R., Novak, M., Carr, M. H., Tinker, M. T., Black, A., Caselle, J. E., … Iles, A. (2014). An Online Database for Informing Ecological Network Models: http://kelpforest.ucsc.edu. PLoS ONE, 9(10), e109356. doi:10.1371/journal.pone.0109356";
            Study study = nodeFactory.getOrCreateStudy(new StudyImpl(source, source, "doi:10.1371/journal.pone.0109356", source));
            LabeledCSVParser parser = parserFactory.createParser(NODES, "UTF-8");
            String line[];
            Map<String, Long> nameToId = new HashMap<String, Long>();
            while ((line = parser.getLine()) != null) {
                if (line.length > 2) {
                    String name = parser.getValueByLabel("working_name");
                    String itisId = parser.getValueByLabel("itis_id");
                    Long id = StringUtils.isBlank(itisId) ? null : Long.parseLong(itisId);
                    id = (id != null && id > 0L) ? id : null;
                    nameToId.put(name, id);
                }
            }

            Map<String, InteractType> typeToType = new HashMap<String, InteractType>() {
                {
                    put("trophic", InteractType.ATE);
                    put("parasitic", InteractType.PARASITE_OF);
                }
            };

            parser = parserFactory.createParser(LINKS, "UTF-8");
            while ((line = parser.getLine()) != null) {
                if (line.length > 2) {
                    String interactionType = parser.getValueByLabel("type");
                    InteractType interactType = typeToType.get(interactionType);
                    if (null == interactType) {
                        LOG.warn("ignoring type [" + interactionType + "] on line: [" + (parser.getLastLineNumber() + 1) + "]");
                    } else {
                        Specimen sourceSpecimen = createSpecimen(parser, nameToId, "node_1_working_name", "node1_stage", study);
                        Specimen targetSpecimen = createSpecimen(parser, nameToId, "node_2_working_name", "node2_stage", study);
                        sourceSpecimen.interactsWith(targetSpecimen, interactType);
                    }
                }
            }
        } catch (IOException | NodeFactoryException e) {
            throw new StudyImporterException("failed to import", e);
        }
    }

    protected Specimen createSpecimen(LabeledCSVParser parser, Map<String, Long> nameToId, String nameLabel, String stageLabel, Study study) throws NodeFactoryException {
        String sourceName = parser.getValueByLabel(nameLabel);
        Long id = nameToId.get(sourceName);
        String taxonExternalId = id == null ? null : TaxonomyProvider.ID_PREFIX_ITIS + id;
        Specimen sourceSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl(sourceName, taxonExternalId));
        String sourceLifeStage = parser.getValueByLabel(stageLabel);
        Term orCreateLifeStage = nodeFactory.getOrCreateLifeStage("KELP:" + sourceLifeStage, sourceLifeStage);
        sourceSpecimen.setLifeStage(orCreateLifeStage);
        return sourceSpecimen;
    }

}

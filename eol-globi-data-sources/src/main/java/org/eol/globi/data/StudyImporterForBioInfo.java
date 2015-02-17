package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupServiceException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForBioInfo extends BaseStudyImporter implements StudyImporter {
    public static final String RELATIONS_DATA_FILE = "bioinfo.org.uk/eol_taxon_relations.csv.gz";
    public static final String BIOINFO_URL = "http://bioinfo.org.uk";
    public static final Map<String, InteractType> INTERACTION_MAPPING = Collections.unmodifiableMap(new HashMap<String, InteractType>() {
        {
            put("Animal / associate", InteractType.INTERACTS_WITH);
            put("Animal / carrion / dead animal feeder", InteractType.INTERACTS_WITH);
            put("Animal / dung / associate", InteractType.INTERACTS_WITH);
            put("Animal / dung / debris feeder", InteractType.INTERACTS_WITH);
            put("Animal / dung / oviposition", InteractType.INTERACTS_WITH);
            put("Animal / dung / saprobe", InteractType.INTERACTS_WITH);
            put("Animal / endozoite", InteractType.INTERACTS_WITH);
            put("Animal / epizoite", InteractType.INTERACTS_WITH);
            put("Animal / gamete vector / crsss fertilises", InteractType.INTERACTS_WITH);
            put("Animal / guest", InteractType.INTERACTS_WITH);
            put("Animal / honeydew feeder", InteractType.INTERACTS_WITH);
            put("Animal / inquiline", InteractType.INTERACTS_WITH);
            put("Animal / kill", InteractType.INTERACTS_WITH);
            put("Animal / kleptoparasite", InteractType.PARASITE_OF);
            put("Animal / parasite / ectoparasite / blood sucker", InteractType.PARASITE_OF);
            put("Animal / parasite / ectoparasite / sweat sucker", InteractType.PARASITE_OF);
            put("Animal / parasite / ectoparasite / tear sucker", InteractType.PARASITE_OF);
            put("Animal / parasite / ectoparasite", InteractType.PARASITE_OF);
            put("Animal / parasite / endoparasite", InteractType.PARASITE_OF);
            put("Animal / parasite", InteractType.PARASITE_OF);
            put("Animal / parasitoid / ectoparasitoid", InteractType.PARASITE_OF);
            put("Animal / parasitoid / endoparasitoid", InteractType.PARASITE_OF);
            put("Animal / parasitoid", InteractType.PARASITE_OF);
            put("Animal / pathogen", InteractType.PATHOGEN_OF);
            put("Animal / phoresy", InteractType.INTERACTS_WITH);
            put("Animal / predator / stocks nest with", InteractType.PREYS_UPON);
            put("Animal / predator", InteractType.PREYS_UPON);
            put("Animal / resting place / on", InteractType.INTERACTS_WITH);
            put("Animal / resting place / under", InteractType.INTERACTS_WITH);
            put("Animal / resting place / within", InteractType.INTERACTS_WITH);
            put("Animal / sequestrates", InteractType.INTERACTS_WITH);
            put("Animal / slave maker", InteractType.INTERACTS_WITH);
            put("Animal / vector", InteractType.HAS_VECTOR);
            put("Bacterium / farmer", InteractType.INTERACTS_WITH);
            put("Bacterium / predator", InteractType.PREYS_UPON);
            put("Foodplant / collects", InteractType.INTERACTS_WITH);
            put("Foodplant / debris feeder", InteractType.ATE);
            put("Foodplant / endophyte", InteractType.INTERACTS_WITH);
            put("Foodplant / false gall", InteractType.INTERACTS_WITH);
            put("Foodplant / feeds on", InteractType.ATE);
            put("Foodplant / gall", InteractType.INTERACTS_WITH);
            put("Foodplant / hemiparasite", InteractType.PARASITE_OF);
            put("Foodplant / immobile silken tube feeder", InteractType.INTERACTS_WITH);
            put("Foodplant / internal feeder", InteractType.INTERACTS_WITH);
            put("Foodplant / miner", InteractType.INTERACTS_WITH);
            put("Foodplant / mobile cased feeder", InteractType.INTERACTS_WITH);
            put("Foodplant / mutualist", InteractType.SYMBIONT_OF);
            put("Foodplant / mycorrhiza / ectomycorrhiza", InteractType.SYMBIONT_OF);
            put("Foodplant / mycorrhiza / endomycorrhiza", InteractType.SYMBIONT_OF);
            put("Foodplant / mycorrhiza", InteractType.SYMBIONT_OF);
            put("Foodplant / nest", InteractType.INTERACTS_WITH);
            put("Foodplant / open feeder", InteractType.INTERACTS_WITH);
            put("Foodplant / parasite", InteractType.PARASITE_OF);
            put("Foodplant / pathogen", InteractType.PATHOGEN_OF);
            put("Foodplant / robber", InteractType.INTERACTS_WITH);
            put("Foodplant / roller", InteractType.INTERACTS_WITH);
            put("Foodplant / sap sucker", InteractType.INTERACTS_WITH);
            put("Foodplant / saprobe", InteractType.INTERACTS_WITH);
            put("Foodplant / secondary infection", InteractType.INTERACTS_WITH);
            put("Foodplant / shot hole causer", InteractType.INTERACTS_WITH);
            put("Foodplant / spinner", InteractType.INTERACTS_WITH);
            put("Foodplant / spot causer", InteractType.INTERACTS_WITH);
            put("Foodplant / visitor / nectar", InteractType.INTERACTS_WITH);
            put("Foodplant / visitor", InteractType.INTERACTS_WITH);
            put("Foodplant / web feeder", InteractType.INTERACTS_WITH);
            put("Fungus / associate", InteractType.INTERACTS_WITH);
            put("Fungus / external feeder", InteractType.INTERACTS_WITH);
            put("Fungus / feeder", InteractType.INTERACTS_WITH);
            put("Fungus / gall", InteractType.INTERACTS_WITH);
            put("Fungus / infection vector", InteractType.INTERACTS_WITH);
            put("Fungus / internal feeder", InteractType.INTERACTS_WITH);
            put("Fungus / nest", InteractType.INTERACTS_WITH);
            put("Fungus / parasite / endoparasite", InteractType.PARASITE_OF);
            put("Fungus / parasite", InteractType.PARASITE_OF);
            put("Fungus / resting place / on", InteractType.INTERACTS_WITH);
            put("Fungus / resting place / within", InteractType.INTERACTS_WITH);
            put("Fungus / saprobe", InteractType.INTERACTS_WITH);
            put("Inhibits or restricts the growth of", InteractType.INTERACTS_WITH);
            put("Lichen / associate", InteractType.INTERACTS_WITH);
            put("Lichen / gall", InteractType.INTERACTS_WITH);
            put("Lichen / grows on or over", InteractType.INTERACTS_WITH);
            put("Lichen / kleptoparasite", InteractType.PARASITE_OF);
            put("Lichen / nest", InteractType.INTERACTS_WITH);
            put("Lichen / parasite", InteractType.PARASITE_OF);
            put("Lichen / pathogen", InteractType.PATHOGEN_OF);
            put("Lichen / photobiont", InteractType.INTERACTS_WITH);
            put("Lichen / saprobe", InteractType.INTERACTS_WITH);
            put("Lichen / sequestrate", InteractType.INTERACTS_WITH);
            put("Lichen / symbiont", InteractType.SYMBIONT_OF);
            put("Plant / associate", InteractType.INTERACTS_WITH);
            put("Plant / epiphyte", InteractType.INTERACTS_WITH);
            put("Plant / grows among", InteractType.INTERACTS_WITH);
            put("Plant / grows inside", InteractType.INTERACTS_WITH);
            put("Plant / hibernates / on", InteractType.INTERACTS_WITH);
            put("Plant / hibernates / under", InteractType.INTERACTS_WITH);
            put("Plant / hibernates / within", InteractType.INTERACTS_WITH);
            put("Plant / nest", InteractType.INTERACTS_WITH);
            put("Plant / pollenated", InteractType.POLLINATES);
            put("Plant / resting place / among", InteractType.INTERACTS_WITH);
            put("Plant / resting place / on", InteractType.INTERACTS_WITH);
            put("Plant / resting place / under", InteractType.INTERACTS_WITH);
            put("Plant / resting place / within", InteractType.INTERACTS_WITH);
            put("Plant / vector", InteractType.HAS_VECTOR);
        }
    });

    public StudyImporterForBioInfo(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = createStudy();

        LabeledCSVParser relationsParser;
        try {
            relationsParser = parserFactory.createParser(RELATIONS_DATA_FILE, CharsetConstant.UTF8);
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading trophic relations file [" + RELATIONS_DATA_FILE + "]", e1);
        }
        return createRelations(relationsParser, study);
    }

    protected Study createStudy() {
        String citation = "Food Webs and Species Interactions in the Biodiversity of UK and Ireland (Online). 2013. Data provided by Malcolm Storey. Also available from " + BIOINFO_URL + ".";
        Study study = nodeFactory.getOrCreateStudy("BIO_INFO",
                "Malcolm Storey",
                BIOINFO_URL,
                "",
                "Food webs and species interactions in the Biodiversity of UK and Ireland.", null, citation);
        study.setCitationWithTx(citation);
        study.setExternalId(BIOINFO_URL);
        return study;
    }

    protected Study createRelations(LabeledCSVParser parser, Study study) throws StudyImporterException {
        getLogger().info(study, "relations being created...");
        try {
            long count = 1;
            while (parser.getLine() != null) {
                if (importFilter.shouldImportRecord(count)) {
                    String relationship = parser.getValueByLabel("relationship");
                    if (StringUtils.isBlank(relationship)) {
                        getLogger().warn(study, "no relationship for record on line [" + (parser.lastLineNumber() + 1) + "]");
                    }
                    InteractType interactType = INTERACTION_MAPPING.get(relationship);
                    if (null == interactType) {
                        getLogger().warn(study, "no mapping found for relationship [" + relationship + "] for record on line [" + (parser.lastLineNumber() + 1) + "]");
                    } else {
                        importInteraction(parser, study, interactType);
                    }
                }
                count++;
            }
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading trophic relations data", e1);
        }
        getLogger().info(study, "relations created.");
        return study;
    }

    private void importInteraction(LabeledCSVParser parser, Study study, InteractType interactType) throws StudyImporterException {
        String passiveId = parser.getValueByLabel("passive NBN Code");
        String activeId = parser.getValueByLabel("active NBN Code");
        if (StringUtils.isNotBlank(passiveId) && StringUtils.isNotBlank(activeId)) {
            Specimen donorSpecimen = createSpecimen(study, parser, passiveId);
            addLifeStage(parser, donorSpecimen, "DonorStage", study);
            Specimen recipientSpecimen = createSpecimen(study, parser, activeId);
            addLifeStage(parser, recipientSpecimen, "RecipStage", study);
            recipientSpecimen.interactsWith(donorSpecimen, interactType);
        }
    }

    private void addLifeStage(LabeledCSVParser parser, Specimen donorSpecimen, String stageColumnName, Study study) throws StudyImporterException {
        List<Term> lifeStage = null;
        String donorLifeStage = parser.getValueByLabel(stageColumnName);
        if (donorLifeStage != null && donorLifeStage.trim().length() > 0) {
            lifeStage = parseLifeStage(donorLifeStage, study);
            if (lifeStage == null) {
                throw new StudyImporterException("failed to map stage [" + donorLifeStage + "] on line [" + parser.getLastLineNumber() + "]");
            }
        }
        donorSpecimen.setLifeStage(lifeStage);
    }

    private List<Term> parseLifeStage(String lifeStageString, Study study) throws StudyImporterException {
        try {
            List<Term> terms = nodeFactory.getTermLookupService().lookupTermByName(lifeStageString);
            if (terms.size() > 0) {
            } else {
                getLogger().warn(study, "failed to map life stage [" + lifeStageString + "]");
            }
            return terms;
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException("failed to lookup lifestage [" + lifeStageString + "]");
        }

    }

    private Specimen createSpecimen(Study study, LabeledCSVParser labeledCSVParser, String externalId) throws StudyImporterException {
        try {
            Specimen specimen = nodeFactory.createSpecimen(study, null, TaxonomyProvider.NBN.getIdPrefix() + externalId);
            specimen.setExternalId(TaxonomyProvider.BIO_INFO + "rel:" + labeledCSVParser.lastLineNumber());
            return specimen;
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create taxon with scientific name [" + externalId + "]", e);
        }
    }

}

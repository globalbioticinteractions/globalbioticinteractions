package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupService;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.service.UberonLookupService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForBioInfo extends BaseStudyImporter implements StudyImporter {
    public static final String TAXA_DATA_FILE = "bioinfo.org.uk/Taxa.txt.gz";
    public static final String RELATION_TYPE_DATA_FILE = "bioinfo.org.uk/TrophicRelations.txt.gz";
    public static final String RELATIONS_DATA_FILE = "bioinfo.org.uk/Relations.txt.gz";

    public TermLookupService termLookupService = new UberonLookupService();

    public static final String BIOINFO_URL = "http://bioinfo.org.uk";

    public StudyImporterForBioInfo(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        Study study = nodeFactory.getOrCreateStudy("BIO_INFO",
                "Malcolm Storey",
                BIOINFO_URL,
                "",
                "Food webs and species interactions in the Biodiversity of UK and Ireland.", null, "http://bioinfo.org.uk");
        study.setCitation("Storey M. Food Webs and Species Interactions in the Biodiversity of UK and Ireland (Online). 2013. Available from " + BIOINFO_URL + ".");
        study.setExternalId(BIOINFO_URL);

        LabeledCSVParser relationsParser;
        try {
            relationsParser = parserFactory.createParser(RELATIONS_DATA_FILE, CharsetConstant.UTF8);
            relationsParser.changeDelimiter('\t');
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading trophic relations file [" + RELATIONS_DATA_FILE + "]", e1);
        }
        return createRelations(createTaxa(study), createRelationTypes(study), relationsParser, study);
    }

    private Map<Long, RelType> createRelationTypes(Study study) throws StudyImporterException {
        getLogger().info(study, "relationTypes being created...");

        Map<Long, RelType> relationsTypeMap;
        try {
            LabeledCSVParser relationTypesParser = parserFactory.createParser(RELATION_TYPE_DATA_FILE, CharsetConstant.UTF8);
            relationTypesParser.changeDelimiter('\t');
            relationsTypeMap = createRelationsTypeMap(relationTypesParser);
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading trophic relations data [" + RELATION_TYPE_DATA_FILE + "", e1);
        }
        getLogger().info(study, "relationTypes created.");
        return relationsTypeMap;
    }

    private Map<Long, String> createTaxa(Study study) throws StudyImporterException {
        getLogger().info(study, "taxa map being created...");
        Map<Long, String> taxaMap;
        try {
            LabeledCSVParser taxaParser = parserFactory.createParser(TAXA_DATA_FILE, CharsetConstant.UTF8);
            taxaParser.changeDelimiter('\t');
            taxaMap = createTaxaMap(taxaParser);
        } catch (IOException e) {
            throw new StudyImporterException("failed to parse taxa file [" + TAXA_DATA_FILE + "]", e);
        }
        getLogger().info(study, "taxa map created.");
        return taxaMap;
    }

    protected Map<Long, String> createTaxaMap(LabeledCSVParser taxaParser) throws StudyImporterException {
        Map<Long, String> taxaMap = new HashMap<Long, String>();

        try {
            while (taxaParser.getLine() != null) {
                Long taxonId = labelAsLong(taxaParser, "Taxon_id");
                if (taxonId == null) {
                    throw new StudyImporterException("failed to parse taxa at line [" + taxaParser.getLastLineNumber() + "]");
                }
                String taxonScientificName = taxaParser.getValueByLabel("Latin80");
                if (taxonScientificName == null || taxonScientificName.trim().length() == 0) {
                    throw new StudyImporterException("found missing or empty scientific taxa name at line [" + taxaParser.getLastLineNumber() + "]");
                }
                taxaMap.put(taxonId, taxonScientificName);
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to parse taxa file");
        }
        return taxaMap;
    }

    protected Map<Long, RelType> createRelationsTypeMap(LabeledCSVParser labeledCSVParser) throws StudyImporterException {
        // Attempt to map Malcolms interactions to http://vocabularies.gbif.org/vocabularies/Interaction
        Map<String, RelType> interactionMapping = new HashMap<String, RelType>();
        interactionMapping.put("ectoparasitises", InteractType.PARASITE_OF);
        interactionMapping.put("endoparasitises", InteractType.PARASITE_OF);
        interactionMapping.put("parasitises", InteractType.PARASITE_OF);
        interactionMapping.put("is ectoparasitoid of", InteractType.PARASITE_OF);
        interactionMapping.put("is parasitoid of", InteractType.PARASITE_OF);
        interactionMapping.put("parasitises", InteractType.PARASITE_OF);
        interactionMapping.put("pollenates or fertilises", InteractType.POLLINATES);
        interactionMapping.put("feeds on dead", InteractType.ATE);
        interactionMapping.put("grazes on", InteractType.ATE);
        interactionMapping.put("feeds on", InteractType.ATE);
        interactionMapping.put("is predator of", InteractType.PREYS_UPON);
        interactionMapping.put("is ectomycorrhizal with", InteractType.INTERACTS_WITH);

        Map<Long, RelType> relationsTypeMap = new HashMap<Long, RelType>();
        try {
            while (labeledCSVParser.getLine() != null) {
                Long trophicRelationId = labelAsLong(labeledCSVParser, "TrophicRel_id");
                String descriptionEnergyRecipient = labeledCSVParser.getValueByLabel("EnergyRecipient");
                RelType relType = interactionMapping.get(descriptionEnergyRecipient);
                if (trophicRelationId != null) {
                    relType = relType == null ? InteractType.INTERACTS_WITH : relType;
                    relationsTypeMap.put(trophicRelationId, relType);
                }
            }
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading the trophic relations data", e1);
        }
        return relationsTypeMap;
    }

    protected Study createRelations(Map<Long, String> taxaMap, Map<Long, RelType> relationsTypeMap, LabeledCSVParser parser, Study study) throws StudyImporterException {
        getLogger().info(study, "relations being created...");
        try {
            long count = 1;
            while (parser.getLine() != null) {
                if (importFilter.shouldImportRecord(count)) {
                    Specimen donorSpecimen = createSpecimen(parser, taxaMap, "DonorTax_id");
                    addLifeStage(parser, donorSpecimen, "DonorStage", study);
                    Specimen recipientSpecimen = createSpecimen(parser, taxaMap, "RecipTax_id");
                    addLifeStage(parser, recipientSpecimen, "RecipStage", study);
                    study.collected(recipientSpecimen);
                    recipientSpecimen.interactsWith(donorSpecimen, relationsTypeMap.get(labelAsLong(parser, "TrophicRel_Id")));
                }
                count++;
            }
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading trophic relations data", e1);
        }
        getLogger().info(study, "relations created.");
        return study;
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
            List<Term> terms = termLookupService.lookupTermByName(lifeStageString);
            if (terms.size() > 0) {
            } else {
                getLogger().warn(study, "failed to map life stage [" + lifeStageString + "]");
            }
            return terms;
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException("failed to lookup lifestage [" + lifeStageString + "]");
        }

    }

    private Specimen createSpecimen(LabeledCSVParser labeledCSVParser, Map<Long, String> taxaMap, String taxonIdString) throws StudyImporterException {
        Long taxonId = labelAsLong(labeledCSVParser, taxonIdString);
        String scientificName = taxaMap.get(taxonId);
        if (scientificName == null) {
            throw new StudyImporterException("failed to find scientific name for taxonId [" + taxonId + "] at line [" + labeledCSVParser.getLastLineNumber() + "]");
        }
        try {
            Specimen specimen = nodeFactory.createSpecimen(scientificName, TaxonomyProvider.BIO_INFO + taxonId);
            specimen.setExternalId(TaxonomyProvider.BIO_INFO + "rel:" + labeledCSVParser.lastLineNumber());
            return specimen;
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create taxon with scientific name [" + scientificName + "]", e);
        }
    }

    private Long labelAsLong(LabeledCSVParser labeledCSVParser, String trophicRelId2) {
        String valueByLabel = labeledCSVParser.getValueByLabel(trophicRelId2);
        Long trophicRelationId = null;
        try {
            trophicRelationId = Long.parseLong(valueByLabel);
        } catch (NumberFormatException ex) {

        }
        return trophicRelationId;
    }
}

package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.RelType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForBioInfo extends BaseStudyImporter implements StudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForBioInfo.class);

    public static final String TAXON_ID = "Taxon_id";
    public static final String LATIN_80 = "Latin80";
    public static final String DONOR_TAX_ID = "DonorTax_id";
    public static final String RECIP_TAX_ID = "RecipTax_id";
    public static final String TROPHIC_REL_ID = "TrophicRel_Id";
    public static final String TROPHIC_REL_ID_2 = "TrophicRel_id";
    public static final String ENERGY_RECIPIENT = "EnergyRecipient";
    public static final String TAXA_DATA_FILE = "bioinfo.org.uk/Taxa.txt.gz";
    public static final String RELATION_TYPE_DATA_FILE = "bioinfo.org.uk/TrophicRelations.txt.gz";
    public static final String RELATIONS_DATA_FILE = "bioinfo.org.uk/Relations.txt.gz";

    public StudyImporterForBioInfo(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        LabeledCSVParser relationsParser = null;
        try {
            relationsParser = parserFactory.createParser(RELATIONS_DATA_FILE, CharsetConstant.CHARSET_MAC_ROMAN);
            relationsParser.changeDelimiter('\t');
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading trophic relations file [" + RELATIONS_DATA_FILE + "]", e1);
        }
        return createRelations(createTaxa(), createRelationTypes(), relationsParser);
    }

    private Map<Long, RelType> createRelationTypes() throws StudyImporterException {
        LOG.info("relationTypes being created...");

        Map<Long, RelType> relationsTypeMap;
        try {
            LabeledCSVParser relationTypesParser = parserFactory.createParser(RELATION_TYPE_DATA_FILE, CharsetConstant.CHARSET_MAC_ROMAN);
            relationTypesParser.changeDelimiter('\t');
            relationsTypeMap = createRelationsTypeMap(relationTypesParser);
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading trophic relations data [" + RELATION_TYPE_DATA_FILE + "", e1);
        }
        LOG.info("relationTypes created.");
        return relationsTypeMap;
    }

    private Map<Long, String> createTaxa() throws StudyImporterException {
        LOG.info("taxa map being created...");
        Map<Long, String> taxaMap;
        try {
            LabeledCSVParser taxaParser = parserFactory.createParser(TAXA_DATA_FILE, CharsetConstant.CHARSET_MAC_ROMAN);
            taxaParser.changeDelimiter('\t');
            taxaMap = createTaxaMap(taxaParser);
        } catch (IOException e) {
            throw new StudyImporterException("failed to parse taxa file [" + TAXA_DATA_FILE + "]", e);
        }
        LOG.info("taxa map created.");
        return taxaMap;
    }

    protected Map<Long, String> createTaxaMap(LabeledCSVParser taxaParser) throws StudyImporterException {
        Map<Long, String> taxaMap = new HashMap<Long, String>();

        try {
            while (taxaParser.getLine() != null) {
                Long taxonId = labelAsLong(taxaParser, StudyImporterForBioInfo.TAXON_ID);
                if (taxonId == null) {
                    throw new StudyImporterException("failed to parse taxa at line [" + taxaParser.getLastLineNumber() + "]");
                }
                String taxonScientificName = taxaParser.getValueByLabel(StudyImporterForBioInfo.LATIN_80);
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
        // Attempt to map Malcolms interations to http://vocabularies.gbif.org/vocabularies/Interaction
        Map<String, RelType> interactionMapping = new HashMap<String, RelType>();
        interactionMapping.put("ectoparasitises", InteractType.PARASITE_OF);
        interactionMapping.put("is predator of", InteractType.PREYS_UPON);
        interactionMapping.put("is ectomycorrhizal with", InteractType.HAS_HOST);

        Map<Long, RelType> relationsTypeMap = new HashMap<Long, RelType>();
        try {
            while (labeledCSVParser.getLine() != null) {
                Long trophicRelationId = labelAsLong(labeledCSVParser, StudyImporterForBioInfo.TROPHIC_REL_ID_2);
                String descriptionEnergyRecipient = labeledCSVParser.getValueByLabel(StudyImporterForBioInfo.ENERGY_RECIPIENT);
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

    protected Study createRelations(Map<Long, String> taxaMap, Map<Long, RelType> relationsTypeMap, LabeledCSVParser labeledCSVParser) throws StudyImporterException {
        LOG.info("relations being created...");
        String title = StudyLibrary.Study.BIO_INFO.toString();

        Study study = nodeFactory.getOrCreateStudy(title,
                "Malcolm Storey",
                "http://bioinfo.org.uk",
                "",
                "Food webs and species interactions in the Biodiversity of UK and Ireland.");
        try {
            long count = 0;
            while (labeledCSVParser.getLine() != null) {
                count++;
                if (count % 1000 == 0) {
                    LOG.info("[" + count + "] relations created.");
                }

                if (importFilter.shouldImportRecord(count)) {
                    Specimen donorSpecimen = createSpecimen(labeledCSVParser, taxaMap, StudyImporterForBioInfo.DONOR_TAX_ID);
                    study.collected(donorSpecimen);
                    Specimen recipientSpecimen = createSpecimen(labeledCSVParser, taxaMap, StudyImporterForBioInfo.RECIP_TAX_ID);
                    study.collected(recipientSpecimen);
                    donorSpecimen.interactsWith(recipientSpecimen, relationsTypeMap.get(labelAsLong(labeledCSVParser, StudyImporterForBioInfo.TROPHIC_REL_ID)));
                }

            }
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading trophic relations data", e1);
        }
        LOG.info("relations created.");
        return study;
    }

    private Specimen createSpecimen(LabeledCSVParser labeledCSVParser, Map<Long, String> taxaMap, String taxonIdString) throws StudyImporterException {
        Long taxonId = labelAsLong(labeledCSVParser, taxonIdString);
        String scientificName = taxaMap.get(taxonId);
        if (scientificName == null) {
            throw new StudyImporterException("failed to find scientific name for taxonId [" + taxonId + "] at line [" + labeledCSVParser.getLastLineNumber() + "]");
        }
        try {
            return nodeFactory.createSpecimen(scientificName, "bioinfo:" + taxonId);
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

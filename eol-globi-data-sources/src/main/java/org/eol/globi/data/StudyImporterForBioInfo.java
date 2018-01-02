package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.domain.Term;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForBioInfo extends BaseStudyImporter implements StudyImporter {
    public static final String REFERENCE_DATA_FILE = "bioinfo.org.uk/eol_references.csv.gz";
    public static final String RELATIONS_DATA_FILE = "bioinfo.org.uk/eol_taxon_relations.csv.gz";
    public static final String TAXON_DATA_FILE = "bioinfo.org.uk/eol_taxa.csv.gz";
    public static final String BIOINFO_URL = "http://bioinfo.org.uk";

    public static final Map<String, InteractType> INTERACTION_MAPPING = Collections.unmodifiableMap(new HashMap<String, InteractType>() {
        {
            put("Animal / associate", InteractType.INTERACTS_WITH);
            put("Animal / carrion / dead animal feeder", InteractType.ATE);
            put("Animal / commensal", InteractType.INTERACTS_WITH);
            // ensure to map the dung to a abiotic term rather than the producing taxon
            put("Animal / dung / associate", InteractType.INTERACTS_WITH);
            put("Animal / dung / debris feeder", InteractType.INTERACTS_WITH);
            put("Animal / dung / oviposition", InteractType.INTERACTS_WITH);
            put("Animal / dung / saprobe", InteractType.INTERACTS_WITH);
            put("Animal / dung / debris saprobe", InteractType.INTERACTS_WITH);

            put("Animal / endozoite", InteractType.INHABITS);
            put("Animal / epizoite", InteractType.LIVES_ON);

            put("Animal / gamete vector / cross fertilises", InteractType.HAS_DISPERAL_VECTOR);
            put("Animal / guest", InteractType.GUEST_OF);
            put("Animal / honeydew feeder", InteractType.INTERACTS_WITH);
            put("Animal / inquiline", InteractType.GUEST_OF);
            put("Animal / kill", InteractType.KILLS);
            put("Animal / kleptoparasite", InteractType.PARASITE_OF);
            put("Animal / parasite / ectoparasite / blood sucker", InteractType.ECTOPARASITE_OF);
            put("Animal / parasite / ectoparasite / sweat sucker", InteractType.ECTOPARASITE_OF);
            put("Animal / parasite / ectoparasite / tear sucker", InteractType.ECTOPARASITE_OF);
            put("Animal / parasite / ectoparasite", InteractType.ECTOPARASITE_OF);
            put("Animal / parasite / endoparasite", InteractType.ENDOPARASITE_OF);
            put("Animal / parasite", InteractType.PARASITE_OF);
            put("Animal / parasitoid / ectoparasitoid", InteractType.ECTOPARASITOID_OF);
            put("Animal / parasitoid / endoparasitoid", InteractType.ENDOPARASITOID_OF);
            put("Animal / parasitoid", InteractType.PARASITOID_OF);
            put("Animal / pathogen", InteractType.PATHOGEN_OF);
            put("Animal / phoresy", InteractType.VECTOR_OF);
            put("Animal / predator / stocks nest with", InteractType.PREYS_UPON);
            put("Animal / predator", InteractType.PREYS_UPON);
            put("Animal / resting place / on", InteractType.LIVES_ON);
            put("Animal / resting place / under", InteractType.LIVES_UNDER);
            put("Animal / resting place / within", InteractType.LIVES_INSIDE_OF);
            put("Animal / sequestrates", InteractType.INTERACTS_WITH);
            put("Animal / slave maker", InteractType.INTERACTS_WITH);
            put("Animal / vector", InteractType.HAS_VECTOR);
            put("Bacterium / farmer", InteractType.FARMS);
            put("Bacterium / predator", InteractType.PREYS_UPON);
            put("Foodplant / collects", InteractType.VISITS_FLOWERS_OF);
            put("Foodplant / debris feeder", InteractType.ATE);
            put("Foodplant / endophyte", InteractType.LIVES_INSIDE_OF);
            put("Foodplant / false gall", InteractType.INTERACTS_WITH);
            put("Foodplant / feeds on", InteractType.ATE);
            put("Foodplant / gall", InteractType.INTERACTS_WITH);
            put("Foodplant / hemiparasite", InteractType.PARASITE_OF);
            put("Foodplant / immobile silken tube feeder", InteractType.ATE);
            put("Foodplant / internal feeder", InteractType.ATE);
            put("Foodplant / miner", InteractType.ATE);
            put("Foodplant / mobile cased feeder", InteractType.ATE);
            put("Foodplant / mutualist", InteractType.SYMBIONT_OF);
            put("Foodplant / mycorrhiza / ectomycorrhiza", InteractType.SYMBIONT_OF);
            put("Foodplant / mycorrhiza / endomycorrhiza", InteractType.SYMBIONT_OF);
            put("Foodplant / mycorrhiza", InteractType.SYMBIONT_OF);
            put("Foodplant / nest", InteractType.INTERACTS_WITH);
            put("Foodplant / open feeder", InteractType.ATE);
            put("Foodplant / parasite", InteractType.PARASITE_OF);
            put("Foodplant / pathogen", InteractType.PATHOGEN_OF);
            put("Foodplant / robber", InteractType.INTERACTS_WITH);
            put("Foodplant / roller", InteractType.INTERACTS_WITH);
            put("Foodplant / sap sucker", InteractType.ATE);
            put("Foodplant / saprobe", InteractType.ATE);
            put("Foodplant / secondary infection", InteractType.PATHOGEN_OF);
            put("Foodplant / shot hole causer", InteractType.INTERACTS_WITH);
            put("Foodplant / spinner", InteractType.INTERACTS_WITH);
            put("Foodplant / spot causer", InteractType.INTERACTS_WITH);
            put("Foodplant / visitor / nectar", InteractType.VISITS_FLOWERS_OF);
            put("Foodplant / visitor", InteractType.VISITS_FLOWERS_OF);
            put("Foodplant / web feeder", InteractType.ATE);
            put("Fungus / associate", InteractType.INTERACTS_WITH);
            put("Fungus / external feeder", InteractType.ATE);
            put("Fungus / feeder", InteractType.ATE);
            put("Fungus / gall", InteractType.INTERACTS_WITH);
            put("Fungus / infection vector", InteractType.HAS_VECTOR);
            put("Fungus / internal feeder", InteractType.ATE);
            put("Fungus / nest", InteractType.INTERACTS_WITH);
            put("Fungus / parasite / endoparasite", InteractType.ENDOPARASITE_OF);
            put("Fungus / parasite", InteractType.PARASITE_OF);
            put("Fungus / resting place / on", InteractType.LIVES_ON);
            put("Fungus / resting place / within", InteractType.LIVES_INSIDE_OF);
            put("Fungus / saprobe", InteractType.ATE);
            put("Inhibits or restricts the growth of", InteractType.INTERACTS_WITH);
            put("Lichen / associate", InteractType.INTERACTS_WITH);
            put("Lichen / gall", InteractType.INTERACTS_WITH);
            put("Lichen / grows on or over", InteractType.LIVES_ON);
            put("Lichen / kleptoparasite", InteractType.KLEPTOPARASITE_OF);
            put("Lichen / nest", InteractType.INTERACTS_WITH);
            put("Lichen / parasite", InteractType.PARASITE_OF);
            put("Lichen / pathogen", InteractType.PATHOGEN_OF);
            put("Lichen / photobiont", InteractType.INTERACTS_WITH);
            put("Lichen / saprobe", InteractType.ATE);
            put("Lichen / sequestrate", InteractType.INTERACTS_WITH);
            put("Lichen / symbiont", InteractType.SYMBIONT_OF);
            put("Plant / associate", InteractType.INTERACTS_WITH);
            put("Plant / epiphyte", InteractType.LIVES_ON);
            put("Plant / grows among", InteractType.LIVES_NEAR);
            put("Plant / grows inside", InteractType.LIVES_INSIDE_OF);
            put("Plant / hibernates / on", InteractType.LIVES_ON);
            put("Plant / hibernates / under", InteractType.LIVES_UNDER);
            put("Plant / hibernates / within", InteractType.LIVES_INSIDE_OF);
            put("Plant / nest", InteractType.INTERACTS_WITH);
            put("Plant / pollinated", InteractType.POLLINATES);
            put("Plant / resting place / among", InteractType.LIVES_NEAR);
            put("Plant / resting place / on", InteractType.LIVES_ON);
            put("Plant / resting place / under", InteractType.LIVES_UNDER);
            put("Plant / resting place / within", InteractType.LIVES_INSIDE_OF);
            put("Plant / vector", InteractType.HAS_VECTOR);
        }
    });

    public StudyImporterForBioInfo(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    protected static Map<String, String> buildRefMap(final LabeledCSVParser parser) throws IOException {
        Map<String, String> refIdMap = new HashMap<String, String>();
        while (parser.getLine() != null) {
            String refId = parser.getValueByLabel("BioInfo reference id");
            List<String> list = new ArrayList<String>() {
                {
                    String refType = parser.getValueByLabel("reference type");

                    if ("Web Site/Page".equals(refType)) {
                        addIfNotBlank("title");
                        addURL();
                    } else {
                        String author = parser.getValueByLabel("author");
                        if (StringUtils.isBlank(author)) {
                            addIfNotBlank("title");
                            addIfNotBlank("source title");
                            addIfNotBlank("year");
                        } else {
                            addIfNotBlank("author");
                            addIfNotBlank("year");
                            addIfNotBlank("title");
                            addIfNotBlank("source title");
                        }

                        addIfNotBlank("edition");
                        String volume = parser.getValueByLabel("volume");
                        if (StringUtils.isNotBlank(volume)) {
                            add("Vol " + volume);
                        }
                        addIfNotBlank("series");
                        String pageRange = parser.getValueByLabel("page range");
                        if (StringUtils.isNotBlank(pageRange)) {
                            add("pp " + pageRange);
                        }
                        addURL();
                    }
                }

                protected void addIfNotBlank(String name) {
                    String title = parser.getValueByLabel(name);
                    if (StringUtils.isNotBlank(title)) {
                        add(title);
                    }
                }

                protected void addURL() {
                    String url = parser.getValueByLabel("URL of online source");
                    if (StringUtils.isNotBlank(url)) {
                        add("Accessed at: " + parser.getValueByLabel("URL of online source"));
                    }
                }
            };
            refIdMap.put(refId, StringUtils.join(list, ". ").replaceAll("\\s+", " ").trim());
        }
        return refIdMap;
    }

    protected static Map<String, Taxon> buildTaxonMap(LabeledCSVParser parser) throws IOException {
        Map<String, Taxon> taxonMap = new HashMap<String, Taxon>();
        while (parser.getLine() != null) {
            if (StringUtils.isBlank(parser.getValueByLabel("NBN Code"))) {
                Taxon taxon = new TaxonImpl();
                taxon.setRank(parser.getValueByLabel("rank"));
                taxon.setName(StringUtils.replaceChars(parser.getValueByLabel("latin"), "'", ""));
                String bioInfoTaxonId = parser.getValueByLabel("my taxon id");
                taxon.setExternalId(TaxonomyProvider.BIO_INFO + "taxon:" + bioInfoTaxonId);
                taxonMap.put(bioInfoTaxonId, taxon);
            }
        }
        return taxonMap;
    }

    @Override
    public void importStudy() throws StudyImporterException {
        Map<String, String> refMap;

        LabeledCSVParser relationsParser;
        try {
            refMap = buildRefMap(parserFactory.createParser(REFERENCE_DATA_FILE, CharsetConstant.UTF8));
            Map<String, Taxon> taxonMap = buildTaxonMap(parserFactory.createParser(TAXON_DATA_FILE, CharsetConstant.UTF8));
            relationsParser = parserFactory.createParser(RELATIONS_DATA_FILE, CharsetConstant.UTF8);
            createRelations(relationsParser, refMap, taxonMap);
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading trophic relations file [" + RELATIONS_DATA_FILE + "]", e1);
        }
    }

    protected Study createStudy(String refId, String citation) throws NodeFactoryException {
        String sourceCitation = "Food Webs and Species Interactions in the Biodiversity of UK and Ireland (Online). 2015. Data provided by Malcolm Storey. Also available from " + BIOINFO_URL + ".";
        String bioInfoId = TaxonomyProvider.BIO_INFO + "ref:" + refId;
        StudyImpl study1 = new StudyImpl(bioInfoId, sourceCitation, null, citation);
        study1.setExternalId(ExternalIdUtil.urlForExternalId(bioInfoId));
        return nodeFactory.getOrCreateStudy(study1);
    }

    protected void createRelations(LabeledCSVParser parser, Map<String, String> refMap, Map<String, Taxon> taxonMap) throws StudyImporterException {
        try {
            long count = 1;
            while (parser.getLine() != null) {
                if (importFilter.shouldImportRecord(count)) {
                    String refIds = parser.getValueByLabel("list of reference ids");
                    if (StringUtils.isNotBlank(refIds)) {
                        String[] ids = StringUtils.split(refIds, ";");
                        for (String id : ids) {
                            String trimmedId = StringUtils.trim(id);
                            Study study = createStudy(trimmedId, refMap.get(trimmedId));
                            String relationship = parser.getValueByLabel("relationship");
                            if (StringUtils.isBlank(relationship)) {
                                getLogger().warn(study, "no relationship for record on line [" + (parser.lastLineNumber() + 1) + "]");
                            }
                            InteractType interactType = INTERACTION_MAPPING.get(relationship);
                            if (null == interactType) {
                                getLogger().warn(study, "no mapping found for relationship [" + relationship + "] for record on line [" + (parser.lastLineNumber() + 1) + "]");
                            } else {
                                importInteraction(parser, study, interactType, taxonMap);
                            }
                        }
                    }
                }
                count++;
            }
        } catch (IOException e1) {
            throw new StudyImporterException("problem reading trophic relations data", e1);
        }
    }

    private void importInteraction(LabeledCSVParser parser, Study study, InteractType interactType, Map<String, Taxon> taxonMap) throws StudyImporterException {
        String passiveId = parser.getValueByLabel("passive NBN Code");
        String activeId = parser.getValueByLabel("active NBN Code");
        Specimen donorSpecimen = createSpecimen(parser, study, taxonMap, passiveId, parser.getValueByLabel("my passive taxon id"));
        Specimen recipientSpecimen = createSpecimen(parser, study, taxonMap, activeId, parser.getValueByLabel("my active taxon id"));

        if (donorSpecimen != null && recipientSpecimen != null) {
            addLifeStage(parser, donorSpecimen, "stage of passive taxon", study);
            addBodyPart(parser, donorSpecimen, "part of passive taxon", study);
            addLifeStage(parser, recipientSpecimen, "stage of active taxon", study);
            addBodyPart(parser, donorSpecimen, "part of active taxon", study);
            recipientSpecimen.interactsWith(donorSpecimen, interactType);
        }
    }

    private Specimen createSpecimen(LabeledCSVParser parser, Study study, Map<String, Taxon> taxonMap, String nbnId, String bioTaxonId) throws StudyImporterException {
        Specimen specimen = null;
        if (StringUtils.isBlank(nbnId)) {
            try {
                Taxon taxon = taxonMap.get(bioTaxonId);
                if (taxon == null) {
                    getLogger().warn(study, "empty/no taxon name for bioinfo taxon id [" + bioTaxonId + "] on line [" + parser.lastLineNumber() + 1 + "]");
                } else {
                    specimen = nodeFactory.createSpecimen(study, new TaxonImpl(taxon.getName(), TaxonomyProvider.BIO_INFO + "taxon:" + bioTaxonId));
                    setSpecimenExternalId(parser, specimen);
                }
            } catch (NodeFactoryException e) {
                throw new StudyImporterException("failed to create taxon with scientific name [" + bioTaxonId + "]", e);
            }
        } else {
            specimen = createSpecimen(study, parser, nbnId);
        }
        return specimen;
    }

    private void addLifeStage(LabeledCSVParser parser, Specimen donorSpecimen, String columnName, Study study) throws StudyImporterException {
        List<Term> lifeStage = parseTerms(parser, columnName, study);
        donorSpecimen.setLifeStage(lifeStage);
    }

    private void addBodyPart(LabeledCSVParser parser, Specimen donorSpecimen, String columnName, Study study) throws StudyImporterException {
        List<Term> bodyParts = parseTerms(parser, columnName, study);
        donorSpecimen.setBodyPart(bodyParts);
    }

    private List<Term> parseTerms(LabeledCSVParser parser, String stageColumnName, Study study) throws StudyImporterException {
        List<Term> lifeStage = null;
        String donorLifeStage = parser.getValueByLabel(stageColumnName);
        if (donorLifeStage != null && donorLifeStage.trim().length() > 0) {
            lifeStage = parseLifeStage(donorLifeStage, study);
            if (lifeStage == null) {
                throw new StudyImporterException("failed to map stage [" + donorLifeStage + "] on line [" + parser.getLastLineNumber() + "]");
            }
        }
        return lifeStage;
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
            Specimen specimen = nodeFactory.createSpecimen(study, new TaxonImpl(null, TaxonomyProvider.NBN.getIdPrefix() + externalId));
            setSpecimenExternalId(labeledCSVParser, specimen);
            return specimen;
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to create taxon with scientific name [" + externalId + "]", e);
        }
    }

    private void setSpecimenExternalId(LabeledCSVParser labeledCSVParser, Specimen specimen) {
        specimen.setExternalId(TaxonomyProvider.BIO_INFO + "rel:" + labeledCSVParser.lastLineNumber());
    }

}

package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.globalbioticinteractions.dataset.CitationUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForSIAD extends BaseStudyImporter {

    private final static Map<String, InteractType> map = new HashMap<String, InteractType>() {
        {
            put("Associate", InteractType.INTERACTS_WITH);
            put("Associate:Consumer:Herbivore:Fungivore", InteractType.ATE);
            put("Associate:Consumer:Herbivore:Wood borer", InteractType.ATE);
            put("Associate:Consumer:Sanguinivore", InteractType.ATE);
            put("Associate:Consumer:Predator:Carnivorous", InteractType.PREYS_UPON);
            put("Associate:Neutral:Plant associate", InteractType.INTERACTS_WITH);
            put("Associate:Consumer:Herbivore:Xylem feeder", InteractType.ATE);
            put("Associate:Consumer:Parasite", InteractType.PARASITE_OF);
            put("Associate:Consumer:Herbivore:Graminivore", InteractType.ATE);
            put("Associate:Neutral:In Ficus sycones", InteractType.INTERACTS_WITH);
            put("Associate:Consumer:Herbivore:Parenchyma feeder", InteractType.ATE);
            put("Associate:Consumer:Herbivore:Leaf miner", InteractType.ATE);
            put("Associate:Mutualist:Attendant ants", InteractType.INTERACTS_WITH);
            put("Associate:Consumer:Predator", InteractType.PREYS_UPON);
            put("Associate:Consumer:Predator:Arthropod feeder", InteractType.PREYS_UPON);
            put("Associate:Neutral:Flower gall associate", InteractType.INTERACTS_WITH);
            put("Associate:Consumer:Herbivore:Mellivore", InteractType.ATE);
            put("Associate:Consumer:Herbivore:Sap feeder", InteractType.ATE);
            put("Associate:Consumer:Myrmecophagous", InteractType.ATE);
            put("Associate:Consumer:Pest", InteractType.INTERACTS_WITH);
            put("Associate:Neutral:Myrmecophilous", InteractType.INTERACTS_WITH);
            put("Associate:Consumer:Parasitoid", InteractType.PARASITE_OF);
            put("Associate:Commensal:Inquiline", InteractType.INTERACTS_WITH);
            put("Associate:Consumer:Parasite:Ectoparasite", InteractType.PARASITE_OF);
            put("Associate:Neutral:On:Macrophytes", InteractType.INTERACTS_WITH);
            put("Associate:Consumer:Parasite:Endoparasite", InteractType.PARASITE_OF);
            put("Associate:Commensal", InteractType.INTERACTS_WITH);
            put("Associate:Consumer:Parasite:Plant parasite", InteractType.PARASITE_OF);
            put("Associate:Consumer:Herbivore:Gall former", InteractType.ATE);
            put("Associate:Consumer:Omnivore", InteractType.ATE);
            put("Associate:Consumer:Herbivore:Granivore", InteractType.ATE);
            put("Associate:Consumer:Herbivore:Root feeder", InteractType.ATE);
            put("Associate:Consumer:Parasite:Mesoparasitic", InteractType.ATE);
            put("Associate:Consumer:Herbivore:Phloem feeder", InteractType.ATE);
            put("Associate:Consumer:Herbivore:Folivore", InteractType.ATE);
            put("Associate:Consumer:Herbivore:Florivore", InteractType.ATE);
            put("Associate:Consumer:Herbivore:Sap sucker", InteractType.ATE);
            put("Associate:Consumer:Herbivore:Frugivore", InteractType.ATE);
            put("Associate:Consumer:Herbivore", InteractType.ATE);
            put("Associate:Neutral:Algae associate", InteractType.INTERACTS_WITH);
        }
    };
    public static final String PREFIX = "http://www.discoverlife.org/siad/data/source/biodiversity.org.au:dataexport/";
    public static final String[] RESOURCES = new String[]{
            PREFIX + "interactions.Heteroptera.txt",
            PREFIX + "interactions.txt"};

    public StudyImporterForSIAD(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }


    @Override
    public void importStudy() throws StudyImporterException {
        String source = "Species Interactions of Australia Database (SIAD): Helping us to understand species interactions in Australia and beyond. "
                + CitationUtil.createLastAccessedString("http://www.discoverlife.org/siad/");
        for (String resource : RESOURCES) {
            downloadAndImportResource(resource, source);
        }
    }

    private void downloadAndImportResource(String resource, String source) throws StudyImporterException {
        try {
            LabeledCSVParser labeledCSVParser = parserFactory.createParser(resource, "UTF-8");
            labeledCSVParser.changeDelimiter('\t');
            while (labeledCSVParser.getLine() != null) {
                String name = labeledCSVParser.getValueByLabel("name");

                String ref = labeledCSVParser.getValueByLabel("source");
                String title = "SIAD-" + ref;
                String citation = "ABRS 2009. Australian Faunal Directory. " + name + ". Australian Biological Resources StudyNode, Canberra. " + CitationUtil.createLastAccessedString(ref);
                StudyImpl study1 = new StudyImpl(title, source, null, citation);
                study1.setExternalId(ref);
                Study study = nodeFactory.getOrCreateStudy(study1);

                Specimen specimen = nodeFactory.createSpecimen(study, new TaxonImpl(name, null));
                String hostName = labeledCSVParser.getValueByLabel("host name");
                Specimen hostSpecimen = nodeFactory.createSpecimen(study, new TaxonImpl(hostName, null));
                InteractType type = map.get(labeledCSVParser.getValueByLabel("interaction"));
                specimen.interactsWith(hostSpecimen, type);
            }
        } catch (FileNotFoundException e) {
            throw new StudyImporterException("failed to open tmp file", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to map data", e);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + resource + "]", e);
        }
    }
}

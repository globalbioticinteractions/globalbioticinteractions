package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.HttpUtil;
import org.joda.time.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForSIAD extends BaseStudyImporter {

    private static final Log LOG = LogFactory.getLog(StudyImporterForSIAD.class);

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

    public StudyImporterForSIAD(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }


    @Override
    public Study importStudy() throws StudyImporterException {
        String source = "Species Interactions of Australia Database (SIAD): Helping us to understand species interactions in Australia and beyond. Accessed at http://ala1.science.unsw.edu.au/ on " + new DateTime().toString("dd MMM YYYY") + ".";
        Study study = nodeFactory.getOrCreateStudy("SIAD", source, null);
        study.setCitationWithTx(source);
        String prefix = "http://ala1.science.unsw.edu.au/data/source/biodiversity.org.au:dataexport/";
        String resources[] = {
                "interactions.Heteroptera.txt",
                "interactions.txt"};
        for (String resource : resources) {
            downloadAndImportResource(study, prefix, resource);
        }
        return study;
    }

    private void downloadAndImportResource(Study study, String prefix, String resource) throws StudyImporterException {
        LOG.info("resource [" + resource + "] downloading ...");
        HttpResponse response = null;
        String resourceURI = prefix + resource;
        try {
            response = HttpUtil.createHttpClient().execute(new HttpGet(resourceURI));
        } catch (IOException e) {
            throw new StudyImporterException("failed to access resource [" + resourceURI + "]", e);
        }
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("inter", ".txt");
        } catch (IOException e) {
            throw new StudyImporterException("failed to create tmp file", e);
        }
        tmpFile.deleteOnExit();
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tmpFile);
            IOUtils.copy(response.getEntity().getContent(), fos);
            LOG.info("resource [" + resource + "] downloaded.");
            LabeledCSVParser labeledCSVParser = new LabeledCSVParser(new CSVParser(new FileInputStream(tmpFile), '\t'));
            while (labeledCSVParser.getLine() != null) {
                String name = labeledCSVParser.getValueByLabel("name");
                Specimen specimen = nodeFactory.createSpecimen(name);
                String hostName = labeledCSVParser.getValueByLabel("host name");
                Specimen hostSpecimen = nodeFactory.createSpecimen(hostName);
                InteractType type = map.get(labeledCSVParser.getValueByLabel("interaction"));
                specimen.interactsWith(hostSpecimen, type);
                study.collected(specimen);
            }
        } catch (FileNotFoundException e) {
            throw new StudyImporterException("failed to open tmp file", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to map data", e);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + resourceURI + "]");
        }
    }
}

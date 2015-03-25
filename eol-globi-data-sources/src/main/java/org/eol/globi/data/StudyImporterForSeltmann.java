package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForSeltmann extends BaseStudyImporter {

    public StudyImporterForSeltmann(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        DB db = DBMaker
                .newMemoryDirectDB()
                .compressionEnable()
                .transactionDisable()
                .make();
        final HTreeMap<String, Map<String, String>> assocMap = db
                .createHashMap("assocMap")
                .make();

        try {
            LabeledCSVParser parser = parserFactory.createParser("associatedTaxa.tsv", "UTF-8");
            parser.changeDelimiter('\t');
            while (parser.getLine() != null) {
                Map<String, String> prop = new HashMap<String, String>();
                addKeyValue(parser, prop, "dwc:coreid");
                addKeyValue(parser, prop, "dwc:basisOfRecord");
                addKeyValue(parser, prop, "aec:associatedScientificName");
                addKeyValue(parser, prop, "dwc:basisOfRecord");
                addKeyValue(parser, prop, "aec:associatedRelationshipTerm");
                addKeyValue(parser, prop, "aec:associatedRelationshipURI");
                addKeyValue(parser, prop, "aec:associatedLocationOnHost");
                addKeyValue(parser, prop, "aec:associatedEmergenceVerbatimDate");
                assocMap.put(parser.getValueByLabel("dwc:coreid"), prop);
            }

            LabeledCSVParser occurrence = parserFactory.createParser("occurrences.tsv", "UTF-8");
            occurrence.changeDelimiter('\t');
            while (occurrence.getLine() != null) {
                String dataSetName = occurrence.getValueByLabel("dwc:datasetName");
                String references = occurrence.getValueByLabel("dcterms:references");

                Study study = nodeFactory.getOrCreateStudy("seltmann", dataSetName + references, "citation");
                String recordId = occurrence.getValueByLabel("idigbio:recordID");
                Map<String, String> assoc = assocMap.get(recordId);
                if (assoc != null) {
                    String targetName = assoc.get("aec:associatedScientificName");
                    String sourceName = occurrence.getValueByLabel("dwc:scientificName");
                    String eventDate = occurrence.getValueByLabel("dwc:eventDate");
                    DateTimeFormatter fmtDateTime1 = DateTimeFormat.forPattern("yyyy-MM-dd");
                    Date date = fmtDateTime1.parseDateTime(eventDate).toDate();

                    Specimen source = nodeFactory.createSpecimen(study, sourceName);
                    Specimen target = nodeFactory.createSpecimen(study, targetName);
                    source.interactsWith(target, InteractType.INTERACTS_WITH);
                    String basisOfRecord = occurrence.getValueByLabel("dwc:basisOfRecord");
                    nodeFactory.setUnixEpochProperty(source, date);
                    nodeFactory.setUnixEpochProperty(target, date);
                    String latitude = occurrence.getValueByLabel("dwc:decimalLatitude");
                    String longitude = occurrence.getValueByLabel("dwc:decimalLongitude");
                    source.caughtIn(nodeFactory.getOrCreateLocation(Double.parseDouble(latitude), Double.parseDouble(longitude), null));
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException(e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException(e);
        }
        return null;
    }

    protected void addKeyValue(LabeledCSVParser parser, Map<String, String> prop, String key) {
        prop.put(key, parser.getValueByLabel(key));
    }

}

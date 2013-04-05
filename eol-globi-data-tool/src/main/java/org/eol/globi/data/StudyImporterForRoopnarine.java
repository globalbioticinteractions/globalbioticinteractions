package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import uk.me.jstott.jcoord.LatLng;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForRoopnarine extends BaseStudyImporter {

    private static final Log LOG = LogFactory.getLog(StudyImporterForRoopnarine.class);

    public StudyImporterForRoopnarine(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        String suffix = ".csv";
        String prefix = "roopnarine/857470.item.";
        String trophicGuildLookup = prefix + 4 + suffix;
        final Map<Integer, List<String>> trophicGuildNumberToSpeciesMap = buildGuildLookup(trophicGuildLookup);

        Map<String, LatLng> resourceLocation = resourceLocationMap(suffix, prefix);

        Study study = nodeFactory.createStudy(StudyLibrary.Study.ROOPNARINE.toString(),
                "Peter D. Roopnarine and Rachel Hertog",
                "Department of Invertebrate Zoology and Geology, California Academy of Sciences, San Francisco, CA 94118, USA",
                "",
                "<a href=\"http://www.hindawi.com/dpis/ecology/2013/857470/dataset/\">Detailed Food Web Networks of Three Greater Antillean Coral Reef Systems: The Cayman Islands, Cuba, and Jamaica</a>");
        for (Map.Entry<String, LatLng> resourceLatLngEntry : resourceLocation.entrySet()) {
            String studyResource = resourceLatLngEntry.getKey();
            LOG.info("import of [" + studyResource + "] started...");
            List<Specimen> predatorSpecimen = importTrophicInteractions(trophicGuildLookup, trophicGuildNumberToSpeciesMap, studyResource);
            for (Specimen predator : predatorSpecimen) {
                study.collected(predator);
                LatLng latLng = resourceLatLngEntry.getValue();
                // TODO - depth is not encoded here
                predator.caughtIn(nodeFactory.getOrCreateLocation(latLng.getLat(), latLng.getLng(), 0.0));
            }
            LOG.info("import of [" + studyResource + "] done.");
        }
        return study;
    }

    private Map<String, LatLng> resourceLocationMap(String suffix, String prefix) {
        LatLng jamaica = new LatLng(18.1315, -77.2736);
        LatLng cuba = new LatLng(22.0289, -79.9370);
        LatLng caymanIslands = new LatLng(19.30, -80.40);

        Map<String, LatLng> resourceLocation = new HashMap<String, LatLng>();
        resourceLocation.put(prefix + 1 + suffix, caymanIslands);
        resourceLocation.put(prefix + 2 + suffix, cuba);
        resourceLocation.put(prefix + 3 + suffix, jamaica);
        return resourceLocation;
    }

    private List<Specimen> importTrophicInteractions(String trophicGuildLookup, Map<Integer, List<String>> trophicGuildNumberToSpeciesMap, String studyResource) throws StudyImporterException {
        try {
            LabeledCSVParser parser = parserFactory.createParser(studyResource);
            List<Specimen> predatorSpecimen = new ArrayList<Specimen>();
            while (parser.getLine() != null) {
                List<Taxon> preyTaxonList = importPreyList(trophicGuildNumberToSpeciesMap, parser);
                if (preyTaxonList.size() > 0) {
                    predatorSpecimen.addAll(importPredatorSpecimen(trophicGuildLookup, trophicGuildNumberToSpeciesMap, parser, preyTaxonList));
                }
            }
            return predatorSpecimen;

        } catch (IOException e) {
            throw new StudyImporterException("failed to read trophic guild lookup [" + trophicGuildLookup + "]", e);
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import trophic links [" + studyResource + "]", e);
        } catch (StudyImporterException e) {
            throw new StudyImporterException("failed to import trophic links from resource [" + studyResource + "]", e);
        }
    }

    private Map<Integer, List<String>> buildGuildLookup(String trophicGuildLookup) throws StudyImporterException {
        final Map<Integer, List<String>> trophicGuildNumberToSpeciesMap = new HashMap<Integer, List<String>>();
        try {
            LabeledCSVParser parser = parserFactory.createParser(trophicGuildLookup);
            while (parser.getLine() != null) {
                Integer guildNumber = parseGuildNumber(trophicGuildLookup, parser);

                String taxaName = parser.getValueByLabel("Taxa");

                if (null == taxaName) {
                    throw new StudyImporterException("missing or empty Taxa field");
                }

                List<String> taxaForGuild = trophicGuildNumberToSpeciesMap.get(guildNumber);
                if (null == taxaForGuild) {
                    taxaForGuild = new ArrayList<String>();
                    trophicGuildNumberToSpeciesMap.put(guildNumber, taxaForGuild);
                }

                taxaForGuild.add(taxaName.trim());
            }


        } catch (IOException e) {
            throw new StudyImporterException("failed to read trophic guild lookup [" + trophicGuildLookup + "]", e);
        }
        return trophicGuildNumberToSpeciesMap;
    }

    private List<Specimen> importPredatorSpecimen(String trophicGuildLookup, Map<Integer, List<String>> trophicGuildNumberToSpeciesMap, LabeledCSVParser parser, List<Taxon> preyTaxonList) throws StudyImporterException, NodeFactoryException {
        Integer predatorGuildNumber = parseGuildNumber(trophicGuildLookup, parser);
        List<Specimen> predatorSpecimenList = new ArrayList<Specimen>();
        List<String> predatorTaxaList = trophicGuildNumberToSpeciesMap.get(predatorGuildNumber);
        if (predatorTaxaList == null) {
            throw new StudyImporterException("no species available for guild number [" + predatorGuildNumber + "]");
        }
        for (String predatorTaxa : predatorTaxaList) {
            // TODO - here's where the specimen model doesn't fit nicely - need a way to distinguish inferred relationships from direct observations
            Specimen predatorSpecimen = nodeFactory.createSpecimen(predatorTaxa);
            predatorSpecimenList.add(predatorSpecimen);
            for (Taxon preyTaxon : preyTaxonList) {
                Specimen preySpecimen = nodeFactory.createSpecimen(preyTaxon);
                predatorSpecimen.ate(preySpecimen);
            }
        }
        return predatorSpecimenList;
    }

    private List<Taxon> importPreyList(Map<Integer, List<String>> trophicGuildNumberToSpeciesMap, LabeledCSVParser parser) throws NodeFactoryException, StudyImporterException {
        String preyList = parser.getValueByLabel("Prey");
        // see README
        preyList = preyList.replace("20.22", "20, 22");
        preyList = preyList.replace(" ", ",");
        preyList = preyList.replace("100103", "100, 103");
        preyList = preyList.replace("100103", "100, 103");
        preyList = preyList.replace("129188157142192000000000", "");
        preyList = preyList.replace("109108188196107000000", "");
        preyList = preyList.replace("207196", "");
        preyList = preyList.replace("142213", "");
        preyList = preyList.replace("152153", "");
        preyList = preyList.replace("152109183215232000000000000000000000000000000000000000000000000", "");
        String[] preyGuildNumberList = preyList.split(",");
        List<Taxon> preyTaxonList = new ArrayList<Taxon>();
        for (String preyGuildNumberString : preyGuildNumberList) {
            String trim = preyGuildNumberString.replaceAll("\\.", "").trim();
            if (trim.length() > 0) {
                try {
                    Integer preyGuildNumber = Integer.parseInt(trim);
                    if (preyGuildNumber == 0) {
                        LOG.warn("ignoring prey with guild number 0 on line [" + parser.lastLineNumber() + "]");
                    } else {
                        addPreyTaxa(trophicGuildNumberToSpeciesMap, parser, preyTaxonList, preyGuildNumberString, preyGuildNumber);
                    }
                } catch (NumberFormatException ex) {
                    throw new StudyImporterException("failed to parse trophic guild number [" + trim + "] at line [" + parser.lastLineNumber() + "]");
                }
            }
        }
        return preyTaxonList;
    }

    private void addPreyTaxa(Map<Integer, List<String>> trophicGuildNumberToSpeciesMap, LabeledCSVParser parser, List<Taxon> preyTaxonList, String preyGuildNumberString, Integer preyGuildNumber) throws StudyImporterException, NodeFactoryException {
        List<String> preyTaxaList = trophicGuildNumberToSpeciesMap.get(preyGuildNumber);
        if (preyTaxaList == null) {
            throw new StudyImporterException("prey trophic guild number [" + preyGuildNumberString + "] does not map to species name, line [" + parser.lastLineNumber() + "]");
        }
        for (String preyTaxa : preyTaxaList) {
            Taxon preyTaxon = nodeFactory.getOrCreateTaxon(preyTaxa);
            preyTaxonList.add(preyTaxon);
        }
    }

    private Integer parseGuildNumber(String trophicGuildLookup, LabeledCSVParser parser) throws StudyImporterException {
        Integer guildNumber = null;
        String guildNumberString = parser.getValueByLabel("Guild Number");
        if (guildNumberString != null && guildNumberString.trim().length() > 0) {
            try {
                guildNumber = Integer.parseInt(guildNumberString);
            } catch (NumberFormatException ex) {
                throw new StudyImporterException("failed to parse Guild Number [" + guildNumberString + "] at line [" + parser.lastLineNumber() + "] of file [" + trophicGuildLookup + "]");
            }
        }
        return guildNumber;
    }
}

package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.LatLng;
import org.eol.globi.service.TermLookupServiceException;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForBrose extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForBrose.class);

    public static final String SOURCE = "Brose, U. et al., 2005. Body sizes of consumers and their resources. Ecology 86:2545. Available from doi:10.1890/05-0379 .";
    public static final String RESOURCE_PATH = "http://www.esapubs.org/archive/ecol/E086/135/bodysizes_2008.txt";
    public static final String REFERENCE_PATH = "brose/references.csv";

    private static final Map<String, LatLng> LOC_MAP = new HashMap<String, LatLng>() {{
        put("Country: United Kingdom; UTM: 51.24'N, 0.34'W; Silwood Park, Berkshire", new LatLng(51.24d, -0.34d));
        put("Country: United Kingdom; UTM: 53.40'N, 0.59'W; Skipwith Common, North Yorkshire", new LatLng(53.40, -0.59));
        put("Country: United Kingdom; the River Frome, Dorset;  UTM: 50.40'N, 2.11'W", new LatLng(50.40, -2.11));
        put("Country: Australia; Mulgrave River; UTM: 17.08'S, 145.52'E", new LatLng(-17.08, 145.52));
        put("Country: United States of America; UTM: 89.32'W, 46.13'N; Tuesday Lake, Michigan ", new LatLng(46.13, -89.32));
        put("Country: United Kingdom; UTM: 51.05'N, 0.03'E; Broadstone Stream in Sussex", new LatLng(51.05, 0.03));
        put("Country: Switzerland; Lake Neuch�tel", new LatLng(46.92518, 6.87572)); // see http://www.geonames.org/2659494/lac-de-neuchatel.html
        put("Europe, Celtic Sea ecosystem", new LatLng(50, -9)); // see http://www.geonames.org/2960856/celtic-sea.html
        put("Country: United Kingdom", new LatLng(54.75844, -2.69531)); // see http://www.geonames.org/2635167/united-kingdom-of-great-britain-and-northern-ireland.html
        put("Antarctica: Eastern Weddell Sea", new LatLng(-75.0494, -47.2628)); // see http://www.geonames.org/4036624/weddell-sea.html
        put("Africa, Benguela ecosystem", new LatLng(-12.854, 13.92825)); // see http://www.geonames.org/3351660/benguela.html
        put("Country: United Kingdom; Sheffield;", new LatLng(53.41667, -1.5)); // see http://www.geonames.org/3333193/sheffield.html
        put("Mid-Europe", new LatLng(48.69096, 9.14062)); // see http://www.geonames.org/6255148/europe.html
        put("Country: United States of America; Atlantic coast", new LatLng(40.9777996, -79.5252906)); // point picked from google map: an educated guess
        put("Country: Australia", new LatLng(-25, 135)); // see http://www.geonames.org/2077456/commonwealth-of-australia.html
        put("Country: United States of America; John Muir Wilderness Area and Kings Canyon National Park, Sierra Nevada mountains, California", new LatLng(36.925347, -118.681823)); // see picked from map halfway point between john muir and kings canyon.
        put("-999", null);
        put("Europe, USA, Arctic, India", null); // too vague
    }};

    public StudyImporterForBrose(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        LabeledCSVParser dataParser;
        try {
            dataParser = parserFactory.createParser(RESOURCE_PATH, CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + RESOURCE_PATH + "]", e);
        }
        dataParser.changeDelimiter('\t');

        Map<String, String> refMap = ReferenceUtil.buildRefMap(parserFactory, REFERENCE_PATH);

        try {
            while (dataParser.getLine() != null) {
                if (importFilter.shouldImportRecord((long) dataParser.getLastLineNumber())) {
                    importLine(dataParser, refMap);
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem importing study at line [" + dataParser.lastLineNumber() + "]", e);
        }
    }

    private void importLine(LabeledCSVParser parser, Map<String, String> refMap) throws StudyImporterException {
        Study localStudy = null;
        try {
            String shortReference = StringUtils.trim(parser.getValueByLabel("Link reference"));
            if (!refMap.containsKey(shortReference)) {
                throw new StudyImporterException("failed to find ref [" + shortReference + "] on line [" + parser.lastLineNumber() + "]");
            }
            String longReference = refMap.get(shortReference);
            localStudy = nodeFactory.getOrCreateStudy(new StudyImpl("BROSE-" + StringUtils.abbreviate(longReference, 20), SOURCE, null, ExternalIdUtil.toCitation(null, longReference, null)));

            String name = getName(parser, "Taxonomy consumer", "Common name(s) consumer");
            if (StringUtils.isBlank(name)) {
                getLogger().warn(localStudy, "found empty name on line [" + parser.lastLineNumber() + "]");
            } else {
                addInteractionForConsumer(parser, localStudy, name);
            }
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("problem creating nodes at line [" + parser.lastLineNumber() + "]", e);
        } catch (NumberFormatException e) {
            String message = "skipping record, found malformed field at line [" + parser.lastLineNumber() + "]: ";
            if (localStudy != null) {
                getLogger().warn(localStudy, message + e.getMessage());
            }
        }
    }

    private String getName(LabeledCSVParser parser, String primaryLabel, String secondaryLabel) {
        String name = parser.getValueByLabel(primaryLabel);
        if ("-999".equals(name)) {
            name = parser.getValueByLabel(secondaryLabel);
        }
        return name;
    }

    private void addInteractionForConsumer(LabeledCSVParser parser, Study localStudy, String predatorName) throws NodeFactoryException, StudyImporterException {

        Location location = null;
        String locationString = parser.getValueByLabel("Geographic location");
        LatLng latLng = LOC_MAP.get(StringUtils.trim(locationString));
        if (latLng == null) {
            getLogger().warn(localStudy, "failed to find location for [" + locationString + "]");
        } else {
            location = nodeFactory.getOrCreateLocation(new LocationImpl(latLng.getLat(), latLng.getLng(), null, null));
            String habitat = StringUtils.join(parser.getValueByLabel("General habitat"), " ", parser.getValueByLabel("Specific habitat"));
            String habitatId = "BROSE:" + habitat.replaceAll("\\W", "_");
            nodeFactory.getOrCreateEnvironments(location, habitatId, habitat);
        }

        Specimen consumer = nodeFactory.createSpecimen(localStudy, new TaxonImpl(predatorName, null));
        consumer.caughtIn(location);
        addLifeStage(parser, consumer, "Lifestage consumer");


        String name = getName(parser, "Taxonomy resource", "Common name(s) resource");
        if (StringUtils.isBlank(name) || StringUtils.length(name) < 2) {
            String message = "found (near) empty prey name on line [" + parser.lastLineNumber() + "] + [" + name + "]";
            LOG.warn(message);
            getLogger().warn(localStudy, message);
        } else {
            Specimen resource = nodeFactory.createSpecimen(localStudy, new TaxonImpl(name, null));
            resource.caughtIn(location);
            addLifeStage(parser, resource, "Lifestage - resource");
            String interactionType = parser.getValueByLabel("Type of feeding interaction");
            Map<String, InteractType> typeMapping = new HashMap<String, InteractType>() {
                {
                    put("predacious", InteractType.PREYS_UPON);
                    put("predator", InteractType.PREYS_UPON);
                    put("herbivorous", InteractType.ATE);
                    put("parasitoid", InteractType.PARASITE_OF);
                    put("parasitic", InteractType.PARASITE_OF);
                    put("bacterivorous", InteractType.ATE);
                    put("omnivore", InteractType.ATE);
                    put("detritivorous", InteractType.ATE);
                    put("pathogen", InteractType.PATHOGEN_OF);
                }
            };
            InteractType interactType = typeMapping.get(interactionType);
            if (interactType == null) {
                throw new StudyImporterException("found unsupported interaction type [" + interactionType + "]");
            }
            consumer.interactsWith(resource, interactType);
        }
    }

    private void addLifeStage(LabeledCSVParser parser, Specimen specimen, String label) throws StudyImporterException {
        String lifeStageString = parser.getValueByLabel(label);
        try {
            List<Term> terms = nodeFactory.getTermLookupService().lookupTermByName(lifeStageString);
            if (terms.size() == 0) {
                throw new StudyImporterException("unsupported life stage [" + lifeStageString + "] on line [" + parser.getLastLineNumber() + "]");
            }
            specimen.setLifeStage(terms);
        } catch (TermLookupServiceException e) {
            throw new StudyImporterException(("failed to map life stage [" + lifeStageString + "]"));
        }
    }

}

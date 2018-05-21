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
import org.eol.globi.util.ExternalIdUtil;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterForBell extends BaseStudyImporter {

    private static final Log LOG = LogFactory.getLog(StudyImporterForBell.class);

    public static final String[] RESOURCE = {"bell/Bell_GloBI_Harb.csv",
            "bell/Bell_GloBI_Hcuc.csv", "bell/Bell_GloBI_Npac.csv", "bell/Bell_GloBI_Seut.csv"};

    private static final Map<String, String> REFS = new HashMap<String, String>() {
        {
            put("DMNS", "Denver Museum of Nature & Science Mammal Collection");
            put("MSB", "Museum of Southwestern Biology Mammal Collection");
            put("MLZ", "Moore Laboratory of Zoology");
            put("MVZ", "Museum of Vertebrate Zoology Mammal Collection");
            put("USNM", "United States National Museum of Natural History");
        }
    };

    public StudyImporterForBell(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        for (String resource : RESOURCE) {
            LabeledCSVParser parser = null;
            try {
                parser = parserFactory.createParser(resource, "UTF-8");
                while (parser.getLine() != null) {
                    String sourceCitation = "Bell, K. C., Matek, D., Demboski, J. R., & Cook, J. A. (2015). Expanded Host Range of Sucking Lice and Pinworms of Western North American Chipmunks. Comparative Parasitology, 82(2), 312–321. doi:10.1654/4756.1 . Data provided by Kayce C. Bell.";
                    String guid = parser.getValueByLabel("GUID");
                    String externalId = "http://arctos.database.museum/guid/" + guid;
                    String description = null;
                    String collectionId = null;

                    for (String key : REFS.keySet()) {
                        if (guid.startsWith(key)) {
                            description = REFS.get(key);
                            collectionId = key;
                            break;
                        }
                    }
                    if (StringUtils.isBlank(description)) {
                        LOG.warn("missing collectionId [" + guid + "] in file [" + resource + "] on line [" + parser.lastLineNumber() + "]");
                        description = sourceCitation;
                        collectionId = "";
                    }

                    Study study = nodeFactory.getOrCreateStudy(new StudyImpl("bell-" + collectionId, sourceCitation, "https://doi.org/10.1654/4756.1", ExternalIdUtil.toCitation(null, sourceCitation + " " + description, null)));

                    String genus = parser.getValueByLabel("Genus");
                    String species = parser.getValueByLabel("Species");

                    String parasiteName = StringUtils.join(new String[]{StringUtils.trim(genus), StringUtils.trim(species)}, " ");
                    Specimen parasite = nodeFactory.createSpecimen(study, new TaxonImpl(parasiteName, null));
                    parasite.setExternalId(externalId);
                    Location location = getLocation(parser, parasite);
                    parasite.caughtIn(location);

                    String scientificName = parser.getValueByLabel("SCIENTIFIC_NAME");
                    String hostName = StringUtils.trim(scientificName);
                    Specimen host = nodeFactory.createSpecimen(study, new TaxonImpl(hostName, null));
                    host.caughtIn(location);
                    host.setExternalId(externalId);
                    parasite.interactsWith(host, InteractType.PARASITE_OF);
                    Date date = parseDate(parser);
                    nodeFactory.setUnixEpochProperty(parasite, date);
                    nodeFactory.setUnixEpochProperty(host, date);

                }
            } catch (Throwable e) {
                throw new StudyImporterException(getErrorMessage(resource, parser), e);
            }
        }
    }

    protected java.util.Date parseDate(LabeledCSVParser parser) throws StudyImporterException {
        String date = StringUtils.trim(parser.getValueByLabel("VERBATIM_DATE"));
        DateTime dateTime = attemptParse(date, "MM/dd/yy");
        if (dateTime == null) {
            dateTime = attemptParse(date, "MM-dd-yy");
        }
        if (dateTime == null) {
            dateTime = attemptParse(date, "yyyy-MM-dd");
        }

        if (!StringUtils.equals("before 1 Jan 2005", date) && dateTime == null) {
            throw new StudyImporterException("failed to parse [" + date + "] line [" + parser.lastLineNumber() + "]");
        }

        return dateTime == null ? null : dateTime.toDate();
    }

    protected DateTime attemptParse(String date, String pattern) {
        DateTime dateTime = null;
        try {
            dateTime = DateTimeFormat.forPattern(pattern).withZoneUTC().parseDateTime(date);
        } catch (IllegalArgumentException ex) {
            //
        }
        return dateTime;
    }

    protected String getErrorMessage(String resource, LabeledCSVParser parser) {
        String msg = "failed to import [" + resource + "]";
        if (parser != null) {
            msg += " on line [" + parser.lastLineNumber() + "]";
        }
        return msg;
    }


    protected Location getLocation(LabeledCSVParser parser, Specimen parasite) throws NodeFactoryException {
        String latitude = parser.getValueByLabel("DEC_LAT");
        String longitude = parser.getValueByLabel("DEC_LONG");
        Location location = null;
        if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude)) {
            location = nodeFactory.getOrCreateLocation(new LocationImpl(Double.parseDouble(latitude), Double.parseDouble(longitude), null, null));
        }
        return location;
    }

}

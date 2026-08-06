package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.LocationImpl;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.geo.LatLng;
import org.globalbioticinteractions.doi.DOI;

import java.io.IOException;
import java.net.URI;

public class DatasetImporterForCruaud extends NodeBasedImporter {

    private static final Logger LOG = LoggerFactory.getLogger(DatasetImporterForCruaud.class);

    public static final String SOURCE = "Cruaud, A., Ronsted, N., Chantarasuwan, B., Chou, L. S., Clement, W. L., Couloux, A., … Savolainen, V. (2012). An Extreme Case of Plant-Insect Codiversification: Figs and Fig-Pollinating Wasps. Systematic Biology, 61(6), 1029–1047. doi:10.1093/sysbio/sys068";
    public static final URI RESOURCE_PATH = URI.create( "cruaud/Cruaud_et_al2012_Appendix_S2-wasp_material.csv");

    public DatasetImporterForCruaud(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        LabeledCSVParser dataParser;
        try {
            dataParser = getParserFactory().createParser(RESOURCE_PATH, CharsetConstant.UTF8);
        } catch (IOException e) {
            throw new StudyImporterException("failed to read resource [" + RESOURCE_PATH + "]", e);
        }
        try {
            Study study = getNodeFactory().getOrCreateStudy(
                    new StudyImpl("cruaud", new DOI("1093", "sysbio/sys068"), SOURCE));
            while (dataParser.getLine() != null) {
                if (importFilter.shouldImportRecord((long) dataParser.getLastLineNumber())) {
                    try {
                        String parasiteName = StringUtils.trim(dataParser.getValueByLabel("Family and Species"));
                        String hostName = StringUtils.trim(dataParser.getValueByLabel("Natural host Ficus species"));
                        hostName = StringUtils.replace(hostName, "F.", "Ficus");
                        if (areNamesAvailable(parasiteName, hostName)) {
                            Specimen parasite = getNodeFactory().createSpecimen(study, new TaxonImpl(parasiteName, null));
                            Specimen host = getNodeFactory().createSpecimen(study, new TaxonImpl(hostName, null));
                            String samplingLocation = StringUtils.trim(dataParser.getValueByLabel("Sampling location"));
                            if (enableGeonames() && getGeoNamesService().hasTermForLocale(samplingLocation)) {
                                LatLng pointForLocality = getGeoNamesService().findLatLng(samplingLocation);
                                if (pointForLocality == null) {
                                    LOG.warn("no location associated with locality [" + samplingLocation + "]");
                                } else {
                                    Location location = getNodeFactory().getOrCreateLocation(new LocationImpl(pointForLocality.getLat(), pointForLocality.getLng(), null, null));
                                    parasite.caughtIn(location);
                                    host.caughtIn(location);
                                }
                            } else {
                                LOG.warn("no location associated with locality [" + samplingLocation + "]");
                            }
                            parasite.interactsWith(host, InteractType.PARASITE_OF);
                        }
                    } catch (NodeFactoryException | NumberFormatException e) {
                        throw new StudyImporterException("failed to import line [" + (dataParser.lastLineNumber() + 1) + "]", e);
                    }
                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("problem importing [" + RESOURCE_PATH + "]", e);
        }
    }

    private boolean enableGeonames() {
        return false;
    }

    protected boolean areNamesAvailable(String parasiteName, String hostName) {
        return StringUtils.isNotBlank(parasiteName) && StringUtils.isNotBlank(hostName)
                && !StringUtils.equals("na", parasiteName) && !StringUtils.equals("na", hostName);
    }

}

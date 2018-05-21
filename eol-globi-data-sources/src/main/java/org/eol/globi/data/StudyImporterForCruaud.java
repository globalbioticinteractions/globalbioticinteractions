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
import org.eol.globi.geo.LatLng;

import java.io.IOException;

public class StudyImporterForCruaud extends BaseStudyImporter {

    private static final Log LOG = LogFactory.getLog(StudyImporterForCruaud.class);

    public static final String SOURCE = "Cruaud, A., Ronsted, N., Chantarasuwan, B., Chou, L. S., Clement, W. L., Couloux, A., … Savolainen, V. (2012). An Extreme Case of Plant-Insect Codiversification: Figs and Fig-Pollinating Wasps. Systematic Biology, 61(6), 1029–1047. doi:10.1093/sysbio/sys068";
    public static final String RESOURCE_PATH = "cruaud/Cruaud_et_al2012_Appendix_S2-wasp_material.csv";

    public StudyImporterForCruaud(ParserFactory parserFactory, NodeFactory nodeFactory) {
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
        try {
            Study study = nodeFactory.getOrCreateStudy(
                    new StudyImpl("cruaud", SOURCE, "https://doi.org/10.1093/sysbio/sys068", null));
            while (dataParser.getLine() != null) {
                if (importFilter.shouldImportRecord((long) dataParser.getLastLineNumber())) {
                    try {
                        String parasiteName = StringUtils.trim(dataParser.getValueByLabel("Family and Species"));
                        String hostName = StringUtils.trim(dataParser.getValueByLabel("Natural host Ficus species"));
                        hostName = StringUtils.replace(hostName, "F.", "Ficus");
                        if (areNamesAvailable(parasiteName, hostName)) {
                            Specimen parasite = nodeFactory.createSpecimen(study, new TaxonImpl(parasiteName, null));
                            Specimen host = nodeFactory.createSpecimen(study, new TaxonImpl(hostName, null));
                            parasite.interactsWith(host, InteractType.PARASITE_OF);
                            String samplingLocation = StringUtils.trim(dataParser.getValueByLabel("Sampling location"));
                            if (getGeoNamesService().hasTermForLocale(samplingLocation)) {
                                LatLng pointForLocality = getGeoNamesService().findLatLng(samplingLocation);
                                if (pointForLocality == null) {
                                    LOG.warn("no location associated with locality [" + samplingLocation + "]");
                                } else {
                                    Location location = nodeFactory.getOrCreateLocation(new LocationImpl(pointForLocality.getLat(), pointForLocality.getLng(), null, null));
                                    parasite.caughtIn(location);
                                    host.caughtIn(location);
                                }
                            } else {
                                LOG.warn("no location associated with locality [" + samplingLocation + "]");
                            }
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

    protected boolean areNamesAvailable(String parasiteName, String hostName) {
        return StringUtils.isNotBlank(parasiteName) && StringUtils.isNotBlank(hostName)
                && !StringUtils.equals("na", parasiteName) && !StringUtils.equals("na", hostName);
    }

}

package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.globalbioticinteractions.dataset.CitationUtil;
import org.globalbioticinteractions.doi.DOI;

import java.io.IOException;
import java.net.URI;

public class DatasetImporterForStrona extends NodeBasedImporter {
    public static final String SOURCE = "Giovanni Strona, Maria Lourdes D. Palomares, Nicolas Bailly, Paolo Galli, and Kevin D. Lafferty. 2013. Host range, host ecology, and distribution of more than 11800 fish parasite species. Ecology 94:544. https://doi.org/10.1890/12-1419.1";
    public static final URI RESOURCE_PATH = URI.create("http://www.esapubs.org/archive/ecol/E094/045/FPEDB.csv");

    public DatasetImporterForStrona(ParserFactory parserFactory, NodeFactory nodeFactory) {
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
                    new StudyImpl("strona2013", new DOI("1890", "12-1419.1"), SOURCE));
            while (dataParser.getLine() != null) {
                if (importFilter.shouldImportRecord((long) dataParser.getLastLineNumber())) {
                    try {
                        String parasiteName = StringUtils.trim(dataParser.getValueByLabel("P_SP"));
                        String hostName = StringUtils.trim(dataParser.getValueByLabel("H_SP"));
                        if (areNamesAvailable(parasiteName, hostName)) {
                            Specimen parasite = getNodeFactory().createSpecimen(study, new TaxonImpl(parasiteName, null));
                            Specimen host = getNodeFactory().createSpecimen(study, new TaxonImpl(hostName, null));
                            parasite.interactsWith(host, InteractType.PARASITE_OF);
                        }
                    } catch (NodeFactoryException | NumberFormatException e) {
                        throw new StudyImporterException("failed to import line [" + (dataParser.lastLineNumber() + 1) + "]", e);
                    }
                }

            }
        } catch (IOException | NodeFactoryException e) {
            throw new StudyImporterException("problem importing [" + RESOURCE_PATH + "]", e);
        }
    }

    protected boolean areNamesAvailable(String parasiteName, String hostName) {
        return StringUtils.isNotBlank(parasiteName) && StringUtils.isNotBlank(hostName)
                && !StringUtils.equals("na", parasiteName) && !StringUtils.equals("na", hostName);
    }

}

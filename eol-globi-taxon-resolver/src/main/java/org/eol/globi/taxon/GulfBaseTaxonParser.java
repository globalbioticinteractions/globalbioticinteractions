package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.data.CharsetConstant;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.util.CSVTSVUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GulfBaseTaxonParser implements TaxonParser {
    @Override
    public void parse(BufferedReader reader, TaxonImportListener listener) throws IOException {
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(reader);
        listener.start();
        while (labeledCSVParser.getLine() != null) {
            List<String> pathNames = new ArrayList<String>();
            List<String> path = new ArrayList<String>();

            String[] nonSpeciesRankFieldNames = {"Kingdom", "Phylum", "Subphylum", "Class", "Subclass", "Infraclass", "Superorder", "Order", "Suborder", "Infraorder", "Section", "Subsection", "Superfamily", "Above family", "Family", "Subfamily", "Tribe", "Supergenus", "Genus"};
            for (String label : nonSpeciesRankFieldNames) {
                String value = labeledCSVParser.getValueByLabel(label);
                if (value == null) {
                    throw new IOException("failed to field [" + label + "] at line [" + labeledCSVParser.getLastLineNumber() + "]");
                }
                if (StringUtils.isNotBlank(value)) {
                    pathNames.add(StringUtils.lowerCase(label));
                    path.add(value);
                }
            }
            Taxon term = new TaxonImpl();
            term.setExternalId(TaxonomyProvider.ID_PREFIX_GULFBASE + labeledCSVParser.getValueByLabel("Species number"));
            String taxonName = labeledCSVParser.getValueByLabel("Scientific name");
            pathNames.add("species");
            path.add(taxonName);
            term.setName(taxonName);

            term.setPath(StringUtils.join(path, CharsetConstant.SEPARATOR));
            term.setPathNames(StringUtils.join(pathNames, CharsetConstant.SEPARATOR));
            listener.addTerm(term);
        }
        listener.finish();

    }

}

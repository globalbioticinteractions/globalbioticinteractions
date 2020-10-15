package org.globalbioticinteractions.util;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TaxonomyProvider;
import org.eol.globi.service.TaxonUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class OpenBiodivUtil {
    public static Taxon retrieveTaxonHierarchyById(String taxonId, SparqlClient sparqlClient) throws IOException {
        final String normalizedTaxonId = StringUtils.replace(taxonId, TaxonomyProvider.OPEN_BIODIV.getIdPrefix(), "");
        String sparql = "PREFIX fabio: <http://purl.org/spar/fabio/>\n" +
                "PREFIX prism: <http://prismstandard.org/namespaces/basic/2.0/>\n" +
                "PREFIX doco: <http://purl.org/spar/doco/>\n" +
                "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                "select ?name ?rank ?id ?kingdom ?phylum ?class ?order ?family ?genus ?specificEpithet " +
                "where { {\n" +
                "    BIND(<http://openbiodiv.net/" + normalizedTaxonId + "> AS ?id). \n" +
                "     ?id <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openbiodiv.net/TaxonomicNameUsage>.\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/specificEpithet> ?specificEpithet.}\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/genus> ?genus.}\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/family> ?family.}\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/order> ?order. }\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/class> ?class. }\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/phylum> ?phylum.}\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/kingdom> ?kingdom.}\n" +
                "    { ?id <http://proton.semanticweb.org/protonkm#mentions> ?taxon.\n" +
                "      ?taxon <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openbiodiv.net/ScientificName>.\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/verbatimTaxonRank> ?rank.}\n" +
                "      ?taxon <http://www.w3.org/2000/01/rdf-schema#label> ?name.\n" +
                "   } " +
                "   UNION\n" +
                "    { ?id <http://proton.semanticweb.org/protonkm#mentions> ?btaxon.\n" +
                "      ?btaxon <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openbiodiv.net/ScientificName>.\n" +
                "      ?btaxon <http://openbiodiv.net/hasGbifTaxon> ?taxon.\n" +
                "      OPTIONAL { ?taxon <http://rs.tdwg.org/dwc/terms/taxonRank> ?rank.}\n" +
                "      ?btaxon <http://www.w3.org/2000/01/rdf-schema#label> ?name.\n" +
                "   }" +
                "  } " +
                "}";

        final LabeledCSVParser parser = sparqlClient.query(sparql);
        Taxon taxon = null;
        while ((taxon == null
                || StringUtils.isBlank(taxon.getPathNames()))
                && parser.getLine() != null) {
            taxon = parseTaxon(parser);
        }
        return taxon;
    }

    private static TaxonImpl parseTaxon(LabeledCSVParser parser) {
        final String name = parser.getValueByLabel("name");
        final String rank = StringUtils.defaultIfBlank(parser.getValueByLabel("rank"), null);
        final String id = parser.getValueByLabel("id");
        final TaxonImpl taxon = new TaxonImpl(name, id);
        taxon.setRank(rank);

        Map<String, String> nameMap = new LinkedHashMap<>();
        nameMap.put(rank, name);
        Arrays.asList("specificEpithet", "genus", "family", "order", "class", "phylum", "kingdom")
                .forEach(x -> add(parser, nameMap, x));

        final String path = TaxonUtil.generateTaxonPath(nameMap);
        taxon.setPath(StringUtils.defaultIfBlank(path, name));
        taxon.setPathNames(TaxonUtil.generateTaxonPathNames(nameMap));
        return taxon;
    }

    public static void add(LabeledCSVParser parser, Map<String, String> nameMap, String rankName) {
        final String valueByLabel = parser.getValueByLabel(rankName);
        if (StringUtils.isNotBlank(valueByLabel)) {
            nameMap.put(rankName, valueByLabel);
        }
    }


}

package org.eol.globi.taxon;

import org.eol.globi.Version;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.service.TaxonUtil;
import org.eol.globi.util.CSVTSVUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NameTool {

    public static void main(String[] args) {
        PropertyEnricher taxonEnricher = PropertyEnricherFactory.createTaxonEnricher();
        try {
            System.err.println(Version.getVersionInfo(TaxonIdLookup.class));
            resolve(System.in, System.out, false, taxonEnricher);
            System.exit(0);
        } catch (IOException | PropertyEnricherException e) {
            System.err.println("failed to resolve taxon: [" + e.getMessage() + "]");
            System.exit(1);
        } finally {
            taxonEnricher.shutdown();
        }

    }

    static void resolve(InputStream is, OutputStream os, boolean shouldReplace, PropertyEnricher enricher) throws IOException, PropertyEnricherException {
        PrintStream p = new PrintStream(os);

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] row = line.split("\t");

            Map<String, String> enriched = enricher.enrich(TaxonUtil.taxonToMap(asTaxon(row)));

            Taxon taxon = TaxonUtil.mapToTaxon(enriched);

            Stream<String> provided = Stream.of(row);

            Stream<String> resolved = Stream.of(taxon.getExternalId(), taxon.getName(), taxon.getRank(), taxon.getCommonNames(), taxon.getPath(), taxon.getPathIds(), taxon.getPathNames(), taxon.getExternalUrl(), taxon.getThumbnailUrl(), taxon.getNameSource(), taxon.getNameSourceURL(), taxon.getNameSourceAccessedAt());

            Stream<String> combined = shouldReplace
                    ? Stream.concat(resolved.limit(2), provided.skip(2))
                    : Stream.concat(provided, resolved);

            p.println(CSVTSVUtil.mapEscapedValues(combined)
                    .collect(Collectors.joining("\t"))
            );
        }
        p.flush();
    }

    private static Taxon asTaxon(String[] row) {
        Taxon taxon;
        if (row.length == 1) {
            taxon = new TaxonImpl(null, row[0]);
        } else if (row.length > 1) {
            taxon = new TaxonImpl(row[1], row[0]);
        } else {
            taxon = new TaxonImpl("", "");
        }
        return taxon;
    }


}

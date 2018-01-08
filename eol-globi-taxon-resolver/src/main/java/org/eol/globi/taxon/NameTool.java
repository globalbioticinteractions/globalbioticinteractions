package org.eol.globi.taxon;

import org.apache.commons.lang.StringUtils;
import org.eol.globi.Version;
import org.eol.globi.domain.NameType;
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
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NameTool {

    private static final String DEPOT_PREFIX = "https://depot.globalbioticinteractions.org/snapshot/target/data/taxa/";
    private final static String TAXON_CACHE_DEFAULT_URL = DEPOT_PREFIX + "taxonCache.tsv.gz";
    private final static String TAXON_MAP_DEFAULT_URL = DEPOT_PREFIX +"taxonMap.tsv.gz";

    public static void main(String[] args) {
        TaxonCacheService cacheService = null;
        try {
            String taxonCacheURI = StringUtils.defaultIfBlank(args.length > 0 ? args[0] : "", TAXON_CACHE_DEFAULT_URL);
            String taxonMapURI = StringUtils.defaultIfBlank(args.length > 1 ? args[1] : "", TAXON_MAP_DEFAULT_URL);
            cacheService = new TaxonCacheService(taxonCacheURI, taxonMapURI);
            cacheService.setTemporary(false);

            System.err.println(Version.getVersionInfo(NameTool.class));
            boolean shouldReplace = false;
            TermMatcher termMatcher = new GlobalNamesService(Arrays.asList(GlobalNamesSources.values()));
            termMatcher = PropertyEnricherFactory.createTaxonMatcher();
            termMatcher = cacheService;
            resolve(System.in, new TermMatchingRowHandler(shouldReplace, System.out, termMatcher));
            System.exit(0);
        } catch (IOException | PropertyEnricherException e) {
            System.err.println("failed to resolve taxon: [" + e.getMessage() + "]");
            System.exit(1);
        } finally {
            if (cacheService != null) {
                cacheService.shutdown();
            }
        }
    }

    static void resolve(InputStream is, RowHandler rowHandler) throws IOException, PropertyEnricherException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        long counter = 0;
        while ((line = reader.readLine()) != null) {
            String[] row = line.split("\t");
            rowHandler.onRow(row);
            counter++;
            if (counter % 25 == 0) {
                System.err.print(".");
            }
            if (counter % (25 * 50) == 0) {
                System.err.println();
            }
        }
    }

    static Taxon resolveTaxon(PropertyEnricher enricher, Taxon taxonProvided) throws PropertyEnricherException {
        Map<String, String> enriched = enricher.enrich(TaxonUtil.taxonToMap(taxonProvided));
        return TaxonUtil.mapToTaxon(enriched);
    }

    static Taxon asTaxon(String[] row) {
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


    static class ResolvingRowHandler implements RowHandler {
        private final PropertyEnricher enricher;
        private final boolean shouldReplace;
        private final PrintStream p;

        public ResolvingRowHandler(boolean shouldReplace, OutputStream os, PropertyEnricher enricher) {
            this.enricher = enricher;
            this.shouldReplace = shouldReplace;
            this.p = new PrintStream(os);
        }

        @Override
        public void onRow(String[] row) throws PropertyEnricherException {
            Stream<Taxon> resolvedTaxa = Stream.of(resolveTaxon(enricher, asTaxon(row)));
            linesForTaxa(row,
                    resolvedTaxa,
                    shouldReplace,
                    p,
                    taxon -> TaxonUtil.isResolved(taxon) ? NameType.SAME_AS : NameType.NONE);
        }

    }

    interface NameTypeOf {
        NameType nameTypeOf(Taxon taxon);
    }

    public static void linesForTaxa(String[] row, Stream<Taxon> resolvedTaxa, boolean shouldReplace, PrintStream p, NameTypeOf nameTypeOf) {
        Stream<String> provided = Stream.of(row);

        Stream<Stream<String>> lines = resolvedTaxa.map(taxon -> Stream.of(
                nameTypeOf.nameTypeOf(taxon).name(),
                taxon.getExternalId(), taxon.getName(),
                taxon.getRank(),
                taxon.getCommonNames(),
                taxon.getPath(),
                taxon.getPathIds(),
                taxon.getPathNames(),
                taxon.getExternalUrl(),
                taxon.getThumbnailUrl(),
                taxon.getNameSource(),
                taxon.getNameSourceURL(),
                taxon.getNameSourceAccessedAt()))
                .map(resolved -> shouldReplace
                        ? Stream.concat(resolved.skip(1).limit(2), provided.skip(2))
                        : Stream.concat(provided, resolved));

        lines.map(combinedLine -> CSVTSVUtil.mapEscapedValues(combinedLine)
                .collect(Collectors.joining("\t")))
                .forEach(p::println);
    }

    static class TermMatchingRowHandler implements RowHandler {
        private final boolean shouldReplace;
        private final PrintStream p;
        private TermMatcher termMatcher;

        public TermMatchingRowHandler(boolean shouldReplace, OutputStream os, TermMatcher termMatcher) {
            this.shouldReplace = shouldReplace;
            this.p = new PrintStream(os);
            this.termMatcher = termMatcher;
        }

        @Override
        public void onRow(final String[] row) throws PropertyEnricherException {
            Taxon taxonProvided = asTaxon(row);
            termMatcher.findTerms(Arrays.asList(taxonProvided), new TermMatchListener() {
                @Override
                public void foundTaxonForName(Long id, String name, Taxon taxon, NameType nameType) {
                    Taxon taxonWithServiceInfo = (TaxonUtil.mapToTaxon(TaxonUtil.appendNameSourceInfo(TaxonUtil.taxonToMap(taxon), termMatcher.getClass(), new Date())));
                    linesForTaxa(row, Stream.of(taxonWithServiceInfo), shouldReplace, p, taxon1 -> nameType);
                }
            });
        }
    }
}

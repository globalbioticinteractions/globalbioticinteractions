package org.eol.globi.taxon;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.CSVTSVUtil;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

public class TermResources {
    public static TermResource<Triple<Taxon, NameType, Taxon>> defaultTaxonMapResource(String taxonMapResource) {
        return new TermResource<Triple<Taxon, NameType, Taxon>>() {

            @Override
            public String getResource() {
                return taxonMapResource;
            }

            @Override
            public Function<String, Triple<Taxon, NameType, Taxon>> getParser() {
                return line -> {
                    String[] strings = CSVTSVUtil.splitTSV(line);
                    Taxon provided = TaxonMapParser.parseProvidedTaxon(strings);
                    Taxon resolved = TaxonMapParser.parseResolvedTaxon(strings);
                    return new ImmutableTriple<>(provided, NameType.SAME_AS, resolved);
                };
            }

            @Override
            public Predicate<String> getValidator() {
                return Objects::nonNull;
            }
        };
    }

    public static TermResource<Taxon> defaultTaxonCacheResource(String termResource) {
        return new TermResource<Taxon>() {

            @Override
            public String getResource() {
                return termResource;
            }

            @Override
            public Function<String, Taxon> getParser() {
                return TaxonCacheParser::parseLine;
            }

            @Override
            public Predicate<String> getValidator() {
                return ((Predicate<String>) Objects::nonNull)
                        .and(line1 -> {
                            final Taxon taxon = getParser().apply(line1);
                            return !StringUtils.isBlank(taxon.getPath());
                        });
            }
        };
    }
}

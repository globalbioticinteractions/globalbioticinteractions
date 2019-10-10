package org.eol.globi.server.util;

import org.eol.globi.util.CypherQuery;
import org.hamcrest.core.Is;
import org.junit.Test;
import org.springframework.http.MediaType;

import java.util.stream.Stream;

import static org.junit.Assert.assertThat;

public class CypherHttpMessageConverterTest {

    @Test
    public void optimizeQuerySVG() {
        CypherQuery cypherQuery = CypherHttpMessageConverter
                .optimizeQueryForType(new CypherQuery("bla bal limit 10"), new ResultFormatterSVG());
        assertThat(cypherQuery.getQuery(), Is.is ("bla bal limit 1"));
    }

    @Test
    public void optimizeQuerySVG2() {
        CypherQuery cypherQuery = CypherHttpMessageConverter
                .optimizeQueryForType(new CypherQuery("bla bal LIMIT 10"), new ResultFormatterSVG());
        assertThat(cypherQuery.getQuery(), Is.is ("bla bal LIMIT 1"));
    }

    @Test
    public void optimizeQueryNonSVG() {
        CypherHttpMessageConverter cypherHttpMessageConverter = new CypherHttpMessageConverter();
        Stream<MediaType> nonSvg = cypherHttpMessageConverter
                .getSupportedMediaTypes()
                .stream()
                .filter(x -> !x.getSubtype().contains("svg+xml"));

        nonSvg.forEach(x -> {
            CypherQuery cypherQuery = CypherHttpMessageConverter
                    .optimizeQueryForType(new CypherQuery("bla bal LIMIT 10"), new ResultFormatterFactory().create(x));
            assertThat(cypherQuery.getQuery(), Is.is ("bla bal LIMIT 10"));
        });
    }

}
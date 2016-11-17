package org.eol.globi.server;

import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class QueryTypeTest {

    @Test
    public void withNumberOfStudiesAccordingTo() {
        QueryType actualType = QueryType.forParams(new HashMap<String, String[]>() {{
            put("accordingTo", new String[]{"someSource"});
            put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_studies"});
        }});
        assertThat(actualType, is(QueryType.MULTI_TAXON_DISTINCT));
    }

    @Test
    public void withNumberOfStudies() {
        QueryType actualType = QueryType.forParams(new HashMap<String, String[]>() {{
            put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_studies"});
        }});
        assertThat(actualType, is(QueryType.MULTI_TAXON_DISTINCT));
    }

    @Test
    public void withObservations() {
        QueryType actualType = QueryType.forParams(new HashMap<String, String[]>() {{
            put("field", new String[]{"source_taxon_name", "target_taxon_name", "number_of_studies"});
            put("includeObservations", new String[]{"t"});
        }});
        assertThat(actualType, is(QueryType.MULTI_TAXON_ALL));
    }

    @Test
    public void byNameOnly() {
        QueryType actualType = QueryType.forParams(new HashMap<String, String[]>() {{
            put("field", new String[]{"source_taxon_name", "target_taxon_name"});
            put("includeObservations", new String[]{"source_taxon_name", "target_taxon_name"});
        }});
        assertThat(actualType, is(QueryType.MULTI_TAXON_DISTINCT_BY_NAME_ONLY));
    }

}
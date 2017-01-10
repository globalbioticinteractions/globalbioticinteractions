package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ReportControllerTest {

    @Test
    public void distinctSource() throws IOException {
        CypherQuery source = new ReportController().sources("a source" , null, null);
        assertThat(source.getQuery(), is("START report = node:reports(source={source}) WHERE not(has(report.title)) AND has(report.source) RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, report.source? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match, report.sourceId? as study_source_id SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().get("source"), is("a source"));
    }

    @Test
    public void distinctSourceIds() throws IOException {
        CypherQuery source = new ReportController().sources("a source" , "a source id", null);
        assertThat(source.getQuery(), is("START report = node:reports(sourceId={source}) WHERE not(has(report.title)) AND has(report.source) RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, report.source? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match, report.sourceId? as study_source_id SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().get("source"), is("a source id"));
    }

    @Test
    public void sources() throws IOException {
        CypherQuery source = new ReportController().sources(null, null, null);
        assertThat(source.getQuery(), is("START report = node:reports('source:*') WHERE not(has(report.title)) AND has(report.source) RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, report.source? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match, report.sourceId? as study_source_id SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().size(), is(0));

    }

    @Test
    public void studiesForSource() throws IOException {
        CypherQuery source = new ReportController().studies("a source", null);
        assertThat(source.getQuery(), is("START report = node:reports(source={source}) WHERE has(report.title) RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, report.source as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().get("source"), is("a source"));
    }

    @Test
    public void studies() throws IOException {
        CypherQuery source = new ReportController().studies(null, null);
        assertThat(source.getQuery(), is("START report = node:reports('source:*') WHERE has(report.title) RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, report.source as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().size(), is(0));
    }

}
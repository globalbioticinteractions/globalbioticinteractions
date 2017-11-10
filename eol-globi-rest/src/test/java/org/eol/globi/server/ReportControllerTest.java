package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ReportControllerTest {

    @Test
    public void distinctSourceNoPrefix() throws IOException {
        CypherQuery source = new ReportController().sources("someSourceId", null);
        assertThat(source.getQuery(), is("START report = node:reports(sourceId={sourceId}) RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, report.source? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match, report.sourceId? as study_source_id SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().get("sourceId"), is("globi:someSourceId"));
    }

    @Test
    public void distinctSourcePrefix() throws IOException {
        CypherQuery source = new ReportController().sources("bla:someSourceId", null);
        assertThat(source.getQuery(), is("START report = node:reports(sourceId={sourceId}) RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, report.source? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match, report.sourceId? as study_source_id SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().get("sourceId"), is("bla:someSourceId"));
    }

    @Test
    public void distinctSourceOrgName() throws IOException {
        CypherQuery source = new ReportController().sourceOrgName("some", "name", null);
        assertThat(source.getQuery(), is("START dataset = node:datasets(namespace={namespace}), report = node:reports('sourceId:*') " +
                "WHERE ('globi:' + dataset.namespace) = report.sourceId RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, dataset.citation? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match, report.sourceId? as study_source_id, dataset.doi? as study_source_doi, dataset.format? as study_source_format, dataset.archiveURI? as study_source_archive_uri, dataset.lastSeenAt? as study_source_last_seen_at SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().get("namespace"), is("some/name"));
    }

    @Test
    public void distinctSourceOrg() throws IOException {
        CypherQuery source = new ReportController().sourceOrg("some", null);
        assertThat(source.getQuery(), is("START dataset = node:datasets(namespace={namespace}), report = node:reports('sourceId:*') " +
                "WHERE ('globi:' + dataset.namespace) = report.sourceId " +
                "RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, dataset.citation? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match, report.sourceId? as study_source_id, dataset.doi? as study_source_doi, dataset.format? as study_source_format, dataset.archiveURI? as study_source_archive_uri, dataset.lastSeenAt? as study_source_last_seen_at SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().get("namespace"), is("some"));
    }

    @Test
    public void distinctSourceRoot() throws IOException {
        CypherQuery source = new ReportController().sourceRoot(null);
        assertThat(source.getQuery(), is("START dataset = node:datasets('namespace:*'), report = node:reports('sourceId:*') " +
                "WHERE ('globi:' + dataset.namespace) = report.sourceId " +
                "RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, dataset.citation? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match, report.sourceId? as study_source_id, dataset.doi? as study_source_doi, dataset.format? as study_source_format, dataset.archiveURI? as study_source_archive_uri, dataset.lastSeenAt? as study_source_last_seen_at SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().size(), is(0));
    }

    @Test
    public void sources() throws IOException {
        CypherQuery source = new ReportController().sources(null, null);
        assertThat(source.getQuery(), is("START report = node:reports('sourceId:*') RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, report.source? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match, report.sourceId? as study_source_id SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().size(), is(0));
    }

    @Test
    public void studiesForSource() throws IOException {
        CypherQuery source = new ReportController().studies("a source", null);
        assertThat(source.getQuery(), is("START report = node:reports(source={source}) WHERE has(report.title) RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, report.source? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().get("source"), is("a source"));
    }

    @Test
    public void studies() throws IOException {
        CypherQuery source = new ReportController().studies(null, null);
        assertThat(source.getQuery(), is("START report = node:reports('source:*') WHERE has(report.title) RETURN report.citation? as study_citation, report.externalId? as study_url, report.doi? as study_doi, report.source? as study_source_citation, report.nInteractions as number_of_interactions, report.nTaxa as number_of_distinct_taxa, report.nStudies? as number_of_studies, report.nSources? as number_of_sources, report.nTaxaNoMatch? as number_of_distinct_taxa_no_match SKIP 0 LIMIT 1024"));
        assertThat(source.getParams().size(), is(0));
    }

}
package org.eol.globi.server;

import org.eol.globi.util.CypherQuery;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ReportControllerTest {

    public static final String CYPHER_VERSION = "CYPHER 2.3 ";

    @Test
    public void distinctSourceNoPrefix() throws IOException {
        CypherQuery source = new ReportController().sources("someSourceId", null);
        assertThat(source.getVersionedQuery(), is(CYPHER_VERSION +
                "START report = node:reports(sourceId={sourceId}) " +
                "RETURN report.citation as study_citation, " +
                "report.externalId as study_url, " +
                "report.doi as study_doi, " +
                "null as study_source_citation, " +
                "report.nInteractions as number_of_interactions, " +
                "report.nTaxa as number_of_distinct_taxa, " +
                "report.nStudies as number_of_studies, " +
                "report.nSources as number_of_sources, " +
                "report.nTaxaNoMatch as number_of_distinct_taxa_no_match, " +
                "report.sourceId as study_source_id " +
                "SKIP 0 " +
                "LIMIT 1024"));
        assertThat(source.getParams().get("sourceId"), is("globi:someSourceId"));
        CypherTestUtil.validate(source);
    }

    @Test
    public void distinctSourcePrefix() throws IOException {
        CypherQuery source = new ReportController().sources("bla:someSourceId", null);
        assertThat(source.getVersionedQuery(), is(CYPHER_VERSION +
                "START report = node:reports(sourceId={sourceId}) " +
                "RETURN report.citation as study_citation, " +
                "report.externalId as study_url, " +
                "report.doi as study_doi, " +
                "null as study_source_citation, " +
                "report.nInteractions as number_of_interactions, " +
                "report.nTaxa as number_of_distinct_taxa, " +
                "report.nStudies as number_of_studies, " +
                "report.nSources as number_of_sources, " +
                "report.nTaxaNoMatch as number_of_distinct_taxa_no_match, " +
                "report.sourceId as study_source_id " +
                "SKIP 0 " +
                "LIMIT 1024"));
        assertThat(source.getParams().get("sourceId"), is("bla:someSourceId"));
        CypherTestUtil.validate(source);
    }

    @Test
    public void distinctSourceOrgName() throws IOException {
        CypherQuery source = new ReportController().sourceOrgName("some", "name", null);
        assertThat(source.getVersionedQuery(), is(CYPHER_VERSION +
                "START " +
                "dataset = node:datasets(namespace={namespace}), " +
                "report = node:reports('sourceId:*') " +
                "WHERE ('globi:' + dataset.namespace) = report.sourceId " +
                "RETURN report.citation as study_citation, " +
                "report.externalId as study_url, " +
                "report.doi as study_doi, " +
                "dataset.citation as study_source_citation, " +
                "report.nInteractions as number_of_interactions, " +
                "report.nTaxa as number_of_distinct_taxa, " +
                "report.nStudies as number_of_studies, " +
                "report.nSources as number_of_sources, " +
                "report.nTaxaNoMatch as number_of_distinct_taxa_no_match, " +
                "report.sourceId as study_source_id, " +
                "dataset.doi as study_source_doi, " +
                "dataset.format as study_source_format, " +
                "dataset.archiveURI as study_source_archive_uri, " +
                "dataset.lastSeenAt as study_source_last_seen_at " +
                "SKIP 0 " +
                "LIMIT 1024"));
        assertThat(source.getParams().get("namespace"), is("some/name"));
        CypherTestUtil.validate(source);

    }

    @Test
    public void collections() throws IOException {
        CypherQuery source = new ReportController().collections();
        assertThat(source.getVersionedQuery(), is(CYPHER_VERSION +
                "START " +
                "report = node:reports('collection:*') " +
                "WHERE " +
                "not(exists(report.title)) " +
                "RETURN " +
                "null as study_citation, " +
                "null as study_url, " +
                "null as study_doi, " +
                "null as study_source_citation, " +
                "report.nInteractions as number_of_interactions, " +
                "report.nTaxa as number_of_distinct_taxa, " +
                "report.nStudies as number_of_studies, " +
                "report.nSources as number_of_sources, " +
                "report.nTaxaNoMatch as number_of_distinct_taxa_no_match"));

        CypherTestUtil.validate(source);

    }

    @Test
    public void distinctSourceOrg() throws IOException {
        CypherQuery source = new ReportController().sourceOrg("some", null);
        assertThat(source.getVersionedQuery(), is(CYPHER_VERSION +
                "START dataset = node:datasets(namespace={namespace}), report = node:reports('sourceId:*') " +
                "WHERE ('globi:' + dataset.namespace) = report.sourceId " +
                "RETURN report.citation as study_citation, " +
                "report.externalId as study_url, " +
                "report.doi as study_doi, " +
                "dataset.citation as study_source_citation, " +
                "report.nInteractions as number_of_interactions, " +
                "report.nTaxa as number_of_distinct_taxa, " +
                "report.nStudies as number_of_studies, " +
                "report.nSources as number_of_sources, " +
                "report.nTaxaNoMatch as number_of_distinct_taxa_no_match, " +
                "report.sourceId as study_source_id, " +
                "dataset.doi as study_source_doi, " +
                "dataset.format as study_source_format, " +
                "dataset.archiveURI as study_source_archive_uri, " +
                "dataset.lastSeenAt as study_source_last_seen_at " +
                "SKIP 0 " +
                "LIMIT 1024"));
        assertThat(source.getParams().get("namespace"), is("some"));
        CypherTestUtil.validate(source);

    }

    @Test
    public void distinctSourceRoot() throws IOException {
        CypherQuery source = new ReportController().sourceRoot(null);
        assertThat(source.getVersionedQuery(), is(CYPHER_VERSION +
                "START dataset = node:datasets('namespace:*'), report = node:reports('sourceId:*') " +
                "WHERE ('globi:' + dataset.namespace) = report.sourceId " +
                "RETURN report.citation as study_citation, " +
                "report.externalId as study_url, " +
                "report.doi as study_doi, " +
                "dataset.citation as study_source_citation, " +
                "report.nInteractions as number_of_interactions, " +
                "report.nTaxa as number_of_distinct_taxa, " +
                "report.nStudies as number_of_studies, " +
                "report.nSources as number_of_sources, " +
                "report.nTaxaNoMatch as number_of_distinct_taxa_no_match, " +
                "report.sourceId as study_source_id, " +
                "dataset.doi as study_source_doi, " +
                "dataset.format as study_source_format, " +
                "dataset.archiveURI as study_source_archive_uri, " +
                "dataset.lastSeenAt as study_source_last_seen_at " +
                "SKIP 0 " +
                "LIMIT 1024"));
        assertThat(source.getParams().size(), is(0));
        CypherTestUtil.validate(source);

    }

    @Test
    public void sources() throws IOException {
        CypherQuery source = new ReportController().sources(null, null);
        assertThat(source.getVersionedQuery(), is(CYPHER_VERSION +
                "START report = node:reports('sourceId:*') " +
                "RETURN report.citation as study_citation, " +
                "report.externalId as study_url, " +
                "report.doi as study_doi, " +
                "null as study_source_citation, " +
                "report.nInteractions as number_of_interactions, " +
                "report.nTaxa as number_of_distinct_taxa, " +
                "report.nStudies as number_of_studies, " +
                "report.nSources as number_of_sources, " +
                "report.nTaxaNoMatch as number_of_distinct_taxa_no_match, " +
                "report.sourceId as study_source_id " +
                "SKIP 0 " +
                "LIMIT 1024"));
        assertThat(source.getParams().size(), is(0));
        CypherTestUtil.validate(source);
    }

    @Test
    public void studiesForSource() throws IOException {
        CypherQuery source = new ReportController().studies("a source", null);
        assertThat(source.getVersionedQuery(), is(CYPHER_VERSION +
                "START report = node:reports(source={source}) " +
                "WHERE exists(report.title) " +
                "RETURN report.citation as study_citation, " +
                "report.externalId as study_url, " +
                "report.doi as study_doi, " +
                "report.source as study_source_citation, " +
                "report.nInteractions as number_of_interactions, " +
                "report.nTaxa as number_of_distinct_taxa, " +
                "report.nStudies as number_of_studies, " +
                "report.nSources as number_of_sources, " +
                "report.nTaxaNoMatch as number_of_distinct_taxa_no_match " +
                "SKIP 0 " +
                "LIMIT 1024"));
        assertThat(source.getParams().get("source"), is("a source"));
        CypherTestUtil.validate(source);
    }

    @Test
    public void studies() throws IOException {
        CypherQuery source = new ReportController().studies(null, null);
        assertThat(source.getVersionedQuery(), is(CYPHER_VERSION +
                "START report = node:reports('source:*') " +
                "WHERE exists(report.title) " +
                "RETURN report.citation as study_citation, " +
                "report.externalId as study_url, " +
                "report.doi as study_doi, " +
                "report.source as study_source_citation, " +
                "report.nInteractions as number_of_interactions, " +
                "report.nTaxa as number_of_distinct_taxa, " +
                "report.nStudies as number_of_studies, " +
                "report.nSources as number_of_sources, " +
                "report.nTaxaNoMatch as number_of_distinct_taxa_no_match " +
                "SKIP 0 " +
                "LIMIT 1024"));
        assertThat(source.getParams().size(), is(0));
        CypherTestUtil.validate(source);
    }

}
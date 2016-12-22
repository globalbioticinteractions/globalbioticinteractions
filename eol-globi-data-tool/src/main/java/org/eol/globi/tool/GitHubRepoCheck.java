package org.eol.globi.tool;

import org.apache.commons.io.FileUtils;
import org.eol.globi.data.NodeFactoryImpl;
import org.eol.globi.data.ParserFactoryImpl;
import org.eol.globi.data.StudyImporter;
import org.eol.globi.data.StudyImporterException;
import org.eol.globi.db.GraphService;
import org.eol.globi.service.DatasetFactory;
import org.eol.globi.service.DatasetFinderGitHubRemote;
import org.eol.globi.service.GitHubImporterFactory;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

public class GitHubRepoCheck {

    public static void main(final String[] args) throws IOException, URISyntaxException, StudyImporterException {
        final File tmpDir = File.createTempFile("graph", ".db", FileUtils.getTempDirectory());
        try {
            final GraphDatabaseService graphService = GraphService.getGraphService(tmpDir.getAbsolutePath());
            final String repoName = args[0];
            final DatasetFinderGitHubRemote datasetFinderGitHubRemote = new DatasetFinderGitHubRemote();
            final StudyImporter importer = new GitHubImporterFactory().createImporter(DatasetFactory.datasetFor(repoName, datasetFinderGitHubRemote), new ParserFactoryImpl(), new NodeFactoryImpl(graphService));
            importer.importStudy();
            new NameResolver(graphService).resolve();

            final ExecutionResult execute = new ExecutionEngine(graphService).execute("START study = node:studies('*:*') " +
                    "MATCH study-[:COLLECTED]->specimen-[:ORIGINALLY_DESCRIBED_AS]->origTaxon" +
                    ", specimen-[inter]->otherSpecimen-[:ORIGINALLY_DESCRIBED_AS]->otherOrigTaxon" +
                    ", specimen-[:CLASSIFIED_AS]->taxon" +
                    ", otherSpecimen-[:CLASSIFIED_AS]->otherTaxon " +
                    " WHERE not(has(inter.inverted)) " +
                    "RETURN origTaxon.name as providedSourceTaxonName, origTaxon.externalId? as providedSourceTaxonId" +
                    ", taxon.name as resolvedSourceTaxonName, taxon.externalId? as resolvedSourceTaxonId" +
                    ", inter.iri as interactionTypeId, inter.label as interactionTypeLabel " +
                    ", otherOrigTaxon.name as providedTargetTaxonName, otherOrigTaxon.externalId? as providedTargetTaxonId" +
                    ", otherTaxon.name as resolvedTargetTaxonName, otherTaxon.externalId? as resolvedTargetTaxonId" +
                    ", study.citation, study.source");
            System.out.println(execute.dumpToString());
        } finally {
            FileUtils.deleteQuietly(tmpDir);
        }
    }
}

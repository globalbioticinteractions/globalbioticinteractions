package org.eol.globi.data;

import org.eol.globi.domain.Study;
import org.eol.globi.geo.LatLng;
import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.Relationship;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;
import static org.junit.matchers.JUnitMatchers.containsString;

public class StudyImporterForDunneTest extends GraphDBTestCase {

    @Test
    public void importStudy() throws StudyImporterException {
        StudyImporterForDunne importer = new StudyImporterForDunne(new ParserFactoryImpl(), nodeFactory);

        importer.setLinkResource("https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakIntertidal/master/links.tsv");
        importer.setNodeResource("https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakIntertidal/master/nodes.tsv");
        importer.setSourceCitation("blabla");
        importer.setNamespace("dunne2016");
        importer.setLocation(new LatLng(60, 60));
        Study study = importStudy(importer);

        assertThat(study, is(notNullValue()));

        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }
        assertThat(count, is(1804 * 2));

    }

    @Test
    public void importStudy2() throws StudyImporterException {
        StudyImporterForDunne importer = new StudyImporterForDunne(new ParserFactoryImpl(), nodeFactory);

        importer.setLinkResource("https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakNearshore/master/links.tsv");
        importer.setNodeResource("https://raw.githubusercontent.com/globalbioticinteractions/dunne2016SanakNearshore/master/nodes.tsv");
        importer.setSourceCitation("blabla");
        importer.setNamespace("dunne2016");
        importer.setLocation(new LatLng(60, 60));
        Study study = importStudy(importer);

        assertThat(study, is(notNullValue()));

        assertThat(study.getSource(), containsString("Accessed at "));

        Iterable<Relationship> specimens = study.getSpecimens();
        int count = 0;
        for (Relationship specimen : specimens) {
            count++;
        }
        assertThat(count, is(6774 * 2));

    }

}
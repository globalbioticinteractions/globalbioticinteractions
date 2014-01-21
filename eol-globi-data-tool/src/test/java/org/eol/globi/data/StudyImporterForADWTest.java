package org.eol.globi.data;

import com.healthmarketscience.jackcess.Column;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.JetFormat;
import com.healthmarketscience.jackcess.Table;
import com.hp.hpl.jena.rdf.model.impl.RDFDefaultErrorHandler;
import org.apache.commons.collections.CollectionUtils;
import org.eol.globi.domain.Environment;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.Term;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.GeoNamesServiceImpl;
import org.junit.Test;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import uk.me.jstott.jcoord.LatLng;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItem;

/**
 * Note that Animal Diversity Web data tested below is currently assumed to be part of SPIRE.
 */

public class StudyImporterForADWTest extends GraphDBTestCase {

    @Test
    public void readMDB() throws URISyntaxException, IOException {
        URI uri = getClass().getResource("spire/econetvis.mdb").toURI();
        assertThat(uri, is(notNullValue()));
        Database db = Database.open(new File(uri), true);
        assertThat(db.getFileFormat().getJetFormat(), is(JetFormat.VERSION_4));

        String[] tableNames = new String[]{
                "attribute_types",
                "common_names",
                "entities",
                "habitats",
                "links",
                "localities",
                "metastudies",
                "part_mapping_new",
                "part_qualifiers",
                "studies",
                "study_habitat",
                "study_local",
                "taxon",
                "taxon_attributes"

        };
        Set<String> expectedSet = new HashSet<String>();
        Collections.addAll(expectedSet, tableNames);

        Set<String> actualTableNames = db.getTableNames();
        assertThat(actualTableNames.size(), is(not(0)));
        assertThat("expected tables names [" + Arrays.toString(tableNames) + "] to be present",
                CollectionUtils.subtract(expectedSet, actualTableNames).size(), is(0));

        Table studies = db.getTable("studies");
        for (Map<String, Object> study : studies) {
            assertNotNull(study.get("reference"));
        }

        List<String> expectedColumnNames = Arrays.asList("study_id", "entity1", "entity2", "link_strength", "link_type", "table_ref", "link_number");
        assertColumnNames(expectedColumnNames, db.getTable("links"));

        expectedColumnNames = Arrays.asList("id", "latinname", "commonname", "parent", "webinfo", "moreinfo", "numchildren", "pos", "classification", "pictures", "sounds", "specimens", "idx", "extinct", "rank");
        assertColumnNames(expectedColumnNames, db.getTable("taxon"));

        Table taxonTable = db.getTable("taxon");
        int numberOfTaxa = 0;
        while (taxonTable.getNextRow() != null) {
            numberOfTaxa++;
        }

        assertThat(numberOfTaxa, is(198301));

        Table links = db.getTable("links");
        int numberOfLinks = 0;
        while (links.getNextRow() != null) {
            numberOfLinks++;
        }

        assertThat(numberOfLinks, is(18189));
    }

    private void assertColumnNames(List<String> expectedColumnNames, Table table) throws IOException {
        Table links = table;
        List<String> actualColumnNames = new ArrayList<String>();
        List<Column> columns = links.getColumns();
        for (Column column : columns) {
            actualColumnNames.add(column.getName());
        }

        assertThat(actualColumnNames, is(expectedColumnNames));
    }

}

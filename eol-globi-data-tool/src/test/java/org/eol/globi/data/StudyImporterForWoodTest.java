package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.Term;
import org.eol.globi.geo.LatLng;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.eol.globi.data.StudyImporterForTSV.*;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.StringContains.containsString;

public class StudyImporterForWoodTest extends GraphDBTestCase {

    @Test
    public void importFirst500() throws StudyImporterException, NodeFactoryException {
        StudyImporterForWood wood = new StudyImporterForWood(new ParserFactoryImpl(), nodeFactory);
        wood.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber < 500;
            }
        });
        importStudy(wood);

        assertThat(taxonIndex.findTaxonByName("Amphipoda"), is(notNullValue()));
    }

    @Test
    public void importLines() throws IOException, StudyImporterException {
        StudyImporterForWood wood = new StudyImporterForWood(new ParserFactoryImpl(), nodeFactory);
        wood.setLocation(new LatLng(54.42972, -162.70889));
        wood.setLocality(new Term("GEONAMES:5873327", "Sanak Island, Alaska, USA"));
        final List<Map<String, String>> maps = new ArrayList<Map<String, String>>();

        wood.importLinks(IOUtils.toInputStream(firstFewLines()), new InteractionListener() {

            @Override
            public void newLink(final Map<String, String> properties) {
                maps.add(properties);
            }
        }, null);
        resolveNames();
        assertThat(maps.size(), is(5));
        Map<String, String> firstLink = maps.get(0);
        assertThat(firstLink.get(StudyImporterForTSV.SOURCE_TAXON_ID), is("ITIS:93294"));
        assertThat(firstLink.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is("Amphipoda"));
        assertThat(firstLink.get(StudyImporterForTSV.TARGET_TAXON_ID), is("ITIS:10824"));
        assertThat(firstLink.get(StudyImporterForTSV.TARGET_TAXON_NAME), is("Pilayella littoralis"));
        assertStaticInfo(firstLink);

        Map<String, String> secondLink = maps.get(1);
        assertThat(secondLink.get(StudyImporterForTSV.SOURCE_TAXON_ID), is(nullValue()));
        assertThat(secondLink.get(StudyImporterForTSV.SOURCE_TAXON_NAME), is("Phytoplankton complex"));
        assertStaticInfo(secondLink);
    }

    protected void assertStaticInfo(Map<String, String> firstLink) {
        assertThat(firstLink.get(STUDY_SOURCE_CITATION), containsString("Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Data from: Effects of spatial scale of sampling on food web structure. Dryad Digital Repository. http://dx.doi.org/10.5061/dryad.g1qr6"));
        assertThat(firstLink.get(STUDY_SOURCE_CITATION), containsString(" Accessed at"));
        assertThat(firstLink.get(REFERENCE_CITATION), containsString("Wood SA, Russell R, Hanson D, Williams RJ, Dunne JA (2015) Data from: Effects of spatial scale of sampling on food web structure. Dryad Digital Repository. http://dx.doi.org/10.5061/dryad.g1qr6"));
        assertThat(firstLink.get(REFERENCE_DOI), is("http://dx.doi.org/10.1002/ece3.1640"));
        assertThat(firstLink.get(REFERENCE_URL), is("http://dx.doi.org/10.1002/ece3.1640"));
        assertThat(firstLink.get(LOCALITY_NAME), is("Sanak Island, Alaska, USA"));
        assertThat(firstLink.get(LOCALITY_ID), is("GEONAMES:5873327"));
        assertThat(firstLink.get(DECIMAL_LONGITUDE), is("-162.70889"));
        assertThat(firstLink.get(DECIMAL_LATITUDE), is("54.42972"));
        assertThat(firstLink.get(INTERACTION_TYPE_ID), is("RO:0002439"));
        assertThat(firstLink.get(INTERACTION_TYPE_NAME), is("preysOn"));
    }

    private String firstFewLines() {
        return "\"WebID\",\"WebScale\",\"WebUnit\",\"PredTSN\",\"PreyTSN\",\"PredName\",\"PreyName\"\n" +
                "9,\"T\",\"22\",\"93294\",\"10824\",\"Amphipoda\",\"Pilayella littoralis\"\n" +
                "9,\"T\",\"22\",\"san267\",\"2286\",\"Phytoplankton complex\",\"Bacillariophyta\"\n" +
                "9,\"T\",\"22\",\"93294\",\"11334\",\"Amphipoda\",\"Fucus\"\n" +
                "9,\"T\",\"22\",\"70395\",\"11334\",\"Littorina\",\"Fucus\"\n" +
                "9,\"T\",\"22\",\"92283\",\"11334\",\"Sphaeromatidae\",\"Fucus\"\n";
    }
}
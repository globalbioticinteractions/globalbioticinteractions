package org.eol.globi.process;

import org.eol.globi.data.StudyImporterException;
import org.eol.globi.domain.LogContext;
import org.eol.globi.tool.NullImportLogger;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.IsCloseTo.closeTo;

public class VerbatimCoordinatesEnricherTest {

    @Test
    public void fromITM() throws StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();

        InteractionProcessor enricher = new VerbatimCoordinatesEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger());

        enricher.on(new TreeMap<String, String>() {{
            put("verbatimLatitude", "564522");
            put("verbatimLongitude", "551458");
            put("verbatimSRS", "epsg:2157");
        }});

        assertThat(received.size(), Is.is(1));

        Map<String, String> sample1 = received.get(0);
        assertThat(sample1.get("verbatimSRS"), is("epsg:2157"));

        assertThat(Double.parseDouble(sample1.get("decimalLatitude")),
                closeTo(51.83082239523039d, 0.05));
        assertThat(Double.parseDouble(sample1.get("decimalLongitude")),
                closeTo(-8.704270621630931d, 0.05));

        assertThat(sample1.get("geodeticDatum"), is("epsg:4326"));
    }

    @Test
    public void keepExisting() throws StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();

        InteractionProcessor enricher = new VerbatimCoordinatesEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger());

        enricher.on(new TreeMap<String, String>() {{
            put("verbatimLatitude", "564522");
            put("verbatimLongitude", "551458");
            put("verbatimSRS", "epsg:2157");
            put("decimalLatitude", "something");
            put("decimalLongitude", "something");
        }});

        assertThat(received.size(), Is.is(1));

        Map<String, String> sample1 = received.get(0);
        assertThat(sample1.get("verbatimSRS"), is("epsg:2157"));

        assertThat(sample1.get("decimalLatitude"), is("something"));

        assertThat(sample1.get("geodeticDatum"), nullValue());
    }

    @Test(expected = StudyImporterException.class)
    public void fromUnknownCoordinateSystem() throws StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();

        InteractionProcessor enricher = new VerbatimCoordinatesEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger());

        try {
            enricher.on(new TreeMap<String, String>() {{
                put("verbatimLatitude", "564522");
                put("verbatimLongitude", "551458");
                put("verbatimSRS", "epsg:66666666");
            }});
        } catch (StudyImporterException ex) {
            assertThat(ex.getMessage(), is("unsupported spatial reference system [epsg:66666666] found in [verbatimSRS]"));
            throw ex;
        }


    }

    @Test
    public void fromUndefinedCoordinateSystem() throws StudyImporterException {

        List<Map<String, String>> received = new ArrayList<>();
        List<String> msgs = new ArrayList<>();

        InteractionProcessor enricher = new VerbatimCoordinatesEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger() {
            @Override
            public void warn(LogContext ctx, String message) {
                msgs.add(message);
            }

        });

        enricher.on(new TreeMap<String, String>() {{
            put("verbatimLatitude", "564522");
            put("verbatimLongitude", "551458");
        }});

        assertThat(msgs.get(0), is("cannot interpret {verbatimLatitude,verbatimLongitude} [{564522,551458}] : no spatial reference system defined using [verbatimSRS]."));

    }

    @Test(expected = StudyImporterException.class)
    public void fromNonNumberLatLng() throws StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();

        InteractionProcessor enricher = new VerbatimCoordinatesEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger());

        try {
            enricher.on(new TreeMap<String, String>() {{
                put("verbatimLatitude", "XXX");
                put("verbatimLongitude", "551458");
                put("verbatimSRS", "epsg:4326");
            }});
        } catch (StudyImporterException ex) {
            assertThat(ex.getMessage(), is("expected a number for [verbatimLatitude], but found [XXX]"));
            throw ex;
        }


    }

    @Test
    public void ignoreNullLatLng() throws StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();

        InteractionProcessor enricher = new VerbatimCoordinatesEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger());

        enricher.on(new TreeMap<String, String>() {{
            put("verbatimLatitude", null);
            put("verbatimLongitude", "551458");
            put("verbatimSRS", "epsg:4326");
        }});

        assertThat(received.size(), Is.is(1));

        Map<String, String> sample1 = received.get(0);
        assertThat(sample1.get("decimalLongitude"), nullValue());

    }

}
package org.eol.globi.process;


import org.eol.globi.data.StudyImporterException;
import org.eol.globi.tool.NullImportLogger;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class EventDateEnricherTest {

    @Test
    public void enrichFromYear() throws StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();

        InteractionProcessor enricher = new EventDateEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger());

        enricher.on(new TreeMap<String, String>() {{
            put("http://rs.tdwg.org/dwc/terms/year", "2012");
        }});

        assertThat(received.size(), Is.is(1));

        Map<String, String> sample1 = received.get(0);
        assertThat(sample1.get("http://rs.tdwg.org/dwc/terms/eventDate"), is("2012"));

    }
    @Test
    public void enrichFromYearAndDayOfYear() throws StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();

        InteractionProcessor enricher = new EventDateEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger());

        enricher.on(new TreeMap<String, String>() {{
            put("http://rs.tdwg.org/dwc/terms/year", "2012");
            put("http://rs.tdwg.org/dwc/terms/dayOfYear", "42");
        }});

        assertThat(received.size(), Is.is(1));

        Map<String, String> sample1 = received.get(0);
        assertThat(sample1.get("http://rs.tdwg.org/dwc/terms/eventDate"), is("2012-042"));

    }
    @Test
    public void enrichFromMonth() throws StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();

        InteractionProcessor enricher = new EventDateEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger());

        enricher.on(new TreeMap<String, String>() {{
            put("http://rs.tdwg.org/dwc/terms/year", "2012");
            put("http://rs.tdwg.org/dwc/terms/month", "10");
        }});

        assertThat(received.size(), Is.is(1));

        Map<String, String> sample1 = received.get(0);
        assertThat(sample1.get("http://rs.tdwg.org/dwc/terms/eventDate"), is("2012-10"));

    }
    @Test
    public void enrichFromDay() throws StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();

        InteractionProcessor enricher = new EventDateEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger());

        enricher.on(new TreeMap<String, String>() {{
            put("http://rs.tdwg.org/dwc/terms/year", "2012");
            put("http://rs.tdwg.org/dwc/terms/month", "10");
            put("http://rs.tdwg.org/dwc/terms/day", "12");
        }});

        assertThat(received.size(), Is.is(1));

        Map<String, String> sample1 = received.get(0);
        assertThat(sample1.get("http://rs.tdwg.org/dwc/terms/eventDate"), is("2012-10-12"));

    }

    @Test
    public void enrichFromDayNoMonth() throws StudyImporterException {
        List<Map<String, String>> received = new ArrayList<>();

        InteractionProcessor enricher = new EventDateEnricher(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                received.add(interaction);
            }
        }, new NullImportLogger());

        enricher.on(new TreeMap<String, String>() {{
            put("http://rs.tdwg.org/dwc/terms/year", "2012");
            put("http://rs.tdwg.org/dwc/terms/day", "12");
        }});

        assertThat(received.size(), Is.is(1));

        Map<String, String> sample1 = received.get(0);
        assertThat(sample1.get("http://rs.tdwg.org/dwc/terms/eventDate"), is("2012--12"));

    }


}
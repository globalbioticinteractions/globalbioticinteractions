package org.trophic.graph.data;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.trophic.graph.domain.Study;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertThat;

public class StudyImporterForSPIRETest extends GraphDBTestCase {

    @Test
    public void importStudy() throws IOException, StudyImporterException {
        StudyImporterForSPIRE importer = new StudyImporterForSPIRE(null, nodeFactory);
        TestTrophicLinkListener listener = new TestTrophicLinkListener();
        importer.setTrophicLinkListener(listener);
        importer.importStudy();

        assertThat(listener.getCount(), Is.is(30196));
        assertThat("number of unique countries changed since this test was written", listener.countries.size(), Is.is(50));
    }


    private static class TestTrophicLinkListener implements TrophicLinkListener {
        public int getCount() {
            return count;
        }

        private int count = 0;
        Set<String> countries = new HashSet<String>();

        @Override
        public void newLink(Study study, String predatorName, String preyName, String country, String state, String locality) {
            if (country != null) {
                countries.add(country);
            }
            count++;
        }
    }


}

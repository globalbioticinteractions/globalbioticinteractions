package org.eol.globi.data;

import com.Ostermiller.util.CSVParse;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.util.ResourceUtil;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForMetaTableIT {

    @Test
    public void importAll() throws IOException, StudyImporterException {
        final List<Map<String, String>> links = new ArrayList<Map<String, String>>();
        final InteractionListener interactionListener = new InteractionListener() {

            @Override
            public void newLink(Map<String, String> properties) throws StudyImporterException {
                links.add(properties);
            }
        };

        final String resource = "https://raw.githubusercontent.com/globalbioticinteractions/AfricaTreeDatabase/master/globi.json";
        final InputStream inputStream = ResourceUtil.asInputStream(resource, null);
        final JsonNode config = new ObjectMapper().readTree(inputStream);

        List<String> columnNames = StudyImporterForMetaTable.columnNamesForMetaTable(config);
        assertThat(columnNames.size(), is(40));

        final CSVParse csvParse = StudyImporterForMetaTable.createCsvParser(config);

        StudyImporterForMetaTable.importAll(interactionListener, columnNames, csvParse);

        assertThat(links.size() > 0, is(true));

    }


}
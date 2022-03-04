package org.eol.globi.server.util;

import com.sun.tools.javac.tree.TreeMaker;
import org.apache.commons.io.IOUtils;
import org.eol.globi.server.InteractionController;
import org.eol.globi.server.ParamName;
import org.eol.globi.util.CypherQuery;
import org.eol.globi.util.CypherUtil;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ResultFormatterGraphStreamingIT {

    @Test
    public void toAriopsisFelisDietToGraphStreaming() throws IOException {
        CypherQuery q = InteractionController.createQuery("Ariopsis felis", "eats", "Animalia", Collections.emptyMap());

        String result = CypherUtil.executeRemote(q);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterGraphStreaming().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        String actual = IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name());

        assertThat(actual,
                is(ResultFormatterGraphStreamingTest.ensureLineFeedAndNewline("ariopsisFelisDiet.gs")));
    }

    @Test
    public void toAriopsisFelisDietIncludeObservationsToGraphStreaming() throws IOException {

        Map<String, String> params = new TreeMap<String, String>() {{
            put(ParamName.INCLUDE_OBSERVATIONS.getName(), "true");
            put("limit", "10");
        }};
        CypherQuery q = InteractionController.createQuery(
                "Enhydra lutris",
                "eats",
                "Animalia",
                params);

        String result = CypherUtil.executeRemote(q);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterGraphStreaming().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        String actual = IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name());

        assertThat(actual,
                is(ResultFormatterGraphStreamingTest.ensureLineFeedAndNewline("enhydraLutrisDietObservations.gs")));
    }

}
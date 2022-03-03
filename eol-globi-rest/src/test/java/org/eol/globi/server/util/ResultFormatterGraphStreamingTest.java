package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ResultFormatterGraphStreamingTest {

    @Test
    public void toGraphStreaming() throws IOException {
        String result = ResultFormatterJSONTestUtil.getNewInteractionResults();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterGraphStreaming().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        String actual = IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name());

        //IOUtils.write(actual, new FileOutputStream("/home/jorrit/proj/globi/eol-globi-data/eol-globi-rest/src/test/resources/org/eol/globi/server/util/example.gs"), StandardCharsets.UTF_8);

        assertThat(actual,
                is(IOUtils.toString(getClass().getResourceAsStream("example.gs"), StandardCharsets.UTF_8)));
    }


    @Test(expected = ResultFormattingException.class)
    public void throwOnErrorStreaming() throws IOException {
        String result = RequestHelperTest.getErrorResult();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterGraphStreaming().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

    }

}
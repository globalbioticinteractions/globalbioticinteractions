package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ResultFormatterJSONTest {

    @Test
    public void convertNewToOld() throws JsonProcessingException, ResultFormattingException {
        String result = ResultFormatterJSONv2Test.newTaxonQueryResult();

        String formatted = new ResultFormatterJSON().format(result);

        assertThat(formatted, is(ResultFormatterJSONv2Test.oldTaxonQueryResult()));


    }

    @Test(expected = ResultFormattingException.class)
    public void throwOnError() throws ResultFormattingException {
        String result = RequestHelperTest.getErrorResult();

        new ResultFormatterJSON().format(result);
    }

    @Test
    public void convertOldToOld() throws JsonProcessingException, ResultFormattingException {
        String result = ResultFormatterJSONv2Test.oldTaxonQueryResult();

        String formatted = new ResultFormatterJSON().format(result);

        assertThat(formatted, is(ResultFormatterJSONv2Test.oldTaxonQueryResult()));


    }

    @Test
    public void convertNewToOldStreaming() throws IOException {
        String result = ResultFormatterJSONv2Test.newTaxonQueryResult();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterJSON().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        assertThat(IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name()),
                is(ResultFormatterJSONv2Test.oldTaxonQueryResult()));


    }

    @Test(expected = ResultFormattingException.class)
    public void throwOnErrorStreaming() throws IOException {
        String result = RequestHelperTest.getErrorResult();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterJSON().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

    }

    @Test
    public void convertOldToOldStreaming() throws IOException {
        String result = ResultFormatterJSONv2Test.oldTaxonQueryResult();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterJSON().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        assertThat(IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name()),
                is(ResultFormatterJSONv2Test.oldTaxonQueryResult()));


    }

}
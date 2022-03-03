package org.eol.globi.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ResultFormatterJSONTest {

    @Test
    public void convertNewToOld() throws ResultFormattingException, JsonProcessingException {
        String result = ResultFormatterJSONTestUtil.newTaxonQueryResult();

        String formatted = new ResultFormatterJSON().format(result);

        assertThat(formatted, is(ResultFormatterJSONTestUtil.oldTaxonQueryResult()));


    }

    @Test(expected = ResultFormattingException.class)
    public void throwOnError() throws ResultFormattingException {
        String result = RequestHelperTest.getErrorResult();

        new ResultFormatterJSON().format(result);
    }

    @Test
    public void convertOldToOld() throws JsonProcessingException, ResultFormattingException {
        String result = ResultFormatterJSONTestUtil.oldTaxonQueryResult();

        String formatted = new ResultFormatterJSON().format(result);

        assertThat(formatted, is(ResultFormatterJSONTestUtil.oldTaxonQueryResult()));


    }

    @Test
    public void convertNewToOldStreaming() throws IOException {
        String result = ResultFormatterJSONTestUtil.newTaxonQueryResult();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterJSON().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        assertThat(IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name()),
                is(ResultFormatterJSONTestUtil.oldTaxonQueryResult()));


    }

    @Test
    public void convertNewInteractionToOldStreaming() throws IOException {
        String result = RequestHelperTest.getSuccessfulResult();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterJSON().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        assertThat(IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name()),
                is(new ObjectMapper().readTree("{\n" +
                        "  \"columns\" : [ \"study.citation\" ],\n" +
                        "  \"data\" : [ [ \"Severe acute respiratory syndrome coronavirus 2 isolate hCoV-19/Netherlands/Gelderland_68/2020 genome assembly, complete genome: monopartite\" ] ]\n" +
                        "}").toString()));


    }

    @Test(expected = ResultFormattingException.class)
    public void throwOnErrorStreaming() throws IOException {
        String result = RequestHelperTest.getErrorResult();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterJSON().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

    }

    @Test
    public void convertOldToOldStreaming() throws IOException {
        String result = ResultFormatterJSONTestUtil.oldTaxonQueryResult();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterJSON().format(IOUtils.toInputStream(result, StandardCharsets.UTF_8), os);

        assertThat(IOUtils.toString(os.toByteArray(), StandardCharsets.UTF_8.name()),
                is(ResultFormatterJSONTestUtil.oldTaxonQueryResult()));


    }

}
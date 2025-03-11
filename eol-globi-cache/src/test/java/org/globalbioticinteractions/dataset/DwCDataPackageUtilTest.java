package org.globalbioticinteractions.dataset;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eol.globi.service.ResourceService;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class DwCDataPackageUtilTest {

    @Test
    public void dataPackage() throws IOException, URISyntaxException {
        JsonNode jsonNode = DwCDataPackageUtil.datasetFor(new ResourceService() {
            @Override
            public InputStream retrieve(URI resourceName) throws IOException {
                String testResourceName = StringUtils.replace(resourceName.toString(), "http://rs.gbif.org/sandbox/experimental/data-packages/dwc-dp/0.1/table-schemas/", "dwc-dp-tuco/");
                InputStream is = getClass().getResourceAsStream(testResourceName.toString());
                assertNotNull("cannot find [" + testResourceName + "]", is);
                return is;
            }
        }, URI.create("dwc-dp-tuco/datapackage.json"));

        assertNotNull(jsonNode);

        String target = "/org/globalbioticinteractions/dataset/dwc-dp-tuco/globi-expected.json";
        URL resource = getClass().getResource(target);
        assertNotNull(resource);

        boolean overwrite = false;
        if (overwrite) {
            File file = new File(resource.toURI());
            String resourcePath = StringUtils.replace(file.getAbsolutePath(), "target/test-classes", "src/test/resources");
            System.out.println("updating test resource at [" + resourcePath + "]");
            IOUtils.copy(
                    IOUtils.toInputStream(jsonNode.toPrettyString(), StandardCharsets.UTF_8),
                    new FileOutputStream(new File(resourcePath)));
        }

        JsonNode expectedConfig = new ObjectMapper().readTree(
                getClass().getResourceAsStream("dwc-dp-tuco/globi-expected.json")
        );
        assertThat(jsonNode.toPrettyString(),
                Is.is(expectedConfig.toPrettyString()));

    }

}
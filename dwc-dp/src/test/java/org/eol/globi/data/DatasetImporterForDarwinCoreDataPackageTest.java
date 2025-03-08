package org.eol.globi.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.ExecutionConfig;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaId;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.junit.Assert.assertFalse;

public class DatasetImporterForDarwinCoreDataPackageTest {

    @Test
    public void validateSchema() throws IOException {

        InputStream resourceAsStream = getClass().getResourceAsStream("/org/eol/globi/data/dwc-dp/index.json");

        JsonNode jsonNode = new ObjectMapper().readTree(resourceAsStream);
        JsonNode tableSchemas = jsonNode.at("/tableSchemas");

        assertThat(tableSchemas.size(), Is.is(greaterThan(1)));

        List<String> schemaResources = new ArrayList<String>();
        for (JsonNode tableSchema : tableSchemas) {
            JsonNode url = tableSchema.at("/url");
            assertFalse(url.isMissingNode());
            String s = "(?<prefix>.*)/(?<filename>[^/]+.json)$";
            Pattern compile = Pattern.compile(s);
            Matcher matcher = compile.matcher(url.asText());
            assertTrue(matcher.matches());
            String filename = matcher.group("filename");
            schemaResources.add("/org/eol/globi/data/dwc-dp/table-schemas/" + filename);
        }

        String schemaResource = "/org/eol/globi/data/dwc-dp/table-schemas/event.json";
        assertThat(schemaResources, hasItem(schemaResource));
        for (String s : schemaResources) {
            validateSchemaResource(s);
        }
    }

    private void validateSchemaResource(String schemaResource) throws IOException {
        JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        SchemaValidatorsConfig.Builder builder = SchemaValidatorsConfig.builder();
        SchemaValidatorsConfig config = builder.build();

        JsonSchema schema = jsonSchemaFactory.getSchema(SchemaLocation.of(SchemaId.V202012), config);


        InputStream schemaStream = getClass().getResourceAsStream(schemaResource);

        Set<ValidationMessage> assertions = schema.validate(
                new ObjectMapper().readTree(schemaStream), executionContext -> {
                    ExecutionConfig executionConfig = executionContext
                            .getExecutionConfig();
                    executionConfig.setDebugEnabled(true);
                    executionConfig.setFormatAssertionsEnabled(true);
        });

        assertThat(assertions.size(), Is.is(0));
    }


}
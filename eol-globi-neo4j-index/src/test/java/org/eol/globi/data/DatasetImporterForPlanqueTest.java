package org.eol.globi.data;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.eol.globi.domain.LogContext;
import org.eol.globi.domain.StudyNode;
import org.eol.globi.tool.NullImportLogger;
import org.eol.globi.util.NodeUtil;
import org.globalbioticinteractions.dataset.DatasetImpl;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class DatasetImporterForPlanqueTest extends GraphDBTestCase {

    @Test
    public void taxonNameParser() {
        assertThat(DatasetImporterForPlanque.normalizeName("SAGITTA_ELEGANS"), is("Sagitta elegans"));
        assertThat(DatasetImporterForPlanque.normalizeName("OSTRACODA_INDET"), is("Ostracoda"));
    }

}

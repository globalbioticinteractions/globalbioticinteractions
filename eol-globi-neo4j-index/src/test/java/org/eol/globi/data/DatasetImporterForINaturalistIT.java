package org.eol.globi.data;

import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.NodeUtil;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class DatasetImporterForINaturalistIT extends DatasetImporterForINaturalistTest {

    @Test
    public void importUsingINatAPI() throws StudyImporterException, PropertyEnricherException {
        importStudy(importer);
        assertThat(NodeUtil.findAllStudies(getGraphDb()).size() > 150, is(true));
    }


}
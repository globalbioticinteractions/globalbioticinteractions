package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.InteractType;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.util.NodeUtil;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class StudyImporterForINaturalistIT extends StudyImporterForINaturalistTest {

    @Test
    public void importUsingINatAPI() throws StudyImporterException, PropertyEnricherException {
        importStudy(importer);
        assertThat(NodeUtil.findAllStudies(getGraphDb()).size() > 150, is(true));
    }


}
package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.domain.InteractType;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.util.HttpUtil;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class StudyImporterForSAIDTest extends GraphDBTestCase {

    @Test
    public void importAll() throws StudyImporterException {
        StudyImporterForSAID importerForSAID = new StudyImporterForSAID(new ParserFactoryImpl(), nodeFactory);
        importerForSAID.importStudy();
        List<Study> allStudies = NodeFactory.findAllStudies(getGraphDb());
        assertThat(allStudies.size(), is(1));

        Study foundStudy = allStudies.get(0);
        Iterable<Relationship> relationships = foundStudy.getSpecimens();
        int count = 0;
        for (Relationship relationship : relationships) {
            count++;
        }

        assertThat(count > 14000, is(true));

    }

}

package org.eol.globi.data;

import org.eol.globi.service.DatasetLocal;
import org.eol.globi.service.GeoNamesService;
import org.eol.globi.service.GeoNamesServiceImpl;
import org.eol.globi.util.InputStreamFactoryNoop;
import org.eol.globi.util.ResourceServiceHTTP;
import org.eol.globi.util.ResourceServiceLocal;
import org.junit.Test;
import org.eol.globi.geo.LatLng;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;
public class DatasetImporterForRaymondIT extends GraphDBNeo4jTestCase {

    @Test
    public void importStudy() throws StudyImporterException {
        DatasetImporterForRaymond importer = new DatasetImporterForRaymond(
                new ParserFactoryLocal(getClass()), nodeFactory
        );
        importer.setGeoNamesService(new GeoNamesService() {
            @Override
            public boolean hasTermForLocale(String locality) {
                return true;
            }

            @Override
            public LatLng findLatLng(String locality) throws IOException {
                return new LatLng(0, 0);
            }

        });
        importer.setDataset(new DatasetLocal(new ResourceServiceLocal(new InputStreamFactoryNoop())));
        importStudy(importer);

        importer.setGeoNamesService(new GeoNamesServiceImpl(new ResourceServiceHTTP(new InputStreamFactoryNoop(), getCacheDir())));

        Collection<String> unmappedLocations = new HashSet<String>();
        for (String location : importer.getLocations()) {
            if (!importer.getGeoNamesService().hasTermForLocale(location)) {
                unmappedLocations.add(location);
            }
        }

        assertThat(unmappedLocations,
                containsInAnyOrder("Not described",
                        "South African waters",
                        "Ocean location",
                        "subantarctic waters",
                        "oceanic habitat in Southern Ocean. 68� 07\u0019 S & 70�13\u0019 S",
                        "Subantarctic Pacific Ocean"));


    }
}

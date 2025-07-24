package org.eol.globi.process;

import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.data.StudyImporterException;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;

public class DOIReferenceExtractorTest {

    @Test
    public void extractDOIFromCitationWithDOIUrl() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Ollerton, J., Trunschke, J. ., Havens, K. ., Landaverde-González, P. ., Keller, A. ., Gilpin, A.-M. ., Rodrigo Rech, A. ., Baronio, G. J. ., Phillips, B. J., Mackin, C. ., Stanley, D. A., Treanore, E. ., Baker, E. ., Rotheray, E. L., Erickson, E. ., Fornoff, F. ., Brearley, F. Q. ., Ballantyne, G. ., Iossa, G. ., Stone, G. N., Bartomeus, I. ., Stockan, J. A., Leguizamón, J., Prendergast, K. ., Rowley, L., Giovanetti, M., de Oliveira Bueno, R., Wesselingh, R. A., Mallinger, R., Edmondson, S., Howard, S. R., Leonhardt, S. D., Rojas-Nossa, S. V., Brett, M., Joaqui, T., Antoniazzi, R., Burton, V. J., Feng, H.-H., Tian, Z.-X., Xu, Q., Zhang, C., Shi, C.-L., Huang, S.-Q., Cole, L. J., Bendifallah, L., Ellis, E. E., Hegland, S. J., Straffon Díaz, S., Lander, T. A. ., Mayr, A. V., Dawson, R. ., Eeraerts, M. ., Armbruster, W. S. ., Walton, B. ., Adjlane, N. ., Falk, S. ., Mata, L. ., Goncalves Geiger, A. ., Carvell, C. ., Wallace, C. ., Ratto, F. ., Barberis, M. ., Kahane, F. ., Connop, S. ., Stip, A. ., Sigrist, M. R. ., Vereecken, N. J. ., Klein, A.-M., Baldock, K. ., & Arnold, S. E. J. . (2022). Pollinator-flower interactions in gardens during the COVID-19 pandemic lockdown of 2020. Journal of Pollination Ecology, 31, 87–96. https://doi.org/10.26786/1920-7603(2022)695.");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is("10.26786/1920-7603(2022)695"));

    }
    @Test
    public void extractDOIFromCitationWithDOIUrl2() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Ollerton, J., Trunschke, J. ., Havens, K. ., Landaverde-González, P. ., Keller, A. ., Gilpin, A.-M. ., Rodrigo Rech, A. ., Baronio, G. J. ., Phillips, B. J., Mackin, C. ., Stanley, D. A., Treanore, E. ., Baker, E. ., Rotheray, E. L., Erickson, E. ., Fornoff, F. ., Brearley, F. Q. ., Ballantyne, G. ., Iossa, G. ., Stone, G. N., Bartomeus, I. ., Stockan, J. A., Leguizamón, J., Prendergast, K. ., Rowley, L., Giovanetti, M., de Oliveira Bueno, R., Wesselingh, R. A., Mallinger, R., Edmondson, S., Howard, S. R., Leonhardt, S. D., Rojas-Nossa, S. V., Brett, M., Joaqui, T., Antoniazzi, R., Burton, V. J., Feng, H.-H., Tian, Z.-X., Xu, Q., Zhang, C., Shi, C.-L., Huang, S.-Q., Cole, L. J., Bendifallah, L., Ellis, E. E., Hegland, S. J., Straffon Díaz, S., Lander, T. A. ., Mayr, A. V., Dawson, R. ., Eeraerts, M. ., Armbruster, W. S. ., Walton, B. ., Adjlane, N. ., Falk, S. ., Mata, L. ., Goncalves Geiger, A. ., Carvell, C. ., Wallace, C. ., Ratto, F. ., Barberis, M. ., Kahane, F. ., Connop, S. ., Stip, A. ., Sigrist, M. R. ., Vereecken, N. J. ., Klein, A.-M., Baldock, K. ., & Arnold, S. E. J. . (2022). Pollinator-flower interactions in gardens during the COVID-19 pandemic lockdown of 2020. Journal of Pollination Ecology, 31, 87–96. https://doi.org/10.26786/1920-7603(2022)695. Accessed at <file:///var/cache/globi/ollerton2022/./> on 23 Jul 2025.");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is("10.26786/1920-7603(2022)695"));

    }

    @Test
    public void extractDOIFromCitationWithDOIUrlURIParseWorkaround() throws StudyImporterException, URISyntaxException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        String providedDoi = "10.1890/0012-9658(2003)084[0145:IOSFWO]2.0.CO;2";

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "foo https://doi.org/" + providedDoi + " bar");
        }});

        try {
            new URI("https://doi.org/" + providedDoi);
            fail("expected some URI parse exception");
        } catch (Throwable ex) {
            assertThat(ex, instanceOf(URISyntaxException.class));
        }

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is(providedDoi));

    }

    @Test
    public void extractDOIFromCitationWithDoi() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION,
                    "Ollerton, J., Trunschke, J. ., Havens, K. ., Landaverde-González, P. ., Keller, A. ., Gilpin, A.-M. ., Rodrigo Rech, A. ., Baronio, G. J. ., Phillips, B. J., Mackin, C. ., Stanley, D. A., Treanore, E. ., Baker, E. ., Rotheray, E. L., Erickson, E. ., Fornoff, F. ., Brearley, F. Q. ., Ballantyne, G. ., Iossa, G. ., Stone, G. N., Bartomeus, I. ., Stockan, J. A., Leguizamón, J., Prendergast, K. ., Rowley, L., Giovanetti, M., de Oliveira Bueno, R., Wesselingh, R. A., Mallinger, R., Edmondson, S., Howard, S. R., Leonhardt, S. D., Rojas-Nossa, S. V., Brett, M., Joaqui, T., Antoniazzi, R., Burton, V. J., Feng, H.-H., Tian, Z.-X., Xu, Q., Zhang, C., Shi, C.-L., Huang, S.-Q., Cole, L. J., Bendifallah, L., Ellis, E. E., Hegland, S. J., Straffon Díaz, S., Lander, T. A. ., Mayr, A. V., Dawson, R. ., Eeraerts, M. ., Armbruster, W. S. ., Walton, B. ., Adjlane, N. ., Falk, S. ., Mata, L. ., Goncalves Geiger, A. ., Carvell, C. ., Wallace, C. ., Ratto, F. ., Barberis, M. ., Kahane, F. ., Connop, S. ., Stip, A. ., Sigrist, M. R. ., Vereecken, N. J. ., Klein, A.-M., Baldock, K. ., & Arnold, S. E. J. . (2022). Pollinator-flower interactions in gardens during the COVID-19 pandemic lockdown of 2020. Journal of Pollination Ecology, 31, 87–96. doi:10.26786/1920-7603(2022)695.");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is("10.26786/1920-7603(2022)695"));

    }

    @Test
    public void extractDOIFromCitationWithDOI2() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Ollerton, J., Trunschke, J. ., Havens, K. ., Landaverde-González, P. ., Keller, A. ., Gilpin, A.-M. ., Rodrigo Rech, A. ., Baronio, G. J. ., Phillips, B. J., Mackin, C. ., Stanley, D. A., Treanore, E. ., Baker, E. ., Rotheray, E. L., Erickson, E. ., Fornoff, F. ., Brearley, F. Q. ., Ballantyne, G. ., Iossa, G. ., Stone, G. N., Bartomeus, I. ., Stockan, J. A., Leguizamón, J., Prendergast, K. ., Rowley, L., Giovanetti, M., de Oliveira Bueno, R., Wesselingh, R. A., Mallinger, R., Edmondson, S., Howard, S. R., Leonhardt, S. D., Rojas-Nossa, S. V., Brett, M., Joaqui, T., Antoniazzi, R., Burton, V. J., Feng, H.-H., Tian, Z.-X., Xu, Q., Zhang, C., Shi, C.-L., Huang, S.-Q., Cole, L. J., Bendifallah, L., Ellis, E. E., Hegland, S. J., Straffon Díaz, S., Lander, T. A. ., Mayr, A. V., Dawson, R. ., Eeraerts, M. ., Armbruster, W. S. ., Walton, B. ., Adjlane, N. ., Falk, S. ., Mata, L. ., Goncalves Geiger, A. ., Carvell, C. ., Wallace, C. ., Ratto, F. ., Barberis, M. ., Kahane, F. ., Connop, S. ., Stip, A. ., Sigrist, M. R. ., Vereecken, N. J. ., Klein, A.-M., Baldock, K. ., & Arnold, S. E. J. . (2022). Pollinator-flower interactions in gardens during the COVID-19 pandemic lockdown of 2020. Journal of Pollination Ecology, 31, 87–96. doi:10.26786/1920-7603(2022)695. Accessed at <file:///var/cache/globi/ollerton2022/./> on 23 Jul 2025.");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is("10.26786/1920-7603(2022)695"));

    }

    @Test
    public void extractDOI() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Gonzalez et al. 2015. Floral integration and pollinator diversity in the generalized plant-pollinator system of Alstroemeria ligtu (Alstroemeriaceae). Evolutionary Ecology, 21(9), 63-75. doi: 10.1007/s10682-014-9746-3");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is("10.1007/s10682-014-9746-3"));

    }

    @Test
    public void existingDOI() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Gonzalez et al. 2015. Floral integration and pollinator diversity in the generalized plant-pollinator system of Alstroemeria ligtu (Alstroemeriaceae). Evolutionary Ecology, 21(9), 63-75. doi: 10.1007/s10682-014-9746-3");
            put(DatasetImporterForTSV.REFERENCE_DOI, "10.1007/444");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is("10.1007/444"));

    }

    @Test
    public void existingReferenceURL() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Gonzalez et al. 2015. Floral integration and pollinator diversity in the generalized plant-pollinator system of Alstroemeria ligtu (Alstroemeriaceae). Evolutionary Ecology, 21(9), 63-75. doi: 10.1007/s10682-014-9746-3");
            put(DatasetImporterForTSV.REFERENCE_URL, "https://example.org");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is(nullValue()));

    }

    @Test
    public void extractMalformedDOI() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Gonzalez et al. 2015. Floral integration and pollinator diversity in the generalized plant-pollinator system of Alstroemeria ligtu (Alstroemeriaceae). Evolutionary Ecology, 21(9), 63-75. doi: 40.1007/s10682-014-9746-3");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is(nullValue()));

    }

    @Test
    public void extractNoDOI() throws StudyImporterException {
        List<Map<String, String>> interactions = new ArrayList<>();

        DOIReferenceExtractor doiReferenceExtractor = new DOIReferenceExtractor(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                interactions.add(interaction);
            }
        }, null);

        doiReferenceExtractor.on(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.REFERENCE_CITATION, "Gonzalez et al. 2015. Floral integration and pollinator diversity in the generalized plant-pollinator system of Alstroemeria ligtu (Alstroemeriaceae). Evolutionary Ecology, 21(9), 63-75.");
        }});

        assertThat(interactions.size(), is(1));

        assertThat(interactions.get(0).get(DatasetImporterForTSV.REFERENCE_DOI), is(nullValue()));

    }

}
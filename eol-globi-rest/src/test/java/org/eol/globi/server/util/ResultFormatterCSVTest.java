package org.eol.globi.server.util;

import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResultFormatterCSVTest {

    public static final String CSV_WITH_TEXT_VALUES = "{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ \"one\", \"two\" ], [ \"three\", \"four\" ] ]}";
    public static final String CSV_WITH_QUOTED_TEXT_VALUES = "{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ \"and he said: \\\"boo\\\"\", \"two\" ], [ \"three\", \"four\" ] ]}";
    public static final String CSV_NUMERIC_VALUES = "{ \"columns\" : [ \"loc.latitude\", \"loc.longitude\" ], \"data\" : [ [ -25.0, 135.0 ], [ 40.9777996, -79.5252906 ] ]}";

    @Test
    public void toCSV() throws ResultFormattingException {
        String format = new ResultFormatterCSV().format(CSV_NUMERIC_VALUES);
        assertThat(format, is("\"loc.latitude\",\"loc.longitude\"\n-25.0,135.0\n40.9777996,-79.5252906\n"));
    }

    @Test(expected = ResultFormattingException.class)
    public void throwOnError() throws ResultFormattingException {
        new ResultFormatterCSV().format(RequestHelperTest.getErrorResult());
    }

    @Test
    public void toCSVOutputStream() throws ResultFormattingException {
        InputStream is = IOUtils.toInputStream(CSV_NUMERIC_VALUES, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterCSV().format(is, os);
        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                is("\"loc.latitude\",\"loc.longitude\"\n-25.0,135.0\n40.9777996,-79.5252906\n"));
    }

    @Test
    public void toCSVText() throws ResultFormattingException {
        String format = new ResultFormatterCSV().format(CSV_WITH_TEXT_VALUES);
        assertThat(format, is("\"loc.latitude\",\"loc.longitude\"\n\"one\",\"two\"\n\"three\",\"four\"\n"));
    }

    @Test
    public void toCSVTextOutputStream() throws ResultFormattingException {
        InputStream is = IOUtils.toInputStream(CSV_WITH_TEXT_VALUES, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterCSV().format(is, os);
        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                is("\"loc.latitude\",\"loc.longitude\"\n\"one\",\"two\"\n\"three\",\"four\"\n"));
    }


    @Test
    public void toCSVQuotedText() throws ResultFormattingException {
        String format = new ResultFormatterCSV().format(CSV_WITH_QUOTED_TEXT_VALUES);
        String expectedValue = "\"loc.latitude\",\"loc.longitude\"\n\"and he said: \"\"boo\"\"\",\"two\"\n\"three\",\"four\"\n";
        assertThat(format, is(expectedValue));
    }

    @Test
    public void toCSVQyotedTextOutputStream() throws ResultFormattingException {
        InputStream is = IOUtils.toInputStream(CSV_WITH_QUOTED_TEXT_VALUES, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterCSV().format(is, os);
        String expectedValue = "\"loc.latitude\",\"loc.longitude\"\n\"and he said: \"\"boo\"\"\",\"two\"\n\"three\",\"four\"\n";
        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                is(expectedValue));
    }

    @Test
    public void toCSVWithJSONArray() throws ResultFormattingException {
        String json = "{\"columns\":[\"source_taxon_name\",\"interaction_type\",\"target_taxon_name\"],\"data\":[[\"Homo sapiens\",\"preysOn\",[\"Tragelaphus spekii\",\"Kobus kob\",\"Hoplobatrachus occipitalis\",\"Pelusios castaneus\",\"Philantomba monticola\",\"Atherurus africanus\",\"Crossarchus platycephalus\",\"Heterotis niloticus\",\"Eidolon helvum\",\"Ctenosaura pectinata\",\"Poiana richardsonii\",\"Pteropus rufus\",\"Notolabrus fucicola\",\"Doryteuthis opalescens\",\"Tragelaphus scriptus\",\"Catopuma temminckii\",\"Rusa unicolor\",\"Helarctos malayanus malayanus\",\"Cercopithecus pogonias\",\"Cercopithecus nictitans\",\"Civettictis civetta\",\"Cercopithecus mona\",\"Nandinia binotata\",\"Daubentonia madagascariensis\",\"Varanus niloticus\",\"Ceratogymna atrata\",\"Thryonomys swinderianus\",\"Osteolaemus tetraspis\",\"Python sebae\",\"Cercopithecus erythrotis\",\"Cercopithecus cephus\",\"Phataginus tricuspis\",\"Bitis gabonica\",\"Rousettus madagascariensis\",\"Rousettus aegyptiacus\",\"Pardofelis marmorata\",\"Callosciurus pygerythrus\",\"Ursus thibetanus\",\"Hystrix brachyura\",\"Pteropus giganteus\",\"Conraua goliath\",\"Kinixys erosa\",\"Cricetomys emini\",\"Neofelis nebulosa\",\"Mus musculus\",\"Cricetomys gambianus\",\"Lybius bidentatus\",\"Treron calvus\",\"Heterobranchus longifilis\",\"Augosoma centaurus\",\"Ursus maritimus\",\"Spatula discors\",\"Anas clypeata\",\"Neotoma leucodon\",\"Lepus victoriae\",\"Pronolagus rupestris\",\"Cercocebus torquatus\",\"Cricetus cricetus\",\"Callipepla californica\",\"Branta canadensis\",\"Pteropus vampyrus\",\"Puma concolor\",\"Meleagris gallopavo\",\"Atilax paludinosus\",\"Brachycope anomala\",\"Grayia ornata\",\"Triaenops menamena\",\"Paratriaenops furculus\",\"Macronycteris commersonii\",\"Corythaeola cristata\",\"Clupea harengus membras\",\"Pteropus melanopogon\",\"Dendrohyrax interfluvialis\",\"Kinixys homeana\",\"Varanus albigularis\",\"Myotis bocagii\",\"Potamochoerus porcus\",\"Lavia frons\",\"Aix sponsa\",\"Pisces\",\"Cheloniidae\",\"Suidae\",\"coconut crabs\",\"Bothus leopardinus\",\"Scomberomorus guttatus\",\"demersal species\",\"Pomatomus saltatrix\",\"Galliformes\",\"Dasyatis\",\"Salvelinus alpinus alpinus\",\"fish carnivores\",\"prawns and shrimps\",\"fish herbivores\",\"Mirounga\",\"Balaenoptera\",\"Physeteridae\",\"Todarodes pacificus\",\"Engraulis japonicus\",\"Scomber japonicus\",\"Cephalopoda\",\"Stenella\",\"Sergia lucens\",\"Trachurus japonicus\"]]]}";
        InputStream is = IOUtils.toInputStream(json, StandardCharsets.UTF_8);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        new ResultFormatterCSV().format(is, os);
        String expectedValue = "\"source_taxon_name\",\"interaction_type\",\"target_taxon_name\"\n" +
                "\"Homo sapiens\",\"preysOn\",\"Tragelaphus spekii\"\n" +
                "\"Homo sapiens\",\"preysOn\",\"Kobus kob\"\n";
        assertThat(new String(os.toByteArray(), StandardCharsets.UTF_8),
                startsWith(expectedValue));

    }

}

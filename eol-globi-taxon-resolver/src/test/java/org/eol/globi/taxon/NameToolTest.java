package org.eol.globi.taxon;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.IOUtils;
import org.eol.globi.domain.PropertyAndValueDictionary;
import org.eol.globi.service.PropertyEnricher;
import org.eol.globi.service.PropertyEnricherException;
import org.eol.globi.service.PropertyEnricherFactory;
import org.eol.globi.util.DateUtil;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class NameToolTest {

    @Test
    public void resolve() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricher enricher = new PropertyEnricherMatch();
        NameTool.resolve(is, new NameTool.ResolvingRowHandler(false, os, enricher));
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tSAME_AS\tNCBI:9606\tHomo sapiens\t\t\tone | two\t\t\thttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606\t\tA name source\thttp://example.org\t1970-01-01T00:00:00Z\n"));
    }

    @Test
    public void resolveWithEnricher() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final TermMatcher matcher = PropertyEnricherFactory.createTaxonMatcher();
        NameTool.resolve(is, new NameTool.TermMatchingRowHandler(false, os, matcher));
        assertThat(os.toString(), startsWith("NCBI:9606\tHomo sapiens\tSAME_AS\tNCBI:9606\tHomo sapiens\tspecies\tman @en | human @en\t"));
    }

    @Test
    public void resolveTaxonCache() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("EOL:327955\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final TermMatcher matcher = createTaxonCacheService();
        NameTool.resolve(is, new NameTool.TermMatchingRowHandler(false, os, matcher));
        String[] lines = os.toString().split("\n");
        assertThat(lines[0], startsWith("EOL:327955\tHomo sapiens\tSAME_AS\tEOL:327955\tHomo sapiens\tSpecies\tإنسان @ar | Insan @az | човешки @bg | মানবীয় @bn | Ljudsko biće @bs | Humà @ca | Muž @cs | Menneske @da | Mensch @de | ανθρώπινο ον @el | Humans @en | Humano @es | Gizakiaren @eu | Ihminen @fi | Homme @fr | Mutum @ha | אנושי @he | մարդու @hy | Umano @it | ადამიანის @ka | Homo @la | žmogaus @lt | Om @mo | Mens @nl | Òme @oc | Om @ro | Человек разумный современный @ru | Qenie Njerëzore @sq | மனிதன் @ta | మానవుడు @te | Aadmi @ur | umuntu @zu |\tAnimalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Primates | Haplorrhini | Simiiformes | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens\tEOL:1 | EOL:3014411 | EOL:8814528 | EOL:694 | EOL:2774383 | EOL:12094272 | EOL:4712200 | EOL:1642 | EOL:57446 | EOL:2844801 | EOL:1645 | EOL:10487985 | EOL:10509493 | EOL:4529848 | EOL:1653 | EOL:10551052 | EOL:42268 | EOL:327955\tkingdom | subkingdom | infrakingdom | division | subdivision | infraphylum | superclass | class | subclass | infraclass | order | suborder | infraorder | superfamily | family | subfamily | genus | species\thttp://eol.org/pages/327955\thttp://media.eol.org/content/2014/08/07/23/02836_98_68.jpg"));
        assertThat(lines.length, Is.is(2));
        assertThat(lines[1], startsWith("EOL:327955\tHomo sapiens\tSAME_AS\tNCBI:9606\tHomo sapiens\tspecies\t"));
        assertThat(lines[0], containsString(matcher.getClass().getSimpleName()));
    }


    @Test
    public void resolveTaxonCacheMatchFirstLine() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("EOL:1276240\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final TermMatcher matcher = new TaxonCacheService("classpath:/org/eol/globi/taxon/taxonCache.tsv.gz", "classpath:/org/eol/globi/taxon/taxonMap.tsv.gz");
        NameTool.resolve(is, new NameTool.TermMatchingRowHandler(false, os, matcher));
        String[] lines = os.toString().split("\n");
        assertThat(lines.length, Is.is(1));
        assertThat(lines[0], startsWith("EOL:1276240\tHomo sapiens\tSAME_AS\tEOL:1276240\tAnas crecca carolinensis"));
    }

    @Test
    public void resolveTaxonCacheNoId() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("\tHomo sapiens");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricher enricher = createTaxonCacheService();
        NameTool.resolve(is, new NameTool.ResolvingRowHandler(false, os, enricher));
        assertThat(os.toString(), Is.is("\tHomo sapiens\tSAME_AS\tEOL:327955\tHomo sapiens\tSpecies\tإنسان @ar | Insan @az | човешки @bg | মানবীয় @bn | Ljudsko biće @bs | Humà @ca | Muž @cs | Menneske @da | Mensch @de | ανθρώπινο ον @el | Humans @en | Humano @es | Gizakiaren @eu | Ihminen @fi | Homme @fr | Mutum @ha | אנושי @he | մարդու @hy | Umano @it | ადამიანის @ka | Homo @la | žmogaus @lt | Om @mo | Mens @nl | Òme @oc | Om @ro | Человек разумный современный @ru | Qenie Njerëzore @sq | மனிதன் @ta | మానవుడు @te | Aadmi @ur | umuntu @zu |\tAnimalia | Bilateria | Deuterostomia | Chordata | Vertebrata | Gnathostomata | Tetrapoda | Mammalia | Theria | Eutheria | Primates | Haplorrhini | Simiiformes | Hominoidea | Hominidae | Homininae | Homo | Homo sapiens\tEOL:1 | EOL:3014411 | EOL:8814528 | EOL:694 | EOL:2774383 | EOL:12094272 | EOL:4712200 | EOL:1642 | EOL:57446 | EOL:2844801 | EOL:1645 | EOL:10487985 | EOL:10509493 | EOL:4529848 | EOL:1653 | EOL:10551052 | EOL:42268 | EOL:327955\tkingdom | subkingdom | infrakingdom | division | subdivision | infraphylum | superclass | class | subclass | infraclass | order | suborder | infraorder | superfamily | family | subfamily | genus | species\thttp://eol.org/pages/327955\thttp://media.eol.org/content/2014/08/07/23/02836_98_68.jpg\t\t\t\n"));
    }

    public TaxonCacheService createTaxonCacheService() {
        return new TaxonCacheService("classpath:/org/eol/globi/taxon/taxonCache.tsv", "classpath:/org/eol/globi/taxon/taxonMap.tsv");
    }

    @Test
    public void resolveAppend() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricher enricher = new PropertyEnricherMatch();
        NameTool.resolve(is, new NameTool.ResolvingRowHandler(false, os, enricher));
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tone\tSAME_AS\tNCBI:9606\tHomo sapiens\t\t\tone | two\t\t\thttps://www.ncbi.nlm.nih.gov/Taxonomy/Browser/wwwtax.cgi?id=9606\t\tA name source\thttp://example.org\t1970-01-01T00:00:00Z\n"));
    }

    @Test
    public void resolveGlobalNamesAppendFuzzyMatch() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("\tHomo saliens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, new NameTool.TermMatchingRowHandler(false, os, new GlobalNamesService()));
        assertThat(os.toString(), containsString("\tHomo saliens\tone\tSIMILAR_TO\t"));
    }

    @Test
    public void resolveReplace() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final PropertyEnricherPassThrough enricher = new PropertyEnricherPassThrough();
        NameTool.resolve(is, new NameTool.ResolvingRowHandler(true, os, enricher));
        assertThat(os.toString(), Is.is("NCBI:9606\tHomo sapiens\tone\n"));
    }

    @Test
    public void resolveGlobalNamesBatch() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, new NameTool.TermMatchingRowHandler(true, os, new GlobalNamesService(GlobalNamesSources.NCBI)));
        assertThat(os.toString(), containsString("NCBI:9606\tHomo sapiens\tone\n"));
    }

    @Test
    public void resolveTaxonCacheBatch() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, new NameTool.TermMatchingRowHandler(true, os, createTaxonCacheService()));
        assertThat(os.toString(), containsString("NCBI:9606\tHomo sapiens\tone\n"));
    }

    @Test
    public void resolveTaxonCacheBatch2() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, new NameTool.TermMatchingRowHandler(true, os, createTaxonCacheService()));
        assertThat(os.toString(), containsString("NCBI:9606\tHomo sapiens\tone\n"));
    }

    @Test
    public void resolveGlobalNamesBatchAppend() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tHomo sapiens\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, new NameTool.TermMatchingRowHandler(false, os, new GlobalNamesService(GlobalNamesSources.NCBI)));
        assertThat(os.toString(), containsString("Mammalia"));
        assertThat(os.toString(), containsString("nih.gov"));
    }

    @Test
    public void resolveGlobalNamesBatchAppendNoMatchName() throws IOException, PropertyEnricherException {
        InputStream is = IOUtils.toInputStream("NCBI:9606\tDonald duck\tone");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        NameTool.resolve(is, new NameTool.TermMatchingRowHandler(false, os, new GlobalNamesService(GlobalNamesSources.NCBI)));
        assertThat(os.toString(), startsWith("NCBI:9606\tDonald duck\tone\tNONE\t\tDonald duck"));
    }

    private static class PropertyEnricherPassThrough implements PropertyEnricher {

        @Override
        public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
            return MapUtils.unmodifiableMap(new TreeMap<String, String>(properties) {{
                put(PropertyAndValueDictionary.NAME_SOURCE, "A name source");
                put(PropertyAndValueDictionary.NAME_SOURCE_URL, "http://example.org");
                put(PropertyAndValueDictionary.NAME_SOURCE_ACCESSED_AT, DateUtil.printDate(new Date(0)));
            }});
        }

        @Override
        public void shutdown() {

        }
    }

    private static class PropertyEnricherMatch implements PropertyEnricher {

        @Override
        public Map<String, String> enrich(Map<String, String> properties) throws PropertyEnricherException {
            return MapUtils.unmodifiableMap(new TreeMap<String, String>(properties) {{
                put(PropertyAndValueDictionary.NAME_SOURCE, "A name source");
                put(PropertyAndValueDictionary.NAME_SOURCE_URL, "http://example.org");
                put(PropertyAndValueDictionary.NAME_SOURCE_ACCESSED_AT, DateUtil.printDate(new Date(0)));
                put(PropertyAndValueDictionary.PATH, "one | two");
            }});
        }

        @Override
        public void shutdown() {

        }
    }

}

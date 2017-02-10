package org.eol.globi.taxon;

import com.Ostermiller.util.LabeledCSVParser;
import org.eol.globi.domain.Taxon;
import org.eol.globi.util.CSVTSVUtil;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

public class TaxonCacheParserTest {

    public static void parse(BufferedReader reader, TaxonCacheListener listener) throws IOException {
        LabeledCSVParser labeledCSVParser = CSVTSVUtil.createLabeledCSVParser(reader);
        listener.start();
        while (labeledCSVParser.getLine() != null) {
            Taxon taxa = TaxonCacheParser.parseLine(labeledCSVParser);
            listener.addTaxon(taxa);
        }
        listener.finish();
    }

    @Test
    public void readThreeLine() throws IOException {
        BufferedReader someLines = new BufferedReader(new StringReader(
                "id,name,rank,commonNames,path,pathIds,pathNames,externalUrl,thumbnailUrl\n" +
                        "EOL:1276240,Anas crecca carolinensis,Infraspecies,Green-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl | ,Animalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis,EOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240,kingdom | phylum | class | order | family | genus | species | infraspecies,http://eol.org/pages/1276240,http://media.eol.org/content/2012/11/04/08/35791_98_68.jpg\n" +
                        "EOL:455065,Acteocina inculta,Species,rude barrel-bubble @en | ,Animalia | Mollusca | Gastropoda | Cephalaspidea | Philinoidea | Cylichnidae | Acteocina | Acteocina inculta,EOL:1 | EOL:2195 | EOL:2366 | EOL:2410 | EOL:10591049 | EOL:2415 | EOL:50321 | EOL:455065,kingdom | phylum | class | order | superfamily | family | genus | species,http://eol.org/pages/455065,\n" +
                        "EOL:1022449,Anisogammarus confervicolus,Species,,Animalia | Arthropoda | Malacostraca | Amphipoda | Anisogammaridae | Anisogammarus | Anisogammarus confervicolus,EOL:1 | EOL:164 | EOL:1157 | EOL:1158 | EOL:1343 | EOL:40790 | EOL:1022449,kingdom | phylum | class | order | family | genus | species,http://eol.org/pages/1022449,"));

        assertThat(someLines, is(notNullValue()));
        final List<Taxon> taxa = new ArrayList<Taxon>();
        parse(someLines, new TaxonCacheListener() {
            @Override
            public void addTaxon(Taxon taxon) {
                taxa.add(taxon);
            }

            @Override
            public void start() {

            }

            @Override
            public void finish() {

            }
        });

        assertThat(taxa.size(), is(3));

        Taxon taxonTerm = taxa.get(0);
        assertThat(taxonTerm.getExternalId(), is("EOL:1276240"));
        assertThat(taxonTerm.getRank(), is("Infraspecies"));
        assertThat(taxonTerm.getName(), is("Anas crecca carolinensis"));
        assertThat(taxonTerm.getPath(), is("Animalia | Chordata | Aves | Anseriformes | Anatidae | Anas | Anas crecca | Anas crecca carolinensis"));
        assertThat(taxonTerm.getPathIds(), is("EOL:1 | EOL:694 | EOL:695 | EOL:8024 | EOL:8027 | EOL:17930 | EOL:1048951 | EOL:1276240"));
        assertThat(taxonTerm.getPathNames(), is("kingdom | phylum | class | order | family | genus | species | infraspecies"));
        assertThat(taxonTerm.getExternalUrl(), is("http://eol.org/pages/1276240"));
        assertThat(taxonTerm.getCommonNames(), is("Green-winged Teal @en | Sarcelle à ailes vertes @fr | Amerikaanse Wintertaling @nl |"));
        assertThat(taxonTerm.getThumbnailUrl(), is("http://media.eol.org/content/2012/11/04/08/35791_98_68.jpg"));

        taxonTerm = taxa.get(2);
        assertThat(taxonTerm.getExternalId(), is("EOL:1022449"));
        assertThat(taxonTerm.getRank(), is("Species"));
        assertThat(taxonTerm.getName(), is("Anisogammarus confervicolus"));
        assertThat(taxonTerm.getPath(), is("Animalia | Arthropoda | Malacostraca | Amphipoda | Anisogammaridae | Anisogammarus | Anisogammarus confervicolus"));
        assertThat(taxonTerm.getPathIds(), is("EOL:1 | EOL:164 | EOL:1157 | EOL:1158 | EOL:1343 | EOL:40790 | EOL:1022449"));
        assertThat(taxonTerm.getPathNames(), is("kingdom | phylum | class | order | family | genus | species"));
        assertThat(taxonTerm.getExternalUrl(), is("http://eol.org/pages/1022449"));
        assertThat(taxonTerm.getThumbnailUrl(), is(""));
    }

}
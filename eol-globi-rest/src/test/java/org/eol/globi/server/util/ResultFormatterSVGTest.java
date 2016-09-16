package org.eol.globi.server.util;

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class ResultFormatterSVGTest {

    @Test
    public void formatter() throws ResultFormattingException {
        String svg = new ResultFormatterSVG().format("{\n" +
                "  \"columns\" : [ \"source_taxon_name\", \"interaction_type\", \"target_taxon_name\" ],\n" +
                "  \"data\" : [ [ \"Vireo olivaceus\", \"preysOn\", [ \"Diptera\", \"Auchenorrhyncha\", \"Coleoptera\", \"Lepidoptera\", \"Arachnida\" ] ], [ \"Colaptes auratus\", \"preysOn\", [ \"Hymenoptera\" ] ], [ \"Anseriformes\", \"preysOn\", [ \"Cyperaceae\", \"Tracheophyta\", \"marine invertebrates\" ] ], [ \"Geositta\", \"preysOn\", [ \"Insecta\", \"Coelopidae\", \"Diptera\", \"Talitridae\" ] ], [ \"Patagioenas squamosa\", \"preysOn\", [ \"fruit\" ] ], [ \"Ardea cinerea\", \"preysOn\", [ \"Anguilla anguilla\", \"Pollachius virens\", \"Ammodytes tobianus\", \"Zoarces viviparus\", \"Pholis gunnellus\", \"Myoxocephalus scorpius\", \"Pomatoschistus microps\", \"Crangon crangon\", \"Salmo trutta\" ] ], [ \"Setophaga petechia\", \"preysOn\", [ \"Araneae\", \"Insecta\", \"Orthoptera\", \"fruit and seeds\", \"Hemiptera\", \"Diptera\", \"Lepidoptera\", \"Formicidae\", \"Hymenoptera\", \"Coleoptera\", \"Auchenorrhyncha\" ] ] ] }");
        assertThat(svg, is(notNullValue()));
    }

    @Test
    public void formatterNotSourceTaxon() throws ResultFormattingException {
        String svg = new ResultFormatterSVG().format("{\n" +
                "  \"columns\" : [ \"source_taxon_namez\", \"interaction_type\", \"target_taxon_namez\" ]\n" +
                "  }");
        assertThat(svg, is(notNullValue()));
    }

}

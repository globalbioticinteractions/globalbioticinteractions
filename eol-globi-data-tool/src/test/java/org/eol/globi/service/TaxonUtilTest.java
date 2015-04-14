package org.eol.globi.service;

import org.eol.globi.domain.TaxonImpl;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class TaxonUtilTest {

    @Test
    public void homonym() {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setName("Lestes");
        taxon.setPath("Insecta|Lestidae|Lestes");
        taxon.setPathNames("class|family|genus");

        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Lestes");
        otherTaxon.setPath("Mammalia|Mesonychidae|Lestes");
        otherTaxon.setPathNames("class|family|genus");

        assertThat(TaxonUtil.likelyHomonym(taxon, otherTaxon), is(true));
    }

    @Test
    public void notHomonym() {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setName("Lestes");
        taxon.setPath("Insecta|Lestidae|Lestes");
        taxon.setPathNames("class|family|genus");

        TaxonImpl otherTaxon = new TaxonImpl();
        otherTaxon.setName("Lestes");
        otherTaxon.setPath("Insecta|Lestidae|Lestes");
        otherTaxon.setPathNames("class|family|genus");

        assertThat(TaxonUtil.likelyHomonym(taxon, otherTaxon), is(false));
    }

}
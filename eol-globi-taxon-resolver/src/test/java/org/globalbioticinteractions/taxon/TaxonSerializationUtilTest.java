package org.globalbioticinteractions.taxon;

import org.eol.globi.domain.Taxon;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.domain.TermImpl;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class TaxonSerializationUtilTest {

    @Test
    public void testDeserialize() {
        Taxon taxon1 = TaxonSerializationUtil.arrayToTaxon(new String[]{
                "externalId",
                "name",
                "authorship",
                "rank",
                "path",
                "pathIds",
                "pathNames",
                "commonNames",
                "statusId",
                "statusName",
                "nameSource",
                "nameSourceURL",
                "nameSourceAccessedAt",
                "externalUrl",
                "thumbnailUrl",
        });

        assertThat(taxon1.getName(), Is.is("name"));
        assertThat(taxon1.getExternalId(), Is.is("externalId"));
        assertThat(taxon1.getId(), Is.is("externalId"));
        assertThat(taxon1.getRank(), Is.is("rank"));
        assertThat(taxon1.getAuthorship(), Is.is("authorship"));
        assertThat(taxon1.getPath(), Is.is("path"));
        assertThat(taxon1.getPathIds(), Is.is("pathIds"));
        assertThat(taxon1.getPathNames(), Is.is("pathNames"));
        assertThat(taxon1.getCommonNames(), Is.is("commonNames"));
        assertThat(taxon1.getStatus().getId(), Is.is("statusId"));
        assertThat(taxon1.getStatus().getName(), Is.is("statusName"));
        assertThat(taxon1.getNameSource(), Is.is("nameSource"));
        assertThat(taxon1.getNameSourceURL(), Is.is("nameSourceURL"));
        assertThat(taxon1.getNameSourceAccessedAt(), Is.is("nameSourceAccessedAt"));
        assertThat(taxon1.getExternalUrl(), Is.is("externalUrl"));
        assertThat(taxon1.getThumbnailUrl(), Is.is("thumbnailUrl"));
    }
    @Test
    public void testSerialize() {
        TaxonImpl taxon = new TaxonImpl();
        taxon.setAuthorship("auth");
        taxon.setExternalId("externalId");
        taxon.setExternalUrl("externalUrl");
        taxon.setPathNames("pathNames");
        taxon.setPathIds("pathIds");
        taxon.setPath("path");
        taxon.setName("name");
        taxon.setNameSource("nameSource");
        taxon.setNameSourceAccessedAt("nameSourceAccessedAt");
        taxon.setNameSourceURL("nameSourceURL");
        taxon.setRank("rank");
        taxon.setCommonNames("commonNames");
        taxon.setStatus(new TermImpl("statusId", "statusName"));
        taxon.setThumbnailUrl("thumbnailUrl");

        String[] taxonArray = TaxonSerializationUtil.taxonToArray(taxon);

        assertThat(taxonArray[0], Is.is("externalId"));
        assertThat(taxonArray[1], Is.is("name"));
        assertThat(taxonArray[2], Is.is("auth"));
        assertThat(taxonArray[3], Is.is("rank"));
        assertThat(taxonArray[4], Is.is("path"));
        assertThat(taxonArray[5], Is.is("pathIds"));
        assertThat(taxonArray[6], Is.is("pathNames"));
        assertThat(taxonArray[7], Is.is("commonNames"));
        assertThat(taxonArray[8], Is.is("statusId"));
        assertThat(taxonArray[9], Is.is("statusName"));
        assertThat(taxonArray[10], Is.is("nameSource"));
        assertThat(taxonArray[11], Is.is("nameSourceURL"));
        assertThat(taxonArray[12], Is.is("nameSourceAccessedAt"));
        assertThat(taxonArray[13], Is.is("externalUrl"));
        assertThat(taxonArray[14], Is.is("thumbnailUrl"));
    }

}
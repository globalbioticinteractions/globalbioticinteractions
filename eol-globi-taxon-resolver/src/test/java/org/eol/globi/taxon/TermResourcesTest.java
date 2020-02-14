package org.eol.globi.taxon;

import org.apache.commons.lang3.tuple.Triple;
import org.eol.globi.domain.NameType;
import org.eol.globi.domain.Taxon;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TermResourcesTest {

    @Test
    public void validTaxonMapLine() {
        TermResource<Triple<Taxon, NameType, Taxon>> tripleTermResource = getTaxonMapResource();

        String validMapLine = "providedId\tprovidedName\tresolvedId\tresolvedName";
        assertTrue(tripleTermResource.getValidator().test(validMapLine));
    }

    @Test
    public void invalidTaxonMapLine() {
        TermResource<Triple<Taxon, NameType, Taxon>> tripleTermResource = getTaxonMapResource();
        assertFalse(tripleTermResource.getValidator().test("donald"));
    }

    private TermResource<Triple<Taxon, NameType, Taxon>> getTaxonMapResource() {
        return TermResources.defaultTaxonMapResource("someresource");
    }

    @Test
    public void nullTaxonMapLine() {
        TermResource<Triple<Taxon, NameType, Taxon>> tripleTermResource = getTaxonMapResource();
        assertFalse(tripleTermResource.getValidator().test(null));
    }

    @Test
    public void validTaxonCacheLine() {
        TermResource<Taxon> tripleTermResource = getTaxonCacheResource();

        String validMapLine = "id\tname\trank\tcommonNames\tpath\tpathIds\tpathNames\texternalId\tthumbnailUrl";
        assertTrue(tripleTermResource.getValidator().test(validMapLine));
    }

    private TermResource<Taxon> getTaxonCacheResource() {
        return TermResources.defaultTaxonCacheResource("someresource");
    }

    @Test
    public void invalidTaxonCacheLine() {
        TermResource<Taxon> tripleTermResource = getTaxonCacheResource();
        assertFalse(tripleTermResource.getValidator().test("donald"));
    }

    @Test
    public void emptyTaxonCacheLine() {
        TermResource<Taxon> tripleTermResource = getTaxonCacheResource();
        assertFalse(tripleTermResource.getValidator().test(""));
    }

    @Test
    public void nullTaxonCacheLine() {
        TermResource<Taxon> tripleTermResource = getTaxonCacheResource();
        assertFalse(tripleTermResource.getValidator().test(null));
    }


}
package org.eol.globi.tool;

import org.junit.Test;

import static org.junit.Assert.*;

public class KnownBadNameFilterTest {

    @Test
    public void seeminglyGoodName() {
        assertFalse(KnownBadNameFilter.seeminglyGoodNameOrId("sp", null));
        assertTrue(KnownBadNameFilter.seeminglyGoodNameOrId("sp", "EOL:1234"));
        assertTrue(KnownBadNameFilter.seeminglyGoodNameOrId("something long", null));
        assertTrue(KnownBadNameFilter.seeminglyGoodNameOrId(null, "EOL:123"));
    }



}
package org.eol.globi.service;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class LanguageCodeLookupTest {

    @Test
    public void spanishEnglish() {
        assertThat(new LanguageCodeLookup().lookupLanguageCodeFor("spa"), is("es"));
        assertThat(new LanguageCodeLookup().lookupLanguageCodeFor("eng"), is("en"));
    }
}
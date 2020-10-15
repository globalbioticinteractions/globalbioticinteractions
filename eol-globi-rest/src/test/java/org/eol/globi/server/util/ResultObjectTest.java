package org.eol.globi.server.util;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ResultObjectTest {

    @Test
    public void resultObjectsForFields() {
        for (ResultField field : ResultField.values()) {
            Assert.assertNotNull("failed to find result object for [" + field.getLabel() + "]"
                    , ResultObject.forField(field));
        }
    }

    @Test
    public void resultObjectForSourceTaxon() {
        assertThat(ResultObject.forField(ResultField.SOURCE_TAXON_NAME), is(ResultObject.SOURCE_TAXON));
    }

}
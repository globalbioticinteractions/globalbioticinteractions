package org.eol.globi.util;

import org.eol.globi.domain.InteractType;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class InteractUtilTest {

    @Test
    public void inverseMap() {
        for (InteractType interactType : InteractType.values()) {
            assertThat("no inverse for [" + interactType + "]", InteractUtil.inverseOf(interactType), is(notNullValue()));
        }
    }
}

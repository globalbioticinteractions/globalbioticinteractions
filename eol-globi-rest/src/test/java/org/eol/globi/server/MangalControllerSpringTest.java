package org.eol.globi.server;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class MangalControllerSpringTest extends SpringTestBase {

    @Autowired
    private MangalController mangalController;

    @Test
    public void taxonList() throws IOException {
        Map<String, Object> result = mangalController.listTaxa(0L, 20L);
        assertTrue(result.get("objects") instanceof List);
        assertThat(((List) result.get("objects")).size(), is(20));
        assertThat(result, is(notNullValue()));
    }

    @Test
    public void taxonView() throws IOException {
        Map<String, Object> result = mangalController.findTaxon(2L);
        assertThat(result, is(notNullValue()));
    }
}

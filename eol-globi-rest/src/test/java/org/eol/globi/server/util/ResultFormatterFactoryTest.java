package org.eol.globi.server.util;

import org.junit.Test;
import org.springframework.http.MediaType;

import java.nio.charset.Charset;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ResultFormatterFactoryTest {

    @Test
    public void mapTypes() {
        MediaType type = new MediaType("application", "json", Charset.forName("UTF-8"));

        MediaType q = new MediaType(type, new HashMap<String, String>() {
            {
                put("charset", "UTF-8");
                put("q", "0.2");
            }
        });

        assertThat(q, is(MediaType.parseMediaType("application/json;q=0.2;charset=UTF-8")));
        assertThat(q, not(is(MediaType.parseMediaType("application/json;charset=UTF-8"))));

        assertThat(new ResultFormatterFactory().create(MediaType.parseMediaType("text/html;charset=UTF-8")),
                instanceOf(ResultFormatterJSONv2.class));
        assertThat(new ResultFormatterFactory().create(MediaType.parseMediaType("application/json;charset=UTF-8")),
                instanceOf(ResultFormatterJSON.class));
    }




}
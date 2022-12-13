package org.globalbioticinteractions.elton;

import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class Elton4NTest {

    @Test
    public void multipleCommands() {
        assertThat(
                Elton4N.run(new String[]{"compile", "interactions"}),
                Is.is(0)
        );
    }

}
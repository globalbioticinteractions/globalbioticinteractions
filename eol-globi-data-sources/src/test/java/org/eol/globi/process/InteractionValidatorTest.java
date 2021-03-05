package org.eol.globi.process;

import org.eol.globi.data.DatasetImporterForDwCA;
import org.eol.globi.data.DatasetImporterForTSV;
import org.eol.globi.domain.InteractType;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class InteractionValidatorTest {

    @Test
    public void interactionTypePredicateInvalid() {
        Predicate<Map<String, String>> interactionTypePredicate =
                InteractionValidator.createInteractionTypePredicate(null);

        assertThat(interactionTypePredicate.test(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.INTERACTION_TYPE_ID, "bla");
        }}), is(false));
    }

    @Test
    public void interactionTypePredicateValid() {
        Predicate<Map<String, String>> interactionTypePredicate =
                InteractionValidator.createInteractionTypePredicate(null);

        assertThat(interactionTypePredicate.test(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.INTERACTION_TYPE_ID, InteractType.INTERACTS_WITH.getIRI());
        }}), is(true));
    }


    @Test
    public void interactionTypePredicateValidNoInteractionDetected() {
        Predicate<Map<String, String>> interactionTypePredicate =
                InteractionValidator.createInteractionTypePredicate(null);

        assertThat(interactionTypePredicate.test(new TreeMap<String, String>() {{
            put(DatasetImporterForTSV.INTERACTION_TYPE_ID, DatasetImporterForDwCA.NONE_DETECTED);
        }}), is(false));
    }

    @Test
    public void interactionTypePredicateMissing() {
        Predicate<Map<String, String>> interactionTypePredicate =
                InteractionValidator.createInteractionTypePredicate(null);

        assertThat(interactionTypePredicate.test(new TreeMap<String, String>()), is(false));
    }


}
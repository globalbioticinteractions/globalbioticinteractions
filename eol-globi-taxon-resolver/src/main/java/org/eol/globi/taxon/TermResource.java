package org.eol.globi.taxon;

import java.util.function.Function;
import java.util.function.Predicate;

public interface TermResource<T> {
    String getResource();
    Function<String, T> getParser();
    Predicate<String> getValidator();
}

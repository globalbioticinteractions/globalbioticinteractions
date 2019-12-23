package org.eol.globi.util;

import org.eol.globi.domain.Term;
import org.eol.globi.domain.TermImpl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TermUtil {

    public static List<Term> toNamesToTerms(List<String> names) {

        return names == null
                ? Collections.emptyList()
                : names.stream().map(x -> new TermImpl(null, x)).collect(Collectors.toList());
    }
}

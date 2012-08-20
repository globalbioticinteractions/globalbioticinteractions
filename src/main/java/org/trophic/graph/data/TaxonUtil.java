package org.trophic.graph.data;

public class TaxonUtil {
    public static String clean(String name) {
        name = name.replaceAll("\\(.*\\)", "");
        String trim = name.trim();
        return trim.replaceAll("(\\s+)", " ");
    }
}

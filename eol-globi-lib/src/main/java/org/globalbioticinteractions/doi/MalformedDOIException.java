package org.globalbioticinteractions.doi;

public class MalformedDOIException extends Throwable {
    public MalformedDOIException(String doi) {
        this(doi, null);
    }
    public MalformedDOIException(String doi, Throwable cause) {
        super(doi + ". See https://doi.org/10.1000/33 for more info.", cause);
    }
}

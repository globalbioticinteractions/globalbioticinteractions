package org.eol.globi.service;

import java.net.URI;

public final class DatasetLocal extends DatasetImpl {

    public DatasetLocal() {
        super("jhpoelen/eol-globidata", URI.create("classpath:/org/eol/globi/data"));
    }

}

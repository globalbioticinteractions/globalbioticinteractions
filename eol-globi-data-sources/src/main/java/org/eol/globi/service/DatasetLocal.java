package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactory;
import org.globalbioticinteractions.dataset.DatasetImpl;

import java.net.URI;

public final class DatasetLocal extends DatasetImpl {

    public DatasetLocal(InputStreamFactory inputStreamFactory) {
        super("jhpoelen/eol-globidata", URI.create("classpath:/org/eol/globi/data"), inputStreamFactory);
    }

}

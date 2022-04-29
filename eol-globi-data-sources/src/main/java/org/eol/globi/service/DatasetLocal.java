package org.eol.globi.service;

import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.dataset.DatasetImpl;

import java.net.URI;

public class DatasetLocal extends DatasetImpl {

    public DatasetLocal(ResourceService service) {
        super("jhpoelen/eol-globidata", service, URI.create("classpath:/org/eol/globi/data"));
    }

}

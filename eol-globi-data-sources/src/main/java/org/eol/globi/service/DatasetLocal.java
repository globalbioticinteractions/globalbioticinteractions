package org.eol.globi.service;

import org.eol.globi.util.InputStreamFactory;
import org.eol.globi.util.ResourceServiceLocalAndRemote;
import org.globalbioticinteractions.dataset.DatasetImpl;

import java.net.URI;

public class DatasetLocal extends DatasetImpl {

    public DatasetLocal(InputStreamFactory inputStreamFactory) {
        super("jhpoelen/eol-globidata", new ResourceServiceLocalAndRemote(inputStreamFactory), URI.create("classpath:/org/eol/globi/data"));
    }

}

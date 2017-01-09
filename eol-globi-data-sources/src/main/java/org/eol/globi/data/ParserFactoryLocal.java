package org.eol.globi.data;

import org.eol.globi.service.DatasetLocal;

public class ParserFactoryLocal extends ParserFactoryForDataset {

    public ParserFactoryLocal() {
        super(new DatasetLocal());
    }

}

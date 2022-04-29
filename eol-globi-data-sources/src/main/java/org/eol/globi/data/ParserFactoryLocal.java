package org.eol.globi.data;

import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.ResourceServiceLocal;

public class ParserFactoryLocal extends ParserFactoryForDataset {

    public ParserFactoryLocal() {
        super(new DatasetLocal(new ResourceServiceLocal(inStream -> inStream)));
    }

}

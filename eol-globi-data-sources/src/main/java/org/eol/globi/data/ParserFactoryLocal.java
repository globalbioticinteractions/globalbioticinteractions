package org.eol.globi.data;

import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.ResourceServiceLocal;
import org.globalbioticinteractions.dataset.Dataset;

public class ParserFactoryLocal extends ParserFactoryForDataset {

    public ParserFactoryLocal(Dataset dataset) {
        super(dataset);
    }

    private ParserFactoryLocal() {
        this(new DatasetLocal(new ResourceServiceLocal(inStream -> inStream)));
    }
    public ParserFactoryLocal(Class classContext) {
        this(new DatasetLocal(new ResourceServiceLocal(inStream -> inStream, classContext)));
    }

}

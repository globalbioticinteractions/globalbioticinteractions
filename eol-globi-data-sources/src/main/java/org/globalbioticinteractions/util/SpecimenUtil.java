package org.globalbioticinteractions.util;

import org.eol.globi.data.NodeFactory;
import org.eol.globi.data.NodeFactoryException;
import org.eol.globi.domain.Specimen;

public class SpecimenUtil {
    public static void setBasisOfRecordAsLiterature(Specimen specimen, NodeFactory nodeFactory) throws NodeFactoryException {
        specimen.setBasisOfRecord(nodeFactory.getOrCreateBasisOfRecord("http://gbif.github.io/gbif-api/apidocs/org/gbif/api/vocabulary/BasisOfRecord.html#LITERATURE", "Literature"));
    }
}

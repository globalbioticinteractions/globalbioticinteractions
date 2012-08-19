package org.trophic.graph.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.trophic.graph.domain.Taxon;
import org.trophic.graph.obo.OboParser;
import org.trophic.graph.obo.OboTerm;
import org.trophic.graph.obo.OboTermListener;
import org.trophic.graph.obo.OboUtil;

import java.io.IOException;

public class OboImporter extends BaseImporter {

    private static final Log LOG = LogFactory.getLog(OboImporter.class);

    public OboImporter(NodeFactory nodeFactory) {
        super(nodeFactory);
    }

    public void importOboTerm(OboTerm term) throws StudyImporterException {
        try {
            Taxon taxon = nodeFactory.findTaxonOfType(term.getName(), term.getRank());
            if (taxon == null) {
                if (term.getId() == null) {
                    throw new StudyImporterException("missing mandatory field id in term with name [" + term.getName() + "]");
                }
                Taxon taxonOfType = nodeFactory.createTaxonOfType(term.getName(), term.getRank(), term.getId());

            }
        } catch (NodeFactoryException e) {
            throw new StudyImporterException("failed to import taxonomy", e);
        }
    }

    public void doImport() throws StudyImporterException {
        OboParser parser = new OboParser();
        try {
            parser.parse(OboUtil.getDefaultBufferedReader(), new OboTermListener() {
                @Override
                public void notifyTermWithRank(OboTerm term) {
                    try {
                        importOboTerm(term);
                    } catch (StudyImporterException e) {
                        LOG.warn("failed to import term with id: [" + term.getId() + "]");
                    }
                }
            });
        } catch (IOException e) {
            throw new StudyImporterException("failed to import taxonomy", e);
        }
    }
}

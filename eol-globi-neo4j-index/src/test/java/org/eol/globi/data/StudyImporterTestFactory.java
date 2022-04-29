package org.eol.globi.data;

import org.eol.globi.service.DatasetLocal;
import org.eol.globi.util.ResourceServiceLocal;

import java.lang.reflect.Constructor;

public class StudyImporterTestFactory {

    private NodeFactory nodeFactory;
    private ParserFactory parserFactory;

    public StudyImporterTestFactory(NodeFactory nodeFactory) {
        this(new ParserFactoryLocal(), nodeFactory);
    }

    public StudyImporterTestFactory(ParserFactory parserFactory, NodeFactory nodeFactory) {
        this.parserFactory = parserFactory;
        this.nodeFactory = nodeFactory;
    }

    public DatasetImporter instantiateImporter(Class<? extends DatasetImporter> clazz) throws StudyImporterException {
        try {
            Constructor<? extends DatasetImporter> aConstructor = clazz.getConstructor(ParserFactory.class, NodeFactory.class);
            DatasetImporter datasetImporter = aConstructor.newInstance(parserFactory, nodeFactory);
            datasetImporter.setDataset(new DatasetLocal(new ResourceServiceLocal(inStream -> inStream)));
            return datasetImporter;
        } catch (Exception ex) {
            throw new StudyImporterException("failed to create study importer for [" + clazz.toString() + "]", ex);
        }
    }

}

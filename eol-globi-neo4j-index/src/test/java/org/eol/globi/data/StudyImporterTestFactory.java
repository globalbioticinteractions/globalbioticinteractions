package org.eol.globi.data;

import org.eol.globi.service.DatasetLocal;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;

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

    public StudyImporter instantiateImporter(Class<? extends StudyImporter> clazz) throws StudyImporterException {
        try {
            Constructor<? extends StudyImporter> aConstructor = clazz.getConstructor(ParserFactory.class, NodeFactory.class);
            StudyImporter studyImporter = aConstructor.newInstance(parserFactory, nodeFactory);
            studyImporter.setDataset(new DatasetLocal());
            return studyImporter;
        } catch (Exception ex) {
            throw new StudyImporterException("failed to create study importer for [" + clazz.toString() + "]", ex);
        }
    }

}

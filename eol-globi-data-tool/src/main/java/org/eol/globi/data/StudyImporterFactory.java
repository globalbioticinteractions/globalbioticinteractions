package org.eol.globi.data;

import org.eol.globi.service.DatasetLocal;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class StudyImporterFactory {

    private static final Collection<Class<? extends StudyImporter>> IMPORTERS = Collections.unmodifiableCollection(new ArrayList<Class<? extends StudyImporter>>() {{
        add(StudyImporterForGitHubData.class);
        add(StudyImporterForThessen.class);
    }});

    private NodeFactory nodeFactory;
    private ParserFactory parserFactory;

    public StudyImporterFactory(NodeFactory nodeFactory) {
        this(new ParserFactoryLocal(), nodeFactory);
    }

    public StudyImporterFactory(ParserFactory parserFactory, NodeFactory nodeFactory) {
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


    public static Collection<Class<? extends StudyImporter>> getImporters() {
        return IMPORTERS;
    }

}

package org.eol.globi.data;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class StudyImporterFactory {

    private static final Collection<Class> IMPORTERS = Collections.unmodifiableCollection(new ArrayList<Class>() {{
        add(StudyImporterForRoopnarine.class);
        add(StudyImporterForSimons.class);
        add(StudyImporterForWrast.class);
        add(StudyImporterForBlewett.class);
        add(StudyImporterForAkin.class);
        add(StudyImporterForBlaremore.class);
        add(StudyImporterForBioInfo.class);
        add(StudyImporterForJRFerrerParis.class);
        add(StudyImporterForSPIRE.class);
        add(StudyImporterForICES.class);
        add(StudyImporterForBarnes.class);
        add(StudyImporterForCook.class);
        add(StudyImporterForGoMexSI.class);
        add(StudyImporterForRobledo.class);
    }});

    private NodeFactory nodeFactory;
    private ParserFactory parserFactory;

    public StudyImporterFactory(ParserFactory parserFactory, NodeFactory nodeFactory) {
        this.parserFactory = parserFactory;
        this.nodeFactory = nodeFactory;
    }

    public StudyImporter instantiateImporter(Class<StudyImporter> clazz) throws StudyImporterException {
        StudyImporter importer = null;
        try {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 2 && parameterTypes[0] == ParserFactory.class && parameterTypes[1] == NodeFactory.class) {
                    Constructor<StudyImporter> aConstructor = clazz.getConstructor(ParserFactory.class, NodeFactory.class);
                    importer = aConstructor.newInstance(parserFactory, nodeFactory);
                }
            }
            if (importer != null) {
                importer = clazz.newInstance();
            }

        } catch (Exception ex) {
            throw new StudyImporterException("failed to create study importer for [" + clazz.toString() + "]", ex);
        }
        return importer;
    }


    public static Collection<Class> getAvailableImporters() {
        return IMPORTERS;
    }
}

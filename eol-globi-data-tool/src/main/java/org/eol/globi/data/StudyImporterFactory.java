package org.eol.globi.data;

import org.eol.globi.domain.RelType;
import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

public class StudyImporterFactory {

    private static final Collection<Class> IMPORTERS = Collections.unmodifiableCollection(new ArrayList<Class>() {{
        add(StudyImporterForHechinger.class);
        add(StudyImporterForRoopnarine.class);
        add(StudyImporterForSimons.class);
        add(StudyImporterForWrast.class);
        add(StudyImporterForBlewett.class);
        add(StudyImporterForAkin.class);
        add(StudyImporterForBaremore.class);
        add(StudyImporterForBioInfo.class);
        add(StudyImporterForJRFerrerParis.class);
        add(StudyImporterForSPIRE.class);
        add(StudyImporterForICES.class);
        add(StudyImporterForBarnes.class);
        add(StudyImporterForCook.class);
        add(StudyImporterForGoMexSI.class);
        add(StudyImporterForRobledo.class);
        add(StudyImporterForINaturalist.class);
    }});

    private NodeFactory nodeFactory;
    private ParserFactory parserFactory;

    public StudyImporterFactory(ParserFactory parserFactory, NodeFactory nodeFactory) {
        this.parserFactory = parserFactory;
        this.nodeFactory = nodeFactory;
    }

    public StudyImporter instantiateImporter(Class<StudyImporter> clazz) throws StudyImporterException {
        try {
            Constructor<StudyImporter> aConstructor = clazz.getConstructor(ParserFactory.class, NodeFactory.class);
            StudyImporter studyImporter = aConstructor.newInstance(parserFactory, nodeFactory);
            studyImporter.setLogger(new ImportLogger() {
                @Override
                public void warn(Study study, String message) {
                    createMsg(study, message, Level.WARNING);
                }

                @Override
                public void info(Study study, String message) {
                    createMsg(study, message, Level.INFO);
                }

                @Override
                public void severe(Study study, String message) {
                    createMsg(study, message, Level.SEVERE);
                }

                private void createMsg(Study study, String message, Level warning) {
                    LogMessage msg = nodeFactory.createLogMessage(warning, message);
                    study.createRelationshipTo(msg, RelTypes.HAS_LOG_MESSAGE);
                }
            });
            return studyImporter;
        } catch (Exception ex) {
            throw new StudyImporterException("failed to create study importer for [" + clazz.toString() + "]", ex);
        }
    }


    public static Collection<Class> getAvailableImporters() {
        return IMPORTERS;
    }
}

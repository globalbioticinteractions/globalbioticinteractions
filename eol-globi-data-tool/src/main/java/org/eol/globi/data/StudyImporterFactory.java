package org.eol.globi.data;

import org.eol.globi.domain.RelTypes;
import org.eol.globi.domain.Study;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

public class StudyImporterFactory {

    private static final Collection<Class> IMPORTERS = Collections.unmodifiableCollection(new ArrayList<Class>() {{
        add(StudyImporterForLifeWatchGreece.class);
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
            return aConstructor.newInstance(parserFactory, nodeFactory);
        } catch (Exception ex) {
            throw new StudyImporterException("failed to create study importer for [" + clazz.toString() + "]", ex);
        }
    }


    public static Collection<Class> getAvailableImporters() {
        return IMPORTERS;
    }
}

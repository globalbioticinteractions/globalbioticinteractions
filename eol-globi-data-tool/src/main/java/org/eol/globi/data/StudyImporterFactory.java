package org.eol.globi.data;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class StudyImporterFactory {

    private static final Collection<Class<? extends StudyImporter>> IMPORTERS = Collections.unmodifiableCollection(new ArrayList<Class<? extends StudyImporter>>() {{
        add(StudyImporterForGitHubData.class);
        add(StudyImporterForSzoboszlai.class);
        add(StudyImporterForWood.class);
        add(StudyImporterForKelpForest.class);
        add(StudyImporterForGemina.class);
        add(StudyImporterForCruaud.class);
        add(StudyImporterForStrona.class);
        add(StudyImporterForBell.class);
        add(StudyImporterForHafner.class);
        add(StudyImporterForPlanque.class);
        add(StudyImporterForBrose.class);
        add(StudyImporterForSIAD.class);
        add(StudyImporterForHurlbert.class);
        add(StudyImporterForByrnes.class);
        add(StudyImporterForRaymond.class);
        add(StudyImporterForLifeWatchGreece.class);
        add(StudyImporterForHechinger.class);
        add(StudyImporterForRoopnarine.class);
        add(StudyImporterForSimons.class);
        add(StudyImporterForWrast.class);
        add(StudyImporterForBlewett.class);
        add(StudyImporterForAkin.class);
        add(StudyImporterForBaremore.class);
        add(StudyImporterForJRFerrerParis.class);
        add(StudyImporterForSPIRE.class);
        add(StudyImporterForICES.class);
        add(StudyImporterForBarnes.class);
        add(StudyImporterForCook.class);
        add(StudyImporterForRobledo.class);
        add(StudyImporterForINaturalist.class);
        add(StudyImporterForThessen.class);
        add(StudyImporterForBioInfo.class);
    }});

    private NodeFactory nodeFactory;
    private ParserFactory parserFactory;

    public StudyImporterFactory(ParserFactory parserFactory, NodeFactory nodeFactory) {
        this.parserFactory = parserFactory;
        this.nodeFactory = nodeFactory;
    }

    public StudyImporter instantiateImporter(Class<? extends StudyImporter> clazz) throws StudyImporterException {
        try {
            Constructor<? extends StudyImporter> aConstructor = clazz.getConstructor(ParserFactory.class, NodeFactory.class);
            return aConstructor.newInstance(parserFactory, nodeFactory);
        } catch (Exception ex) {
            throw new StudyImporterException("failed to create study importer for [" + clazz.toString() + "]", ex);
        }
    }


    public static Collection<Class<? extends StudyImporter>> getOpenImporters() {
        return IMPORTERS;
    }

    public static ArrayList<Class<? extends StudyImporter>> getDarkImporters() {
        return new ArrayList<Class<? extends StudyImporter>>() {{
            add(StudyImporterForFWDP.class);
            add(StudyImporterForFishbase.class);
        }};
    }
}

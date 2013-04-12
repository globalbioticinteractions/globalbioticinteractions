package org.eol.globi.data;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterFactory {
    public static enum Study {
        ROOPNARINE,
        SIMONS,
        WRAST,
        BLEWETT,
        AKIN_MAD_ISLAND,
        BAREMORE_ANGEL_SHARK,
        BIO_INFO,
        JR_FERRER_PARIS,
        SPIRE,
        ICES,
        BARNES,
        COOK,
        GOMEXSI,
        ROBLEDO;
    }

    private Map<Study, Class> importerMap = new HashMap<Study, Class>() {{
        put(Study.ROOPNARINE, StudyImporterForRoopnarine.class);
        put(Study.SIMONS, StudyImporterForSimons.class);
        put(Study.WRAST, StudyImporterForWrast.class);
        put(Study.BLEWETT, StudyImporterForBlewett.class);
        put(Study.AKIN_MAD_ISLAND, StudyImporterForAkin.class);
        put(Study.BAREMORE_ANGEL_SHARK, StudyImporterForBlaremore.class);
        put(Study.BIO_INFO, StudyImporterForBioInfo.class);
        put(Study.JR_FERRER_PARIS, StudyImporterForJRFerrerParis.class);
        put(Study.SPIRE, StudyImporterForSPIRE.class);
        put(Study.ICES, StudyImporterForICES.class);
        put(Study.BARNES, StudyImporterForBarnes.class);
        put(Study.COOK, StudyImporterForCook.class);
        put(Study.GOMEXSI, StudyImporterForGoMexSI.class);
        put(Study.ROBLEDO, StudyImporterForRobledo.class);
    }};

    private NodeFactory nodeFactory;
    private ParserFactory parserFactory;

    public StudyImporterFactory(ParserFactory parserFactory, NodeFactory nodeFactory) {
        this.parserFactory = parserFactory;
        this.nodeFactory = nodeFactory;
    }

    public StudyImporter createImporterForStudy(Study study) throws StudyImporterException {
        Class<StudyImporter> clazz = (Class<StudyImporter>) importerMap.get(study);
        try {
            Constructor<StudyImporter> constructor = clazz.getConstructor(ParserFactory.class, NodeFactory.class);
            return constructor.newInstance(parserFactory, nodeFactory);
        } catch (Exception ex) {
            throw new StudyImporterException("failed to create study importer for [" + study.toString() + "]", ex);
        }
    }


}

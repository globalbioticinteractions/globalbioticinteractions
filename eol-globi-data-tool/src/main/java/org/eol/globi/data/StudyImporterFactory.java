package org.eol.globi.data;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

public class StudyImporterFactory {
    private NodeFactory nodeFactory;
    private ParserFactory parserFactory;

    private Map<StudyLibrary.Study, Class> importerMap = new HashMap<StudyLibrary.Study, Class>() {{
        put(StudyLibrary.Study.ROOPNARINE, StudyImporterForRoopnarine.class);
        put(StudyLibrary.Study.SIMONS, StudyImporterForSimons.class);
        put(StudyLibrary.Study.WRAST, StudyImporterForWrast.class);
        put(StudyLibrary.Study.BLEWETT, StudyImporterForBlewett.class);
        put(StudyLibrary.Study.AKIN_MAD_ISLAND, StudyImporterForAkin.class);
        put(StudyLibrary.Study.BAREMORE_ANGEL_SHARK, StudyImporterForBlaremore.class);
        put(StudyLibrary.Study.BIO_INFO, StudyImporterForBioInfo.class);
        put(StudyLibrary.Study.JR_FERRER_PARIS, StudyImporterForJRFerrerParis.class);
        put(StudyLibrary.Study.SPIRE, StudyImporterForSPIRE.class);
        put(StudyLibrary.Study.ICES, StudyImporterForICES.class);
        put(StudyLibrary.Study.BARNES, StudyImporterForBarnes.class);
        put(StudyLibrary.Study.COOK, StudyImporterForCook.class);
        put(StudyLibrary.Study.GOMEXSI, StudyImporterForGoMexSI.class);
    }};

    public StudyImporterFactory(ParserFactory parserFactory, NodeFactory nodeFactory) {
        this.parserFactory = parserFactory;
        this.nodeFactory = nodeFactory;
    }

    public StudyImporter createImporterForStudy(StudyLibrary.Study study) throws StudyImporterException {
        Class<StudyImporter> clazz = (Class<StudyImporter>)importerMap.get(study);
        try {
            Constructor<StudyImporter> constructor = clazz.getConstructor(ParserFactory.class, NodeFactory.class);
            return constructor.newInstance(parserFactory, nodeFactory);
        } catch (Exception ex) {
            throw new StudyImporterException("failed to create study importer for [" + study.toString() + "]", ex);
        }
    }
}

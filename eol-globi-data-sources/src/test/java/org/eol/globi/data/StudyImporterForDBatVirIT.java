package org.eol.globi.data;

import org.eol.globi.service.DatasetLocal;
import org.junit.Test;

public class StudyImporterForDBatVirIT {

    @Test
    public void importAll() throws StudyImporterException {
        StudyImporterForDBatVir studyImporterForDBatVir = new StudyImporterForDBatVir(null, null);
        studyImporterForDBatVir.setDataset(new DatasetLocal(in -> in));
        studyImporterForDBatVir.setInteractionListener(System.out::println);
        studyImporterForDBatVir.importStudy();
    }

}
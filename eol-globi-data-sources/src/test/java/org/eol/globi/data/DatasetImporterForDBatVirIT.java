package org.eol.globi.data;

import org.eol.globi.service.DatasetLocal;
import org.junit.Test;

public class DatasetImporterForDBatVirIT {

    @Test
    public void importAll() throws StudyImporterException {
        DatasetImporterForDBatVir studyImporterForDBatVir = new DatasetImporterForDBatVir(null, null);
        studyImporterForDBatVir.setDataset(new DatasetLocal(in -> in));
        studyImporterForDBatVir.setInteractionListener(System.out::println);
        studyImporterForDBatVir.importStudy();
    }

}
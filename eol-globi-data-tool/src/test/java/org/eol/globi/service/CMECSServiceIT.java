package org.eol.globi.service;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.JetFormat;
import com.healthmarketscience.jackcess.Table;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.util.HttpUtil;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class CMECSServiceIT {

    @Test
    public void importCMECS() throws IOException {
        // from http://cmecscatalog.org/ at 30 April 2014
        HttpResponse execute = HttpUtil.createHttpClient().execute(new HttpGet("http://cmecscatalog.org/docs/cmecs4.accdb"));
        File cmecs = File.createTempFile("cmecs", "accdb");
        IOUtils.copy(execute.getEntity().getContent(), new FileOutputStream(cmecs));

        Database db = Database.open(new File(cmecs.toURI()), true);
        assertThat(db.getFileFormat().getJetFormat(), is(JetFormat.VERSION_12));

        String[] tableNames = new String[]{
                "Aquatic Setting",
                "AquaticSetting_Description",
                "BiogeographicSetting",
                "BiogeographicSetting_Description",
                "d_Component",
                "d_Level",
                "Modifier",
                "ModifierType",
                "ModifierValue",
                "Unit",
                "Unit_AquaticSetting",
                "Unit_BiogeographicSetting",
                "Unit_Description",
                "Unit_Modifier",
                "Unit_Unit"
        };
        Set<String> expectedSet = new HashSet<String>();
        Collections.addAll(expectedSet, tableNames);

        Set<String> actualTableNames = db.getTableNames();
        assertThat(actualTableNames.size(), is(not(0)));
        assertThat("expected tables names [" + Arrays.toString(tableNames) + "] to be present",
                CollectionUtils.subtract(expectedSet, actualTableNames).size(), is(0));


        for (String actualTableName : actualTableNames) {
            System.out.println("table [" + actualTableName + "] START");
            Table taxonTable = db.getTable(actualTableName);
            Map<String, Object> row = null;
            while ((row = taxonTable.getNextRow()) != null) {
                System.out.println(row);
            }
            System.out.println("table [" + actualTableName + "] END");
        }

        cmecs.delete();

    }
}

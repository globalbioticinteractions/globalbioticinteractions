package org.eol.globi.data;

import org.eol.globi.process.InteractionListener;
import org.eol.globi.service.DatasetLocal;
import org.eol.globi.tool.NullImportLogger;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;

public class InjectRelatedRecordsTest {

    @Test
    public void linkEmptyRecord() throws StudyImporterException {
        List<Map<String, String>> records = new ArrayList<>();
        InteractionListener listener = new InjectRelatedRecords(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                records.add(interaction);

            }
        }, new DatasetLocal(resourceName -> null), new TreeMap<>(), new NullImportLogger());

        listener.on(new TreeMap<>());


        assertThat(records.size(), Is.is(1));
        assertThat(records.get(0).isEmpty(), Is.is(true));


    }

    @Test
    public void linkRecordWithoutIndexedDependency() throws StudyImporterException {
        List<Map<String, String>> records = new ArrayList<>();
        InteractionListener listener = new InjectRelatedRecords(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                records.add(interaction);

            }
        }, new DatasetLocal(resourceName -> null), new TreeMap<>(), new NullImportLogger());

        TreeMap<String, String> interaction = new TreeMap<String, String>() {{
            put("referenceId", "R001");
        }};
        listener.on(interaction);


        assertThat(records.size(), Is.is(1));
        assertThat(records.get(0).size(), Is.is(1));
        assertThat(records.get(0).get("referenceId"), Is.is("R001"));
    }

    @Test
    public void linkRecordWithNonTaxonIndexedDependency() throws StudyImporterException {
        List<Map<String, String>> records = new ArrayList<>();
        TreeMap<String, Map<String, Map<String, String>>> indexedDependencies = new TreeMap<>();
        indexedDependencies.put("referenceId", new TreeMap<String, Map<String, String>>() {{
            put("R001", new TreeMap<String, String>() {{
                put("referenceId", "R001");
                put("referenceCitation", "Maria Silva et al. 2024");
            }});
        }});
        InteractionListener listener = new InjectRelatedRecords(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                records.add(interaction);

            }
        }, new DatasetLocal(resourceName -> null), indexedDependencies, new NullImportLogger());

        TreeMap<String, String> interaction = new TreeMap<String, String>() {{
            put("referenceId", "R001");
        }};
        listener.on(interaction);


        assertThat(records.size(), Is.is(1));
        assertThat(records.get(0).size(), Is.is(2));
        assertThat(records.get(0).get("referenceId"), Is.is("R001"));
        assertThat(records.get(0).get("referenceCitation"), Is.is("Maria Silva et al. 2024"));
    }

    @Test
    public void linkSourceIdRecordWithTaxonIndexedDependency() throws StudyImporterException {
        List<Map<String, String>> records = new ArrayList<>();
        TreeMap<String, Map<String, Map<String, String>>> indexedDependencies = new TreeMap<>();
        indexedDependencies.put("taxonId", new TreeMap<String, Map<String, String>>() {{
            put("Anguilla", new TreeMap<String, String>() {{
                put("taxonId", "Anguilla");
                put("taxonFamilyName", "Anguillidae");
            }});
        }});
        InteractionListener listener = new InjectRelatedRecords(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                records.add(interaction);

            }
        }, new DatasetLocal(resourceName -> null), indexedDependencies, new NullImportLogger());

        TreeMap<String, String> interaction = new TreeMap<String, String>() {{
            put("sourceTaxonId", "Anguilla");
        }};
        listener.on(interaction);


        assertThat(records.size(), Is.is(1));
        assertThat(records.get(0).size(), Is.is(2));
        assertThat(records.get(0).get("sourceTaxonId"), Is.is("Anguilla"));
        assertThat(records.get(0).get("sourceTaxonFamilyName"), Is.is("Anguillidae"));
    }

    @Test
    public void linkTargetIdRecordWithTaxonIndexedDependency() throws StudyImporterException {
        List<Map<String, String>> records = new ArrayList<>();
        TreeMap<String, Map<String, Map<String, String>>> indexedDependencies = new TreeMap<>();
        indexedDependencies.put("taxonId", new TreeMap<String, Map<String, String>>() {{
            put("Anguilla", new TreeMap<String, String>() {{
                put("taxonId", "Anguilla");
                put("taxonFamilyName", "Anguillidae");
            }});
        }});
        InteractionListener listener = new InjectRelatedRecords(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                records.add(interaction);

            }
        }, new DatasetLocal(resourceName -> null), indexedDependencies, new NullImportLogger());

        TreeMap<String, String> interaction = new TreeMap<String, String>() {{
            put("targetTaxonId", "Anguilla");
        }};
        listener.on(interaction);


        assertThat(records.size(), Is.is(1));
        assertThat(records.get(0).size(), Is.is(2));
        assertThat(records.get(0).get("targetTaxonId"), Is.is("Anguilla"));
        assertThat(records.get(0).get("targetTaxonFamilyName"), Is.is("Anguillidae"));
    }

    @Test
    public void linkSourceReferenceAndTargetIdRecordWithTaxonIndexedDependency() throws StudyImporterException {
        List<Map<String, String>> records = new ArrayList<>();
        TreeMap<String, Map<String, Map<String, String>>> indexedDependencies = new TreeMap<>();
        TreeMap<String, Map<String, String>> taxonIdIndex = new TreeMap<String, Map<String, String>>() {{
            put("Anguilla", new TreeMap<String, String>() {{
                put("taxonId", "Anguilla");
                put("taxonFamilyName", "Anguillidae");
            }});
            put("Konosirus", new TreeMap<String, String>() {{
                put("taxonId", "Konosirus");
                put("taxonFamilyName", "Clupeidae");
            }});
        }};
        indexedDependencies.put("taxonId", taxonIdIndex);

        TreeMap<String, Map<String, String>> referenceIdIndex = new TreeMap<String, Map<String, String>>() {{
            put("R001", new TreeMap<String, String>() {{
                put("referenceId", "R001");
                put("referenceCitation", "Maria Silva et al. 2024");
            }});
        }};
        indexedDependencies.put("referenceId", referenceIdIndex);

        InteractionListener listener = new InjectRelatedRecords(new InteractionListener() {
            @Override
            public void on(Map<String, String> interaction) throws StudyImporterException {
                records.add(interaction);

            }
        }, new DatasetLocal(resourceName -> null), indexedDependencies, new NullImportLogger());

        TreeMap<String, String> interaction = new TreeMap<String, String>() {{
            put("sourceTaxonId", "Konosirus");
            put("targetTaxonId", "Anguilla");
            put("referenceId", "R001");
        }};
        listener.on(interaction);


        assertThat(records.size(), Is.is(1));
        assertThat(records.get(0).size(), Is.is(6));
        assertThat(records.get(0).get("sourceTaxonId"), Is.is("Konosirus"));
        assertThat(records.get(0).get("sourceTaxonFamilyName"), Is.is("Clupeidae"));
        assertThat(records.get(0).get("targetTaxonId"), Is.is("Anguilla"));
        assertThat(records.get(0).get("targetTaxonFamilyName"), Is.is("Anguillidae"));
        assertThat(records.get(0).get("referenceId"), Is.is("R001"));
        assertThat(records.get(0).get("referenceCitation"), Is.is("Maria Silva et al. 2024"));
    }

}
package org.eol.globi.data;

import com.hp.hpl.jena.vocabulary.DCTerms;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.*;
import org.apache.commons.lang3.StringUtils;
import org.gbif.dwc.Archive;
import org.gbif.dwc.record.Record;
import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.globalbioticinteractions.dataset.DwCAUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.eol.globi.data.StudyImporterForTSV.*;

public class StudyImporterForDwCA extends BaseStudyImporter {

    private InteractionListener listener;

    public StudyImporterForDwCA(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            if (getDataset() == null) {
                throw new IllegalArgumentException("no dataset found");
            }

            Path tmpDwA = Files.createTempDirectory("dwca");
            Archive archive = DwCAUtil.archiveFor(getDataset().getResourceURI(getDataset().getArchiveURI().toString()), tmpDwA.toString());
            InteractionListener interactionListener = getListener();
            for (Record rec : archive.getCore()) {
                String associatedTaxa = rec.value(DwcTerm.associatedTaxa);
                String dynamicProperties = rec.value(DwcTerm.dynamicProperties);
                if (associatedTaxa != null && dynamicProperties != null) {

                    List<Map<String, String>> interactionCandidates = new ArrayList<>();
                    if (StringUtils.isNotBlank(associatedTaxa)) {
                        interactionCandidates.addAll(parseAssociatedTaxa(associatedTaxa));
                    }

                    if (StringUtils.isNotBlank(dynamicProperties)) {
                        interactionCandidates.add(parseDynamicProperties(dynamicProperties));
                    }

                    List<Map<String, String>> interactions = interactionCandidates
                            .stream()
                            .filter(x -> x.containsKey(INTERACTION_TYPE_NAME) || x.containsKey(TARGET_TAXON_NAME))
                            .collect(Collectors.toList());


                    Map<String, String> interaction = new HashMap<>(rec.terms().size());
                    for (Term term : rec.terms()) {
                        interaction.put(term.qualifiedName(), rec.value(term));
                    }

                    for (Map<String, String> interactionProperties : interactions) {
                        interactionProperties.putAll(interaction);
                        mapIfAvailable(rec, interactionProperties, SOURCE_TAXON_NAME, DwcTerm.scientificName);
                        mapIfAvailable(rec, interactionProperties, SOURCE_LIFE_STAGE, DwcTerm.lifeStage);
                        mapIfAvailable(rec, interactionProperties, REFERENCE_CITATION, DcTerm.references);
                        mapIfAvailable(rec, interactionProperties, REFERENCE_ID, DcTerm.references);

                        interactionListener.newLink(interactionProperties);
                    }
                }
            }

            org.apache.commons.io.FileUtils.deleteQuietly(tmpDwA.toFile());
        } catch (IOException e) {
            throw new StudyImporterException("failed to read archive [" + getDataset().getArchiveURI() + "]", e);
        }
    }

    private void mapIfAvailable(Record rec, Map<String, String> interactionProperties, String key, Term term) {
        String value = rec.value(term);
        if ((StringUtils.isNotBlank(value))) {
            interactionProperties.put(key, value);
        }
    }

    InteractionListener getListener() {
        return listener == null
                ? new InteractionListenerImpl(nodeFactory, getGeoNamesService(), getLogger())
                : listener;
    }

    void setListener(InteractionListener listener) {
        this.listener = listener;
    }

    static List<Map<String, String>> parseAssociatedTaxa(String s) {
        List<Map<String, String>> properties = new ArrayList<>();
        String[] parts = StringUtils.splitByWholeSeparator(s, "|");
        for (String part : parts) {
            String[] verbTaxon = StringUtils.splitByWholeSeparator(part, ":", 2);
            if (verbTaxon.length == 2) {
                HashMap<String, String> e = new HashMap<>();
                e.put(INTERACTION_TYPE_NAME, StringUtils.lowerCase(StringUtils.trim(verbTaxon[0])));
                e.put(TARGET_TAXON_NAME, StringUtils.trim(verbTaxon[1]));
                properties.add(e);
            }
        }
        return properties;
    }

    static Map<String, String> parseDynamicProperties(String s) {
        Map<String, String> properties = new HashMap<>();
        String[] parts = StringUtils.splitByWholeSeparator(s, ";");
        for (String part : parts) {
            String[] propertyValue = StringUtils.splitByWholeSeparator(part, ":", 2);
            if (propertyValue.length == 2) {
                properties.put(StringUtils.trim(propertyValue[0]), StringUtils.trim(propertyValue[1]));
            }
        }
        return properties;
    }


}

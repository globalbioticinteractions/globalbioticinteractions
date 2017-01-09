package org.eol.globi.data;

import com.Ostermiller.util.CSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.domain.StudyImpl;
import org.eol.globi.domain.TaxonImpl;
import org.eol.globi.util.ExternalIdUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class StudyImporterForLifeWatchGreece extends BaseStudyImporter {


    public StudyImporterForLifeWatchGreece(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public void importStudy() throws StudyImporterException {
        try {
            InteractionListener interactionListener = new InteractionListener();
            interactionListener.setListener(new ParsedInteractionListener(nodeFactory));
            handleTable(interactionListener, "pub_tax_trait.csv.gz");
        } catch (IOException e) {
            throw new StudyImporterException("failed to import study", e);
        }
    }

    protected void handleTable(RowListener listener, String tableName) throws IOException {
        InputStream is = getDataset().getResource("polytraits.lifewatchgreece.eu/" + tableName);

        CSVParser parser = new CSVParser(is);
        String[] line;
        while ((line = parser.getLine()) != null) {
            listener.nextLine(line);
        }
    }

    public static class InteractionListener implements RowListener {
        private final Map<String, String> TERM_TO_TAXON_NAME = new HashMap<String, String>() {{
            put("http://polytraits.lifewatchgreece.eu/terms/PRED_BIRD", "Aves");
            put("http://www.owl-ontologies.com/unnamed.owl#Algae", "Algae");
            put("http://polytraits.lifewatchgreece.eu/terms/TF_CIL", "Ciliophora");
            put("http://polytraits.lifewatchgreece.eu/terms/PRED_ANN", "Annelida");
            put("http://polytraits.lifewatchgreece.eu/terms/PRED_FISH", "Actinopterygii");
            put("http://polytraits.lifewatchgreece.eu/terms/PRED_CRUS", "Crustacea");
            put("http://www.owl-ontologies.com/unnamed.owl#Detritus", "Detritus");
            put("http://polytraits.lifewatchgreece.eu/terms/TF_DIAT", "Bacillariophyta");
            put("http://polytraits.lifewatchgreece.eu/terms/TF_FLAG", "Flagellates");
            put("http://www.owl-ontologies.com/unnamed.owl#Aquatic_crustaceans", "Crustacea");
            put("http://polytraits.lifewatchgreece.eu/terms/TF_BACT", "Bacteria");
            put("http://polytraits.lifewatchgreece.eu/terms/TF_FORAM", "Foraminifera");
            put("http://polytraits.lifewatchgreece.eu/terms/TF_ANN", "Annelida");
            put("http://www.owl-ontologies.com/unnamed.owl#Molluscs", "Mollusca");
            put("http://polytraits.lifewatchgreece.eu/terms/PRED_MOLL", "Mollusca");
            put("http://www.owl-ontologies.com/unnamed.owl#Fish", "Actinopterygii");
            put("http://polytraits.lifewatchgreece.eu/terms/PRED_ECHI", "Echinodermata");
            put("http://www.owl-ontologies.com/unnamed.owl#Echinoderms", "Echinodermata");
            put("http://polytraits.lifewatchgreece.eu/terms/TF_ASC", "Ascidiacea");
            put("http://www.owl-ontologies.com/unnamed.owl#Cnidarians", "Cnidaria");
            put("http://polytraits.lifewatchgreece.eu/terms/TF_SED", "Sediment");
        }};

        protected final Set<String> unmapped = new HashSet<String>();

        protected ParsedInteractionListener listener;

        @Override
        public void nextLine(String[] line) {
            String reference_id = line[0];
            String reference = line[1];
            String taxonName = line[2];
            String modalityTerm = line[3];
            String modalityDefinition = line[4];
            String traitTerm = line[5];
            LifeStage lifeStage = null;
            String mode = line[7];
            if ("1".equals(mode)) {
                lifeStage = LifeStage.ADULT;
            } else if ("2".equals(mode)) {
                // reproductive stage . . . not quite sure how to map this
            } else if ("3".equals(mode)) {
                lifeStage = LifeStage.LARVA;
            } else if (StringUtils.isNotBlank(mode)) {
                throw new IllegalArgumentException("found invalid unexpected [" + mode + "]");
            }

            Integer traitValue = null;
            String traitValueString = line[8];
            if (StringUtils.isNotBlank(traitValueString)) {
                traitValue = Integer.parseInt(traitValueString);
            }

            if ("http://eol.org/schema/terms/preysUpon".equals(traitTerm)) {
                if (!TERM_TO_TAXON_NAME.containsKey(modalityTerm)) {
                    unmapped.add(modalityTerm);
                }
                if (listener != null) {
                    String predatorTaxonName = taxonName;
                    String preyTaxonName = TERM_TO_TAXON_NAME.get(modalityTerm);
                    listener.foundInteraction(predatorTaxonName, preyTaxonName, reference_id, reference);
                }

            } else if ("http://polytraits.lifewatchgreece.eu/terms/PRED".equals(traitTerm)) {
                if (!TERM_TO_TAXON_NAME.containsKey(modalityTerm)) {
                    unmapped.add(modalityTerm);
                }
                if (listener != null) {
                    String predatorTaxonName = TERM_TO_TAXON_NAME.get(modalityTerm);
                    String preyTaxonName = taxonName;
                    listener.foundInteraction(predatorTaxonName, preyTaxonName, reference_id, reference);
                }
            } else if ("http://eol.org/schema/terms/Habitat".equals(traitTerm)) {
                // TODO map habitats
            } else if ("http://polytraits.lifewatchgreece.eu/terms/DZP".equals(traitTerm)) {
                // TODO map depth
            } else if ("http://polytraits.lifewatchgreece.eu/terms/DZ".equals(traitTerm)) {
                // TODO map depth
            } else if ("http://purl.obolibrary.org/obo/CMO_0000013".equals(traitTerm)) {
                // TODO map specimen length
            }

        }

        public void setListener(ParsedInteractionListener parsedInteractionListener) {
            this.listener = parsedInteractionListener;
        }
    }

    private static class ParsedInteractionListener {
        private final Log LOG = LogFactory.getLog(ParsedInteractionListener.class);
        private NodeFactory nodeFactory;

        public ParsedInteractionListener(NodeFactory factory) {
            this.nodeFactory = factory;
        }


        public void foundInteraction(String predatorTaxonName, String preyTaxonName, String studyId, String studyReference) {
            try {
                Study study = nodeFactory.getOrCreateStudy(new StudyImpl("http://polytraits.lifewatchgreece.eu/publication/" + studyId, "Faulwetter S, Markantonatou V, Pavloudi C, Papageorgiou N, Keklikoglou K, Chatzinikolaou E, Pafilis E, Chatzigeorgiou G, Vasileiadou K, Dailianis T, Fanini L, Koulouri P, Arvanitidis C (2014) Polytraits: A database on biological traits of marine polychaetes. Biodiversity Data Journal 2: e1024. doi:10.3897/BDJ.2.e1024 . Available at http://polytraits.lifewatchgreece.eu.", null, ExternalIdUtil.toCitation(null, studyReference, null)));
                Specimen predator = nodeFactory.createSpecimen(study, new TaxonImpl(predatorTaxonName, null));
                predator.ate(nodeFactory.createSpecimen(study, new TaxonImpl(preyTaxonName, null)));
            } catch (NodeFactoryException e) {
                LOG.warn("failed to create specimen with name [" + predatorTaxonName + "]", e);
            }
        }
    }
}

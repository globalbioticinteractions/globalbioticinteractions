package org.eol.globi.data.taxon;

import java.io.BufferedReader;
import java.io.IOException;

public class GulfBaseTaxonReaderFactory implements TaxonReaderFactory {
    private static final String[] DATA_FILES = {
                "Acanthocephala_O.csv", "Chaetognatha.csv", "Gastrotricha.csv", "Pinophyta.csv",
                "Acoelomorpha.csv", "Chlorophyta.csv", "Gnathostomulida.csv", "Platyhelminthes.csv",
                "Annelida_O.csv", "Chordata_O.csv", "Hemichordata.csv", "Polypodiophyta.csv",
                "Arthropoda_O.csv", "Cnidaria.csv", "Kinorhyncha.csv", "Porifera_O.csv",
                "Ascomycota_O.csv", "Ctenophora.csv", "Loricifera.csv", "Priapulida.csv",
                "Bacillariophyta.csv", "Dicyemida.csv", "Magnoliophyta.csv", "Rhodophyta.csv",
                "Basidiomycota.csv", "Echinodermata.csv", "Mollusca.csv", "Rotifera_O.csv",
                "Brachiopoda.csv", "Echiura.csv", "Nematoda.csv", "Tardigrada.csv",
                "Bryophyta.csv", "Entoprocta.csv", "Nemertea.csv",
                "Bryozoa.csv", "Foraminifera.csv", "Phoronida.csv"};


    @Override
    public BufferedReader getReader() throws IOException {
        return null;
    }
}

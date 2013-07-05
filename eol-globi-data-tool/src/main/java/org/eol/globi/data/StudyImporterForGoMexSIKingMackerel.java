package org.eol.globi.data;

public class StudyImporterForGoMexSIKingMackerel extends StudyImporterForGoMexSIBase {

    public StudyImporterForGoMexSIKingMackerel(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    protected String getPreyResourcePath() {
        return getResourcePackage() + "/PREY km .csv";
    }

    private String getResourcePackage() {
        return "gomexsi_km";
    }

    @Override
    protected String getPredatorResourcePath() {
        return getResourcePackage() + "/PREDATORS km .csv";
    }

    @Override
    protected String getReferencesResourcePath() {
        return getResourcePackage() + "/REFERENCES km.csv";
    }

    @Override
    protected String getLocationsResourcePath() {
        return getResourcePackage() + "/LOCATIONS km .csv";
    }

}

package org.eol.globi.data;

public class StudyImporterForGoMexSI extends StudyImporterForGoMexSIBase {

    public StudyImporterForGoMexSI(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    protected String getPreyResourcePath() {
        return getResourcePackage() + "/Prey.csv";
    }

    private String getResourcePackage() {
        return "gomexsi";
    }

    @Override
    protected String getPredatorResourcePath() {
        return getResourcePackage() + "/Predators.csv";
    }

    @Override
    protected String getReferencesResourcePath() {
        return getResourcePackage() + "/References.csv";
    }

    @Override
    protected String getLocationsResourcePath() {
        return getResourcePackage() + "/Locations.csv";
    }

}

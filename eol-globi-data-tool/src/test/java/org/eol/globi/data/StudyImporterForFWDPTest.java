package org.eol.globi.data;

import com.Ostermiller.util.CSVPrinter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.eol.globi.geo.EcoRegion;
import org.eol.globi.geo.EcoRegionFinderException;
import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.Relationship;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

public class StudyImporterForFWDPTest extends GraphDBTestCase {
    private static final Log LOG = LogFactory.getLog(StudyImporterForFWDPTest.class);

    @Ignore
    @Test
    public void importAll() throws StudyImporterException, IOException, EcoRegionFinderException {
        StudyImporterForFWDP studyImporter = new StudyImporterForFWDP(new ParserFactoryImpl(), nodeFactory);
        studyImporter.setFilter(new ImportFilter() {
            @Override
            public boolean shouldImportRecord(Long recordNumber) {
                return recordNumber % 1000 == 0;
            }
        });
        LOG.info("test import started... importing every 1000th record");
        File tempCsvExport = File.createTempFile("fwdp", ".csv");
        FileOutputStream fos = new FileOutputStream(tempCsvExport);
        CSVPrinter csvOut = new CSVPrinter(fos);
        csvOut.write(new String[]{"predator specimen id (cruise6-PDID-CATNUM)", "predator taxon (pdscinam)", "predator length in mm (PDLEN * 10.0)", "prey taxon (PYNAM)", "year (year)", "month (month)", "ecoRegion (derived from declat;declon)"});
        Study study = studyImporter.importStudy();
        Iterable<Relationship> collected = study.getSpecimens();
        for (Relationship coll : collected) {
            Specimen specimen = new Specimen(coll.getEndNode());
            Location sampleLocation = specimen.getSampleLocation();
            assertNotNull(sampleLocation);
            assertThat(sampleLocation.getLatitude() > 0, is(true));
            assertThat(sampleLocation.getLongitude() < 0, is(true));
            Collection<EcoRegion> ecoRegions = nodeFactory.getEcoRegionFinder().findEcoRegion(sampleLocation.getLatitude(), sampleLocation.getLongitude());
            String ecoRegionId = "";
            for (EcoRegion ecoRegion : ecoRegions) {
                ecoRegionId = ecoRegion.getId();
            }

            Long property = (Long) coll.getProperty(Specimen.DATE_IN_UNIX_EPOCH);
            DateTime dateTime = new DateTime(property);

            Iterable<Relationship> stomachContents = specimen.getStomachContents();
            for (Relationship preyRel : stomachContents) {
                Specimen preySpecimen = new Specimen(preyRel.getEndNode());
                csvOut.writeln();
                csvOut.write(specimen.getExternalId());
                csvOut.write(specimen.getOriginalTaxonDescription());
                csvOut.write(specimen.getLengthInMm().toString());
                csvOut.write(preySpecimen.getOriginalTaxonDescription());
                csvOut.write(Integer.toString(dateTime.getYear()));
                csvOut.write(Integer.toString(dateTime.getMonthOfYear()));
                csvOut.write(ecoRegionId);
            }

        }
        fos.flush();
        IOUtils.closeQuietly(fos);

        StringWriter writer = new StringWriter();
        IOUtils.copy(new FileInputStream(tempCsvExport), writer);

        assertThat(writer.toString(), containsString("Mustelus canis"));
        LOG.info("test import done.");
        tempCsvExport.delete();
    }

    @Test
    public void parseDateTimeInvalid() {
        DateTime dateTime = StudyImporterForFWDP.parseDateTime(null, null, null, null, null);
        assertThat(dateTime, is(nullValue()));
    }

    @Test
    public void parseDateTimeValid() {
        DateTime dateTime = StudyImporterForFWDP.parseDateTime("1992", "12", "12", null, null);
        assertThat(dateTime, is(notNullValue()));
    }

    @Test(expected = org.joda.time.IllegalFieldValueException.class)
    public void parseDateTimeInvalid2() {
        DateTime dateTime = StudyImporterForFWDP.parseDateTime("1992", "12", "48", null, null);
        assertThat(dateTime, is(nullValue()));
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void parseDateTimeInvalid3() {
        DateTime dateTime = StudyImporterForFWDP.parseDateTime(".", ".", ".", null, null);
        assertThat(dateTime, is(nullValue()));
    }

    @Test
    public void importAFewLines() throws StudyImporterException, NodeFactoryException {
        String aFewLines = "Obs,PYNAM,PYABBR,ANALCAT,ANALSCI,COLLCAT,COLLSCI,STATION,PDSEX,PDLEN,PDID,PDWGT,FHDAT,cruise6,svspp,sizecat,pdgutw,pdgutv,STRATUM,SETDEPTH,SURFTEMP,BOTTEMP,BEGLAT,BEGLON,hour,minute,month,day,year,PURCODE,SEASON,yr2block,yr3block,yr5block,decade,geoarea,declat,declon,CATWGT,CATNUM,NUMLEN,PYNUM,pyamtw,pyamtv,pdscinam,pdcomnam\n" +
                "1,CEPHALOPODA,CEPHAL,CEPHAL,CEPHALOPODA,CEPHAL,CEPHALOPODA,45,1,78,2,.,,197702,13,M,3.12,2.84,1610,35,11.5,10,3549,7504,,,3,24,1977,10,SPRING,1977.5,1977,1980,1970s,MAB,35.8167,75.0667,24,12,1,2,0.84,0.76,MUSTELUS CANIS,SMOOTH DOGFISH\n" +
                "2,CRUSTACEA,CRUSTA,CRUSTA,CRUSTACEA,CRUSTA,CRUSTACEA,45,1,78,2,.,,197702,13,M,3.12,2.84,1610,35,11.5,10,3549,7504,,,3,24,1977,10,SPRING,1977.5,1977,1980,1970s,MAB,35.8167,75.0667,24,12,1,.,2,1.82,MUSTELUS CANIS,SMOOTH DOGFISH\n" +
                "3,POLYCHAETA,POLYCH,ANNELI,ANNELIDA,POLYCH,POLYCHAETA,45,1,78,2,.,,197702,13,M,3.12,2.84,1610,35,11.5,10,3549,7504,,,3,24,1977,10,SPRING,1977.5,1977,1980,1970s,MAB,35.8167,75.0667,24,12,1,.,0.28,0.26,MUSTELUS CANIS,SMOOTH DOGFISH\n" +
                "4,ANIMAL REMAINS,ANIREM,AR,ANIMAL REMAINS,AR,ANIMAL REMAINS,45,1,74,3,.,,197702,13,M,0.03,0.02,1610,35,11.5,10,3549,7504,,,3,24,1977,10,SPRING,1977.5,1977,1980,1970s,MAB,35.8167,75.0667,24,12,1,.,0.03,0.02,MUSTELUS CANIS,SMOOTH DOGFISH\n" +
                "5,CRUSTACEA,CRUSTA,CRUSTA,CRUSTACEA,CRUSTA,CRUSTACEA,45,1,104,4,.,,197702,13,L,0.73,0.67,1610,35,11.5,10,3549,7504,,,3,24,1977,10,SPRING,1977.5,1977,1980,1970s,MAB,35.8167,75.0667,24,12,.,.,0.73,0.67,MUSTELUS CANIS,SMOOTH DOGFISH\n" +
                "6,FISH,FISH,OTHFIS,OTHER FISH,OTHFIS,OTHER FISH,45,1,102,5,.,,197702,13,L,43.25,39.32,1610,35,11.5,10,3549,7504,,,3,24,1977,10,SPRING,1977.5,1977,1980,1970s,MAB,35.8167,75.0667,24,12,1,.,43.25,39.32,MUSTELUS CANIS,SMOOTH DOGFISH\n" +
                "7,CRUSTACEA,CRUSTA,CRUSTA,CRUSTACEA,CRUSTA,CRUSTACEA,45,1,58,10,.,,197702,13,M,4.94,4.49,1610,35,11.5,10,3549,7504,,,3,24,1977,10,SPRING,1977.5,1977,1980,1970s,MAB,35.8167,75.0667,24,12,.,.,4.78,4.34,MUSTELUS CANIS,SMOOTH DOGFISH\n";


        StudyImporterForFWDP studyImporter = new StudyImporterForFWDP(new TestParserFactory(aFewLines), nodeFactory);

        studyImporter.importStudy();

        assertThat(nodeFactory.findTaxon("Mustelus canis"), is(notNullValue()));

    }


}

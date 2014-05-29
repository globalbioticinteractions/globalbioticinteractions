package org.eol.globi.data;

import com.Ostermiller.util.LabeledCSVParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eol.globi.domain.Location;
import org.eol.globi.domain.Specimen;
import org.eol.globi.domain.Study;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudyImporterForFishbase extends BaseStudyImporter {
    private static final Log LOG = LogFactory.getLog(StudyImporterForFishbase.class);

    public static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.dateTimeParser();

    public StudyImporterForFishbase(ParserFactory parserFactory, NodeFactory nodeFactory) {
        super(parserFactory, nodeFactory);
    }

    @Override
    public Study importStudy() throws StudyImporterException {
        String studyResource = "fishbase/fooditems.tsv";

        Map<String, Long> predatorSpecimenMap = new HashMap<String, Long>();
        try {
            LabeledCSVParser parser = parserFactory.createParser(studyResource, CharsetConstant.UTF8);
            parser.changeDelimiter('\t');
            while (parser.getLine() != null) {
                int lastLineNumber = parser.getLastLineNumber();
                if (importFilter.shouldImportRecord((long) lastLineNumber)) {
                    String foodName = StringUtils.join(new String[] {parser.getValueByLabel("food item genus"),
                            parser.getValueByLabel("food item species")}, " ");
                    if (StringUtils.isBlank(foodName) || StringUtils.contains(foodName, "NULL")) {
                        foodName = parser.getValueByLabel("food III");
                    }


                    String consumerName = StringUtils.join(new String[] {parser.getValueByLabel("consumer genus"),
                            parser.getValueByLabel("consumer species")}, " ");


                    String author = parser.getValueByLabel("author");
                    String year = parser.getValueByLabel("year");
                    String title = parser.getValueByLabel("title");
                    Study study = nodeFactory.getOrCreateStudy("Fishbase-" + author + year,
                            author,
                            "",
                            "",
                            title
                            , year
                            , "Database export shared by http://fishbase.org in December 2013. For use by Brian Hayden and Jorrit Poelen only.", null);

                    Specimen predator;
                    try {
                        predator = nodeFactory.createSpecimen(consumerName);
                        predator.ate(nodeFactory.createSpecimen(foodName));
                    } catch (NodeFactoryException e) {
                        throw new StudyImporterException("failed to create specimen [" + consumerName + "] on line [" + parser.lastLineNumber() + 1 + "]", e);
                    }
                    study.collected(predator);


                    parser.getValueByLabel("locality");
                    parser.getValueByLabel("countryCode");
                    String latitude = StringUtils.replace(parser.getValueByLabel("latitude"), "NULL", "");
                    String longitude = StringUtils.replace(parser.getValueByLabel("longitude"), "NULL", "");

                    if (StringUtils.isNotBlank(latitude) && StringUtils.isNotBlank(longitude))  {
                        try {
                            predator.caughtIn(nodeFactory.getOrCreateLocation(Double.parseDouble(latitude),
                                    Double.parseDouble(longitude), null));
                        } catch (NodeFactoryException e) {
                            throw new StudyImporterException("failed to create location using [" + latitude + "] and [" + longitude + "] on line [" + parser.lastLineNumber() + 1 + "]", e);

                        } catch (NumberFormatException e) {
                            throw new StudyImporterException("failed to create location using [" + latitude + "] and [" + longitude + "] on line [" + parser.lastLineNumber() + 1 + "]", e);
                        }
                    }



                }
            }
        } catch (IOException e) {
            throw new StudyImporterException("failed to access resource [" + studyResource + "]");
        }

        return null;
    }
}

package org.trophic.graph.data;

import com.Ostermiller.util.CSVParser;
import com.Ostermiller.util.LabeledCSVParser;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

@Component
public class ParserFactoryImpl implements ParserFactory {

    public LabeledCSVParser createParser() throws IOException {
        return new LabeledCSVParser(new CSVParser(new GZIPInputStream(getClass().getResourceAsStream("mississippiAlabamaFishDiet.csv.gz"))));
    }

}

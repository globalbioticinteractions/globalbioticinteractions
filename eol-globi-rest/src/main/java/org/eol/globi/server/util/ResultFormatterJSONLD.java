package org.eol.globi.server.util;

public class ResultFormatterJSONLD implements ResultFormatter {

    @Override
    public String format(String result) throws ResultFormattingException {
       return "{\"@context\": \"https://raw.githubusercontent.com/globalbioticinteractions/jsonld-template-dataset/master/context.jsonld\"}";
    }
}

package org.eol.globi.service;

import java.io.IOException;

public interface AuthorIdResolver {
    String findFullName(String authorURI) throws IOException;
}

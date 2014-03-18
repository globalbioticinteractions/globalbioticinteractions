package org.eol.globi.service;

import org.eol.globi.domain.Taxon;

interface TermMatchListener {
    void foundTermForName(Long id, String name, Taxon taxon);
}

package org.eol.globi.tool;

import org.nanopub.Nanopub;

import java.io.OutputStream;

public interface NanopubOutputStreamFactory {
    OutputStream outputStreamFor(Nanopub nanopub);
}

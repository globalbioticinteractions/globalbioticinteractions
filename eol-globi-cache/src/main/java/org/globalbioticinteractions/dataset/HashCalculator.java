package org.globalbioticinteractions.dataset;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

public interface HashCalculator {

    String calculateContentHash(InputStream is, OutputStream os) throws IOException, NoSuchAlgorithmException;
}

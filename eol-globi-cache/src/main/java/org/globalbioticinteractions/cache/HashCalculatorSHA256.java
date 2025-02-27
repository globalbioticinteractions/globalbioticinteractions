package org.globalbioticinteractions.cache;

import org.globalbioticinteractions.dataset.HashCalculator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;

public class HashCalculatorSHA256 implements HashCalculator {

    @Override
    public String calculateContentHash(InputStream is, OutputStream os) throws IOException, NoSuchAlgorithmException {
        return calculateContentHash(is, os);
    }

}

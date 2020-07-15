package org.eol.globi.data;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.HttpGet;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class TestUtil {
    public static ResourceService getResourceServiceTest() {
        return resourceName -> {
            HttpGet req = new HttpGet(resourceName);
            String csvString = HttpUtil.executeAndRelease(req, HttpUtil.getFailFastHttpClient());
            return IOUtils.toInputStream(csvString, StandardCharsets.UTF_8);
        };
    }
}

package org.globalbioticinteractions.util;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eol.globi.service.ResourceService;
import org.eol.globi.util.CustomServiceUnavailableStrategy;
import org.eol.globi.util.HttpUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class GitClient {
    private static CloseableHttpClient gitClient;

    public static String getLastCommitSHA1(String baseUrl, ResourceService resourceService) throws IOException {

        URI baseURL = URI.create(baseUrl + "/info/refs?service=git-upload-pack");

        String refs;
        try (InputStream retrieve = resourceService.retrieve(baseURL)) {
            refs = IOUtils.toString(retrieve, StandardCharsets.UTF_8);
        }

//        String refs = gitClient.execute(new HttpGet(baseURL), handler);
        String[] refsSplit = StringUtils.split(refs, "\n");
        if (refsSplit.length < 1) {
            throw new IOException("expected response with at least one newline, but got [" + refs + "]");
        }

        String secondRefsLine = refsSplit[1];
        int offset = "00000108".length();
        int sha1Length = "b33da6c6caad4b094fc5a0b6478c6840f8b5ae21".length();
        String sha1Hash = StringUtils.substring(secondRefsLine, offset, offset + sha1Length);
        if (StringUtils.length(sha1Hash) != sha1Length) {
            throw new IOException("expected sha1 hash with length [" + sha1Length + "], but got [" + sha1Hash + "] with length [" + StringUtils.length(sha1Hash) + "] instead");
        }
        return sha1Hash;
    }

    private static CloseableHttpClient createGitHttpClient() {
        RequestConfig config = RequestConfig.custom()
                .setSocketTimeout(HttpUtil.TIMEOUT_SHORT)
                .setConnectTimeout(HttpUtil.TIMEOUT_SHORT)
                .build();

        HttpClientBuilder clientBuilder = HttpClientBuilder.create()
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .setUserAgent("git/1.8.1")
                .setServiceUnavailableRetryStrategy(new CustomServiceUnavailableStrategy())
                .setDefaultRequestConfig(config);

        return clientBuilder.build();
    }

}

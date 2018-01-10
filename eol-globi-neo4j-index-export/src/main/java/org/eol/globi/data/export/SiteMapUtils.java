package org.eol.globi.data.export;

import com.redfin.sitemapgenerator.ChangeFreq;
import com.redfin.sitemapgenerator.SitemapIndexGenerator;
import com.redfin.sitemapgenerator.WebSitemapGenerator;
import com.redfin.sitemapgenerator.WebSitemapUrl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eol.globi.data.StudyImporterException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class SiteMapUtils {
    public static List<File> generateSiteMapFor(String queryParamName, Set<String> queryParamValues, File baseDir, String siteMapLocation) throws IOException {
        final String hostname = "www.globalbioticinteractions.org";
        final String baseUrl = "http://" + hostname;
        WebSitemapGenerator wsg = WebSitemapGenerator.builder(baseUrl, baseDir)
                .gzip(true)
                .build();
        for (String accordingTo : queryParamValues) {
            URI uri;
            try {
                uri = new URI("http", hostname, "/", queryParamName + accordingTo, null);
            } catch (URISyntaxException e) {
                throw new IOException("unexpected malformed uri", e);
            }
            WebSitemapUrl url = new WebSitemapUrl.Options(StringEscapeUtils.escapeXml(uri.toString()))
                    .lastMod(new Date()).priority(1.0).changeFreq(ChangeFreq.WEEKLY).build();
            wsg.addUrl(url);
        }
        final List<File> maps = wsg.write();

        File outFile = new File(baseDir, "sitemap_index.xml");
        SitemapIndexGenerator sig = (new SitemapIndexGenerator.Options(siteMapLocation, outFile))
                .build();
        if (maps.size() > 1) {
            sig.addUrls(siteMapLocation + "sitemap", ".xml.gz", maps.size()).write();
        }  else {
            sig.addUrl(siteMapLocation + "sitemap.xml.gz");
        }
        sig.write();

        return maps;
    }

    public static void generateSiteMap(Set<String> names, String baseDirPath, String queryParamName, String siteMapLocation) throws StudyImporterException {
        try {
            final File baseDir = new File(baseDirPath);
            FileUtils.forceMkdir(baseDir);
            generateSiteMapFor(queryParamName, names, baseDir, siteMapLocation);
        } catch (IOException e) {
            throw new StudyImporterException("failed to generate site map", e);
        }
    }
}

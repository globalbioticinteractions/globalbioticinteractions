package org.globalbioticinteractions.doi;

import java.io.Serializable;
import java.net.URI;

/**
 * Parses and presents Digital Object Identifiers (DOIs, also see <a href="https://doi.org">https://doi.org</a>).
 *
 * <p>Mainly introduced to avoid encoding mistakes like mentioned in http://www.doi.org/doi_handbook/2_Numbering.html#2.5.2.3 :</p>
 *
 * <p>2.5.2.3 Encoding issues</p>
 * <p>There are special encoding requirements when a DOI is used with HTML, URLs, and HTTP. The syntax for Uniform Resource Identifiers (URIs) is much more restrictive than the syntax for the DOI. A URI can be a Uniform Resource Locator (URL) or a Uniform Resource Name (URN).</p
 * <p>Hexadecimal (%) encoding must be used for characters in a DOI that are not allowed, or have other meanings, in URLs or URNs. Hex encoding consists of substituting for the given character its hexadecimal value preceded by percent. Thus, # becomes %23 and https://doi.org/10.1000/456#789 is encoded as https://doi.org/10.1000/456%23789. The browser does not now encounter the bare #, which it would normally treat as the end of the URL and the start of a fragment, and so sends the entire string off to the DOI network of servers for resolution, instead of stopping at the #. Note that the DOI itself does not change with encoding, merely its representation in a URL. A DOI that has been encoded is decoded before being sent to the DOI Registry. At the moment the decoding is handled by the proxy server https://doi.org/. Only unencoded DOIs are stored in the DOI Registry database. For example, the number above is in the DOI Registry as "10.1000/456#789" and not "10.1000/456%23789". The percent character (%) must always be hex encoded (%25) in any URLs.</p>
 * <p>There are few character restrictions for DOI number strings per se. When DOIs are embedded in URLs, they must follow the URL syntax conventions. The same DOI need not follow those conventions in other contexts.The directory indicator shall be "10". The directory indicator distinguishes the entire set of character strings (prefix and suffix) as digital object identifiers within the resolution system.</p>
 *
 * @see <a href="https://doi.org">https://doi.org</a>
 */

public final class DOI implements Serializable {

    private final static String DIRECTORY_INDICATOR = "10";
    private final static String DIRECTORY_INDICATOR_PREFIX = DIRECTORY_INDICATOR + ".";
    private final String registrantCode;
    private final String suffix;

    public DOI(String registrantCode, String suffix) {
        this.registrantCode = registrantCode;
        this.suffix = suffix;
    }

    /**
     * Returns DOI suffix as defined in https://www.doi.org/doi_handbook/2_Numbering.html#2.2.3 :
     *
     * <b>2.2.3 DOI suffix</b>

     <p>The DOI suffix shall consist of a character string of any length chosen by the registrant. Each suffix shall be unique to the prefix element that precedes it. The unique suffix can be a sequential number, or it might incorporate an identifier generated from or based on another system used by the registrant (e.g. ISAN, ISBN, ISRC, ISSN, ISTC, ISNI; in such cases, a preferred construction for such a suffix can be specified, as in Example 1).</p>

     <b>EXAMPLE 1</b>
     <p>10.1000/123456	DOI name with the DOI prefix "10.1000" and the DOI suffix "123456".</p>
     EXAMPLE 2
     10.1038/issn.1476-4687   	DOI suffix using an ISSN. To construct a DOI suffix using an ISSN, precede the ISSN (including the hyphen) with the lowercase letters "issn" and a period, as in this hypothetical example of a DOI for the electronic version of Nature.

     * </quote>
     *
     * @return DOI suffix
     */

    public String getSuffix() {
        return suffix;
    }

    /**
     * Returns DOI prefix as defined in <a href="https://www.doi.org/doi_handbook/2_Numbering.html#2.2.2">2.2.2 DOI prefix</a> of the DOI handbook :
     *
     * 2.2.2 DOI prefix

     General

     The DOI prefix shall be composed of a directory indicator followed by a registrant code. These two components shall be separated by a full stop (period).

     Directory indicator

     The directory indicator shall be "10". The directory indicator distinguishes the entire set of character strings (prefix and suffix) as digital object identifiers within the resolution system.

     Registrant code

     The second element of the DOI prefix shall be the registrant code. The registrant code is a unique string assigned to a registrant.
     *
     * @return DOI prefix
     */

    public String getPrefix() {
        return DIRECTORY_INDICATOR_PREFIX + registrantCode;
    }

    /**
     * Returns the DOI Directory Indicator. According to <a href="https://www.doi.org/doi_handbook/2_Numbering.html#2.2.2">2.2.2 DOI prefix</a> of the DOI handbook :
     *
     * <quote>The directory indicator shall be "10". The directory indicator distinguishes the entire set of character strings (prefix and suffix) as digital object identifiers within the resolution system.</quote>

     * @return directory indicator (always "10")
     */

    public String getDirectoryIndicator() {
        return DIRECTORY_INDICATOR;
    }

    /**
     * Returns DOI Registrant Code as defined in <a href="https://www.doi.org/doi_handbook/2_Numbering.html#2.2.2">2.2.2 DOI prefix</a> of the DOI handbook :
     *
     * <quote>The second element of the DOI prefix shall be the registrant code. The registrant code is a unique string assigned to a registrant.
     * </quote>
     *
     * @return DOI registrant code
     */

    public String getRegistrantCode() {
        return registrantCode;
    }

    /**
     * Returns printable string as defined in <a href="https://www.doi.org/doi_handbook/2_Numbering.html#2.6.1">2.6.1 Screen and print presentation</a> of the DOI handbook :
     *
     * <p>When displayed on screen or in print, a DOI name is preceded by a lowercase "doi:" unless the context clearly indicates that a DOI name is implied. The "doi:" label is not part of the DOI name value.</p>
     * <p>EXAMPLE</p>
     * <p>The DOI name "10.1006/jmbi.1998.2354" is displayed and printed as "doi:10.1006/jmbi.1998.2354".</p>
     *
     * @return doi string for use in print or display
     */

    public String getPrintableDOI() {
        return String.format("doi:%s", this.toString());
    }

    /**
     * @return URI presentation as described in http://www.doi.org/doi_handbook/2_Numbering.html#2.6.2 using default resolver https://doi.org/
     */
    public URI toURI() {
        return DOIUtil.URIForDoi(this);
    }

    /**
     * @param resolver resolver (e.g., https://doi.org , http://dx.doi.org) to be used
     * @return URI presentation as described in http://www.doi.org/doi_handbook/2_Numbering.html#2.6.2 using specified resolver
     */

    public URI toURI(URI resolver) {
        return DOIUtil.URIForDoi(this, resolver);
    }

    /**
     * Creates a DOI from commonly used DOI presentations, including:
     * <ul>
     * <li>"pure" DOIs (e.g., 10.123/456)</li>
     * <li>printable DOIs (e.g., doi:10.123/456)</li>
     * <li>DOI URIs like https://doi.org/[some escaped doi] and http://dx.doi.org/[some escaped doi]</li>
     * </ul>
     *
     * @param doiString a string containing a doi.
     * @return a well-formed DOI
     * @throws MalformedDOIException
     */
    public static DOI create(String doiString) throws MalformedDOIException {
        String s = DOIUtil.stripDOIPrefix(doiString);
        return getDOI(s);
    }

    /**
     * Creates a DOI from a well-formed DOI URI, decoding DOIs when necessary.
     *
     * For instance, an URI https://doi.org/10.1000/456%23789 results in a doi 10.1000/456#789 .
     *
     * @param doiURI a well-formed DOI URI
     * @return well-formed DOI
     * @throws MalformedDOIException
     */
    public static DOI create(URI doiURI) throws MalformedDOIException {
        String path = doiURI == null ? "" : doiURI.getPath();
        int i = path.indexOf('/');
        if (i != 0) {
            throw new MalformedDOIException("path [" + path + "] does not start with [/]");
        }
        return getDOI(path.substring(1));
    }

    private static DOI getDOI(String doiCandidate) throws MalformedDOIException {
        if (!doiCandidate.startsWith(DIRECTORY_INDICATOR_PREFIX)) {
            throw new MalformedDOIException("expected directory indicator [10.] in [" + doiCandidate + "]");
        }

        int s = doiCandidate.indexOf('/');
        if (s < DIRECTORY_INDICATOR_PREFIX.length()) {
            throw new MalformedDOIException("missing registrant code in [" + doiCandidate + "]");
        }
        if (s < DIRECTORY_INDICATOR_PREFIX.length() + 1) {
            throw new MalformedDOIException("missing suffix in [" + doiCandidate + "]");
        }
        String registrantCode = doiCandidate.substring(DIRECTORY_INDICATOR_PREFIX.length(), s);
        String suffix = doiCandidate.substring(s + 1);
        return new DOI(registrantCode, suffix);
    }

    @Override
    public String toString() {
        return String.format("%s.%s/%s", DIRECTORY_INDICATOR, registrantCode, suffix);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null && obj instanceof DOI)
                && sameAs((DOI) obj);
    }

    private boolean sameAs(DOI other) {
        return other != null && this.toString().equalsIgnoreCase(other.toString());
    }

    /**
     * Utility method to check whether a prefix is commonly used for DOIs.
     *
     * @param idPrefix a string prefix
     * @return true if the prefix is a commonly used doi prefix (e.g., "doi:",  "https://doi.org/")
     */

    public static boolean isCommonlyUsedDoiPrefix(String idPrefix) {
        String prefixLower = idPrefix == null ? "" : idPrefix.toLowerCase();
        return DOIUtil.PRINTABLE_DOI_PREFIX.contains(prefixLower) || DOIUtil.DOI_URLS.contains(prefixLower);
    }

}

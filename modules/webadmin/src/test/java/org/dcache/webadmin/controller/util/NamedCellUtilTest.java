package org.dcache.webadmin.controller.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.dcache.webadmin.model.dataaccess.impl.XMLDataGathererHelper;
import org.dcache.webadmin.model.exceptions.ParsingException;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jans
 */
public class NamedCellUtilTest {

    private static final String GPLAZMA_CELL = "gPlazma";
    private static final String EMPTY_DOMAIN = "";
    private static Map<String, List<String>> _domainsMap;

    @BeforeClass
    public static void setUpClass() throws ParsingException {
        _domainsMap = XMLDataGathererHelper.getDomainsMap();
    }

    @Test
    public void testFindWithEmptyDomainsMap() {
        Map<String, List<String>> emptyMap = new HashMap<String, List<String>>();
        String domain = NamedCellUtil.findDomainOfUniqueCell(emptyMap, GPLAZMA_CELL);
        assertEquals(EMPTY_DOMAIN, domain);
    }

    @Test
    public void testSuccessfulFind() {
        String domain = NamedCellUtil.findDomainOfUniqueCell(_domainsMap,
                XMLDataGathererHelper.POOL1_NAME);
        assertEquals(XMLDataGathererHelper.POOL1_DOMAIN, domain);
    }
}
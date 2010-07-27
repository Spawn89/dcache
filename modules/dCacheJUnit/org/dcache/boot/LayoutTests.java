package org.dcache.boot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;

import org.dcache.util.ReplaceableProperties;
import org.junit.Before;
import org.junit.Test;

public class LayoutTests {

    private static final String PROPERTY_DOMAIN_NAME_KEY = "domain.name";

    Layout _layout;
    LayoutStringBuffer _readerSource;

    @Before
    public void setUp() {
        _layout = new Layout( new ReplaceableProperties( new Properties()));
        _readerSource = new LayoutStringBuffer();
    }

    @Test
    public void testLoadSingleDomain() throws IOException {
        String domainName = "domainName";

        _readerSource.appendDomain( domainName);
        load();

        Domain domain = _layout.getDomain(domainName);
        assertNotNull(domain);
        assertDomainPropertySize( domain, 1);
        assertEquals(domainName, domain.getName());
        assertDomainHasProperty( domain, PROPERTY_DOMAIN_NAME_KEY, domainName);
    }

    @Test
    public void testLoadSingleDomainWithProperty() throws IOException {
        String domainName = "domainName";
        String propertyName = "foo";
        String propertyValue = "bar";

        _readerSource.appendDomain( domainName);
        _readerSource.addProperty( propertyName, propertyValue);

        load();

        Domain domain = _layout.getDomain(domainName);
        assertNotNull(domain);
        assertDomainPropertySize( domain, 2);
        assertDomainHasProperty( domain, PROPERTY_DOMAIN_NAME_KEY, domainName);
        assertDomainHasProperty( domain, propertyName, propertyValue);
    }

    @Test
    public void testLoadSingleDomainWithGlobalProperty() throws IOException {
        String domainName = "domainName";
        String propertyName = "foo";
        String propertyValue = "bar";

        _readerSource.addProperty(propertyName, propertyValue);
        _readerSource.appendDomain(domainName);
        load();

        Domain domain = _layout.getDomain(domainName);
        assertNotNull(domain);
        assertDomainPropertySize( domain, 1);
        assertDomainHasProperty( domain, PROPERTY_DOMAIN_NAME_KEY, domainName);
        assertDomainHasProperty( domain, propertyName, propertyValue);
    }

    @Test
    public void testLoadSingleDomainWithService() throws IOException {
        String domainName = "domainName";
        String serviceName = "serviceName";

        _readerSource.appendDomain( domainName);
        _readerSource.appendService( domainName, serviceName);
        load();

        Domain domain = _layout.getDomain(domainName);
        assertNotNull(domain);
        assertDomainPropertySize( domain, 1);
        assertDomainHasProperty( domain, PROPERTY_DOMAIN_NAME_KEY, domainName);

        assertDomainServicesSize( domain, 1);

        ReplaceableProperties serviceProperties = domain.getServices().get(0);
        assertServicePropertySize( serviceProperties, 1);
        assertServiceHasProperty( serviceProperties, PROPERTY_DOMAIN_NAME_KEY, domainName);
    }


    @Test
    public void testSimpleLoadWithLeadingSpace() throws IOException {
        String domainName = "domainName";

        _readerSource.append(" ");
        _readerSource.appendDomain(domainName);
        load();

        Domain domain = _layout.getDomain(domainName);
        assertNotNull(domain);
    }

    /*
     * SUPPORT METHODS
     */

    private void assertDomainHasProperty( Domain domain, String propertyKey, String expectedValue) {
        Properties properties = domain.properties();
        assertEquals( expectedValue, properties.getProperty( propertyKey));
    }

    private void assertDomainPropertySize( Domain domain, int expectedSize) {
        assertEquals(expectedSize,domain.properties().size());
    }

    private void assertDomainServicesSize( Domain domain, int expectedSize) {
        List<ReplaceableProperties> services = domain.getServices();
        assertEquals( expectedSize, services.size());
    }

    private void assertServiceHasProperty( ReplaceableProperties properties, String propertyKey, String expectedValue) {
        assertEquals( expectedValue, properties.getProperty( propertyKey));
    }

    private void assertServicePropertySize( ReplaceableProperties properties, int expectedSize) {
        assertEquals(expectedSize,properties.size());
    }


    private void load() throws IOException {
        StringReader r = new StringReader(_readerSource.toString());
        BufferedReader reader = new BufferedReader(r);
        _layout.load(reader);
    }

    class LayoutStringBuffer {
        final private StringBuffer _sb = new StringBuffer();

        public LayoutStringBuffer append( String string) {
            _sb.append(string);
            return this;
        }

        public LayoutStringBuffer appendLine( String line) {
            _sb.append(line).append("\n");
            return this;
        }

        public LayoutStringBuffer appendDomain( String domainName) {
            appendLine( "[" + domainName + "]");
            return this;
        }

        public LayoutStringBuffer appendService( String domainName, String serviceName) {
            appendLine( "[" + domainName + "/" + serviceName + "]");
            return this;
        }

        public LayoutStringBuffer addProperty( String key, String value) {
            appendLine( key + "=" + value);
            return this;
        }

        @Override
        public String toString() {
            return _sb.toString();
        }
    }
}
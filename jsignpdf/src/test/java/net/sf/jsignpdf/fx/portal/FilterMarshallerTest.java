package net.sf.jsignpdf.fx.portal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;

public class FilterMarshallerTest {

    @Test
    public void marshal_preservesInsertionOrder() {
        // Site 3 (CertificateSettingsController): "All Files" MUST be index 0.
        List<ExtensionFilter> input = Arrays.asList(
                ExtensionFilter.of("All Files", "*.*"),
                ExtensionFilter.of("PKCS12", "*.p12", "*.pfx"),
                ExtensionFilter.of("JKS", "*.jks")
        );

        List<PortalFilter> result = FilterMarshaller.marshal(input);

        assertEquals("Filter count preserved", 3, result.size());
        assertEquals("Index 0 must be All Files", "All Files", result.get(0).getLabel());
        assertEquals("Index 1 must be PKCS12",    "PKCS12",    result.get(1).getLabel());
        assertEquals("Index 2 must be JKS",        "JKS",       result.get(2).getLabel());
    }

    @Test
    public void marshal_singleGlobFilter() {
        List<ExtensionFilter> input = List.of(ExtensionFilter.of("PDF Files", "*.pdf"));
        List<PortalFilter> result = FilterMarshaller.marshal(input);

        assertEquals(1, result.size());
        PortalFilter pf = result.get(0);
        assertEquals("PDF Files", pf.getLabel());
        assertEquals(1, pf.getPatterns().size());
        assertEquals(0, pf.getPatterns().get(0).getType().intValue()); // glob type
        assertEquals("*.pdf", pf.getPatterns().get(0).getPattern());
    }

    @Test
    public void marshal_multiGlobFilter() {
        // Image Files → png, jpg, jpeg, gif
        List<ExtensionFilter> input = List.of(
                ExtensionFilter.of("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        List<PortalFilter> result = FilterMarshaller.marshal(input);

        assertEquals(1, result.size());
        PortalFilter pf = result.get(0);
        assertEquals("Image Files", pf.getLabel());
        assertEquals(4, pf.getPatterns().size());
        assertEquals("*.png",  pf.getPatterns().get(0).getPattern());
        assertEquals("*.gif",  pf.getPatterns().get(3).getPattern());
        // all are glob type 0
        for (FilterPattern fp : pf.getPatterns()) {
            assertEquals(0, fp.getType().intValue());
        }
    }

    @Test
    public void toPortalFilter_preservesLabel() {
        ExtensionFilter f = ExtensionFilter.of("All Files", "*.*");
        PortalFilter pf = FilterMarshaller.toPortalFilter(f);
        assertEquals("All Files", pf.getLabel());
        assertEquals(1, pf.getPatterns().size());
        assertEquals("*.*", pf.getPatterns().get(0).getPattern());
    }

    @Test
    public void marshal_emptyList() {
        List<PortalFilter> result = FilterMarshaller.marshal(List.of());
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}

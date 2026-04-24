package net.sf.jsignpdf.fx.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.file.Path;
import java.util.List;

import org.junit.Test;

import net.sf.jsignpdf.fx.util.NativeFileChooser.ExtensionFilter;

/**
 * Unit tests for {@link NativeFileChooser} logic that does not require a running portal or FX
 * runtime (extension auto-append, filter construction).
 */
public class NativeFileChooserTest {

    // -----------------------------------------------------------------------
    // Extension auto-append
    // -----------------------------------------------------------------------

    private NativeFileChooser chooser(List<ExtensionFilter> filters, ExtensionFilter selected) {
        NativeFileChooser c = new NativeFileChooser();
        for (ExtensionFilter f : filters) c.addFilter(f);
        if (selected != null) c.setSelectedFilter(selected);
        return c;
    }

    @Test
    public void appendExtension_addsWhenNoExtension() {
        NativeFileChooser c = chooser(
                List.of(ExtensionFilter.of("PDF Files", "*.pdf")), null);
        Path input = Path.of("/tmp/myfile");
        Path result = c.appendExtensionIfNeeded(input);
        assertEquals("/tmp/myfile.pdf", result.toString());
    }

    @Test
    public void appendExtension_noOpWhenAlreadyHasExtension() {
        NativeFileChooser c = chooser(
                List.of(ExtensionFilter.of("PDF Files", "*.pdf")), null);
        Path input = Path.of("/tmp/myfile.pdf");
        Path result = c.appendExtensionIfNeeded(input);
        assertEquals("/tmp/myfile.pdf", result.toString());
    }

    @Test
    public void appendExtension_noOpForMultiGlob() {
        // Image Files has multiple globs — should not append anything.
        NativeFileChooser c = chooser(
                List.of(ExtensionFilter.of("Image Files", "*.png", "*.jpg", "*.gif")), null);
        Path input = Path.of("/tmp/image");
        Path result = c.appendExtensionIfNeeded(input);
        assertEquals("/tmp/image", result.toString());
    }

    @Test
    public void appendExtension_noOpForAllFilesWildcard() {
        NativeFileChooser c = chooser(
                List.of(ExtensionFilter.of("All Files", "*.*")), null);
        Path input = Path.of("/tmp/myfile");
        Path result = c.appendExtensionIfNeeded(input);
        assertEquals("/tmp/myfile", result.toString()); // *.* is not a simple extension
    }

    @Test
    public void appendExtension_honorsSelectedFilterOverDefault() {
        NativeFileChooser c = chooser(
                List.of(
                        ExtensionFilter.of("PDF Files", "*.pdf"),
                        ExtensionFilter.of("All Files", "*.*")),
                ExtensionFilter.of("PDF Files", "*.pdf"));
        Path input = Path.of("/tmp/report");
        Path result = c.appendExtensionIfNeeded(input);
        assertEquals("/tmp/report.pdf", result.toString());
    }

    @Test
    public void appendExtension_noFilters_unchanged() {
        NativeFileChooser c = new NativeFileChooser();
        Path input = Path.of("/tmp/myfile");
        Path result = c.appendExtensionIfNeeded(input);
        assertEquals("/tmp/myfile", result.toString());
    }

    @Test
    public void appendExtension_dotInDirectoryName_usesFilenameOnly() {
        // Path like /tmp/my.dir/filename — "my.dir" has a dot but the filename doesn't.
        // The filename is "filename" (no dot), so extension should be appended.
        NativeFileChooser c = chooser(
                List.of(ExtensionFilter.of("PDF Files", "*.pdf")), null);
        Path input = Path.of("/tmp/my.dir/filename");
        Path result = c.appendExtensionIfNeeded(input);
        // getFileName() returns "filename" which has no dot → append
        assertEquals("/tmp/my.dir/filename.pdf", result.toString());
    }

    @Test
    public void appendExtension_usesDefaultFilterWhenSelectedNotSet() {
        NativeFileChooser c = chooser(
                List.of(
                        ExtensionFilter.of("PDF Files", "*.pdf"),
                        ExtensionFilter.of("All Files", "*.*")),
                null); // no selectedFilter
        Path input = Path.of("/tmp/output");
        Path result = c.appendExtensionIfNeeded(input);
        assertEquals("/tmp/output.pdf", result.toString()); // first filter = PDF
    }

    // -----------------------------------------------------------------------
    // ExtensionFilter record
    // -----------------------------------------------------------------------

    @Test
    public void extensionFilter_of_createsRecord() {
        ExtensionFilter f = ExtensionFilter.of("PDF Files", "*.pdf");
        assertEquals("PDF Files", f.description());
        assertEquals(List.of("*.pdf"), f.extensions());
    }

    @Test
    public void extensionFilter_of_multipleExts() {
        ExtensionFilter f = ExtensionFilter.of("Images", "*.png", "*.jpg");
        assertEquals(2, f.extensions().size());
        assertEquals("*.png", f.extensions().get(0));
        assertEquals("*.jpg", f.extensions().get(1));
    }
}

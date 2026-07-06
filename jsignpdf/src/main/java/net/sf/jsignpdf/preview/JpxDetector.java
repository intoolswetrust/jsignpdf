package net.sf.jsignpdf.preview;

import static net.sf.jsignpdf.Constants.LOGGER;

import java.io.File;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.logging.Level;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * Cheap, structural check for whether a PDF contains any JPEG 2000 ({@code /JPXDecode}) image. Walks the page resource
 * dictionaries and their nested form XObjects at the COS level only — no image is decoded. Used to decide whether to
 * offer the optional JPEG 2000 codec download when a document is opened.
 */
public final class JpxDetector {

    private static final COSName JPX_DECODE = COSName.getPDFName("JPXDecode");

    private JpxDetector() {
    }

    /**
     * Returns {@code true} if any page of the document references a {@code /JPXDecode}-encoded image XObject. Any error
     * (unreadable file, wrong password, malformed structure) is swallowed and reported as {@code false} — detection must
     * never block opening a document.
     *
     * @param file the PDF file
     * @param password owner/user password, or {@code null}/empty for none
     */
    public static boolean containsJpx(File file, String password) {
        try (PDDocument doc = Loader.loadPDF(file, password == null ? "" : password)) {
            Set<COSBase> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
            for (PDPage page : doc.getPages()) {
                COSBase resources = resolve(page.getCOSObject().getDictionaryObject(COSName.RESOURCES));
                if (resources instanceof COSDictionary && resourcesContainJpx((COSDictionary) resources, visited)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "JPX detection skipped for " + file, e);
        }
        return false;
    }

    private static boolean resourcesContainJpx(COSDictionary resources, Set<COSBase> visited) {
        if (!visited.add(resources)) {
            return false;
        }
        COSBase xobjBase = resolve(resources.getDictionaryObject(COSName.XOBJECT));
        if (!(xobjBase instanceof COSDictionary)) {
            return false;
        }
        COSDictionary xobjects = (COSDictionary) xobjBase;
        for (COSName name : xobjects.keySet()) {
            COSBase value = resolve(xobjects.getDictionaryObject(name));
            if (!(value instanceof COSDictionary)) {
                continue;
            }
            COSDictionary stream = (COSDictionary) value;
            if (hasJpxFilter(stream)) {
                return true;
            }
            // Form XObjects carry their own /Resources with possibly-nested image XObjects.
            if (COSName.FORM.equals(stream.getDictionaryObject(COSName.SUBTYPE))) {
                COSBase nested = resolve(stream.getDictionaryObject(COSName.RESOURCES));
                if (nested instanceof COSDictionary && resourcesContainJpx((COSDictionary) nested, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasJpxFilter(COSDictionary stream) {
        COSBase filter = resolve(stream.getDictionaryObject(COSName.FILTER));
        if (JPX_DECODE.equals(filter)) {
            return true;
        }
        if (filter instanceof COSArray) {
            for (COSBase entry : (COSArray) filter) {
                if (JPX_DECODE.equals(resolve(entry))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static COSBase resolve(COSBase base) {
        return base instanceof COSObject ? ((COSObject) base).getObject() : base;
    }
}

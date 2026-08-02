package net.sf.jsignpdf;

import static net.sf.jsignpdf.Constants.RES;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.sf.jsignpdf.types.PageInfo;
import net.sf.jsignpdf.types.SignatureFieldInfo;
import net.sf.jsignpdf.utils.PdfUtils;

import org.apache.commons.lang3.StringUtils;
import org.openpdf.text.Rectangle;
import org.openpdf.text.exceptions.BadPasswordException;
import org.openpdf.text.pdf.AcroFields;
import org.openpdf.text.pdf.PdfArray;
import org.openpdf.text.pdf.PdfDictionary;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfNumber;
import org.openpdf.text.pdf.PdfReader;

/**
 * Provides additional information for selected input PDF file.
 * 
 * @author Josef Cacek
 */
public class PdfExtraInfo {

    private BasicSignerOptions options;

    /**
     * @param anOptions
     */
    public PdfExtraInfo(BasicSignerOptions anOptions) {
        options = anOptions;
    }

    /**
     * Returns number of pages in PDF document. If error occures (file not found or sth. similar) -1 is returned.
     *
     * @return number of pages (or -1 if error occures)
     * @throws BadPasswordException if the PDF is password-protected and the configured password is wrong or missing
     */
    public int getNumberOfPages() throws BadPasswordException {
        int tmpResult = 0;
        PdfReader reader = null;
        try {
            try {
                reader = new PdfReader(options.getInFile(), options.getPdfOwnerPwdStrX().getBytes());
            } catch (BadPasswordException e) {
                try {
                    reader = new PdfReader(options.getInFile(), new byte[0]);
                } catch (Exception e2) {
                    reader = new PdfReader(options.getInFile());
                }
            } catch (Exception e) {
                try {
                    reader = new PdfReader(options.getInFile(), new byte[0]);
                } catch (Exception e2) {
                    // try to read without password
                    reader = new PdfReader(options.getInFile());
                }
            }
            tmpResult = reader.getNumberOfPages();
        } catch (BadPasswordException e) {
            throw e;
        } catch (Exception e) {
            tmpResult = -1;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }

        return tmpResult;
    }

    /**
     * Returns page info.
     * 
     * @param aPage number of page for which size should be returned
     * @return FloatPoint or null
     */
    public PageInfo getPageInfo(int aPage) {
        PageInfo tmpResult = null;
        PdfReader reader = null;
        try {
            reader = PdfUtils.getPdfReader(options.getInFile(), options.getPdfOwnerPwdStrX().getBytes());
            final Rectangle tmpRect = reader.getPageSizeWithRotation(aPage);
            if (tmpRect != null) {
                tmpResult = new PageInfo(tmpRect.getRight(), tmpRect.getTop());
            }
        } catch (Exception e) {
            // nothing to do
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }

        return tmpResult;
    }

    /**
     * Returns all signature form fields of the input PDF - both blank and already signed ones - in document
     * order: pages in order, and within a page the order of the widget in the page's {@code /Annots} array.
     * Neither {@code AcroFields.getFieldNamesWithBlankSignatures()} (a {@code HashMap} iteration) nor the
     * AcroForm {@code /Fields} array gives that order, so it is computed here and used both for the
     * {@code --list-sig-fields} output and for resolving the {@code #N} selector - the numbers a user reads
     * are the numbers they can pass back.
     *
     * <p>Unlike the other methods of this class, failures are reported rather than swallowed: the listing
     * command has to tell "this PDF has no signature fields" from "this PDF could not be opened".
     *
     * @return signature fields in document order, numbered from 1 (never {@code null})
     * @throws IOException when the input PDF can't be opened
     */
    public List<SignatureFieldInfo> getSignatureFields() throws IOException {
        PdfReader reader = null;
        try {
            reader = PdfUtils.getPdfReader(options.getInFile(), options.getPdfOwnerPwdStrX().getBytes());
            return readSignatureFields(reader);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // nothing to do
                }
            }
        }
    }

    /**
     * Resolves a {@code --sig-field} selector against the input PDF.
     *
     * @param selector field name, {@code #N} or {@code auto}
     * @return the resolved blank signature field
     * @throws IOException when the input PDF can't be opened
     * @throws SignatureFieldException when the selector doesn't resolve to a signable field
     */
    public SignatureFieldInfo resolveSignatureField(String selector) throws IOException, SignatureFieldException {
        return resolveSignatureField(getSignatureFields(), selector);
    }

    /**
     * Resolves a {@code --sig-field} selector against an already read field list.
     *
     * <p>An exact field name always wins, so a field really named {@code auto} or {@code #1} stays reachable;
     * the selectors are only interpreted when no field carries that literal name.
     *
     * @param fields signature fields in document order
     * @param selector field name, {@code #N} or {@code auto}
     * @return the resolved blank signature field
     * @throws SignatureFieldException when the selector doesn't resolve to a signable field
     */
    public static SignatureFieldInfo resolveSignatureField(List<SignatureFieldInfo> fields, String selector)
            throws SignatureFieldException {
        if (fields.isEmpty()) {
            throw new SignatureFieldException(RES.get("console.sigField.noFields"));
        }
        for (SignatureFieldInfo field : fields) {
            if (field.name().equals(selector)) {
                return requireBlank(field);
            }
        }
        if (Constants.SIG_FIELD_SELECTOR_AUTO.equals(selector)) {
            for (SignatureFieldInfo field : fields) {
                if (field.blank()) {
                    return field;
                }
            }
            throw new SignatureFieldException(RES.get("console.sigField.noBlankField", describe(fields)));
        }
        if (selector != null && selector.startsWith(Constants.SIG_FIELD_SELECTOR_NUMBER_PREFIX)) {
            final String number = selector.substring(Constants.SIG_FIELD_SELECTOR_NUMBER_PREFIX.length());
            if (StringUtils.isNumeric(number) && !number.isEmpty()) {
                final int idx = Integer.parseInt(number);
                if (idx < 1 || idx > fields.size()) {
                    throw new SignatureFieldException(RES.get("console.sigField.numberOutOfRange", selector,
                            String.valueOf(fields.size()), describe(fields)));
                }
                return requireBlank(fields.get(idx - 1));
            }
        }
        throw new SignatureFieldException(
                RES.get("console.sigField.notFound", String.valueOf(selector), describe(fields)));
    }

    private static SignatureFieldInfo requireBlank(SignatureFieldInfo field) throws SignatureFieldException {
        if (field.signed()) {
            throw new SignatureFieldException(RES.get("console.sigField.alreadySigned", field.name()));
        }
        return field;
    }

    /**
     * Renders the available fields for an error message, e.g. {@code #1 Signature1, #2 Signature2 (signed)}.
     */
    public static String describe(List<SignatureFieldInfo> fields) {
        final StringBuilder sb = new StringBuilder();
        for (SignatureFieldInfo field : fields) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(Constants.SIG_FIELD_SELECTOR_NUMBER_PREFIX).append(field.number()).append(' ').append(field.name());
            if (field.signed()) {
                sb.append(' ').append(RES.get("console.sigField.state.signed"));
            }
        }
        return sb.toString();
    }

    static List<SignatureFieldInfo> readSignatureFields(PdfReader reader) {
        final AcroFields acroFields = reader.getAcroFields();
        final List<Widget> widgets = new ArrayList<>();
        for (Map.Entry<String, AcroFields.Item> entry : acroFields.getAllFields().entrySet()) {
            final AcroFields.Item item = entry.getValue();
            if (item == null || item.size() == 0) {
                continue;
            }
            final PdfDictionary merged = item.getMerged(0);
            if (merged == null || !PdfName.SIG.equals(merged.getAsName(PdfName.FT))) {
                continue;
            }
            // Widget 0 is what both engines fill: OpenPDF's setVisibleSignature(String) uses getMerged(0) /
            // getPage(0), DSS's drawer uses the first widget of the field. A multi-widget signature field is
            // pathological; reporting the same widget keeps the listing, the resolution and the output aligned.
            final Integer page = item.getPage(0);
            final Integer tabOrder = item.getTabOrder(0);
            widgets.add(new Widget(entry.getKey(), page == null ? Integer.MAX_VALUE : page,
                    tabOrder == null ? Integer.MAX_VALUE : tabOrder, rectangleOf(merged),
                    merged.get(PdfName.V) != null, isHidden(merged)));
        }
        widgets.sort(Comparator.comparingInt(Widget::page).thenComparingInt(Widget::tabOrder).thenComparing(Widget::name));

        final List<SignatureFieldInfo> result = new ArrayList<>(widgets.size());
        int number = 1;
        for (Widget w : widgets) {
            final Rectangle r = w.rect();
            result.add(new SignatureFieldInfo(number++, w.name(), w.page() == Integer.MAX_VALUE ? 0 : w.page(),
                    r.getLeft(), r.getBottom(), r.getRight(), r.getTop(), w.signed(), w.hidden()));
        }
        return result;
    }

    private static Rectangle rectangleOf(PdfDictionary widget) {
        final PdfArray rect = widget.getAsArray(PdfName.RECT);
        if (rect == null || rect.size() < 4) {
            return new Rectangle(0f, 0f, 0f, 0f);
        }
        final float[] coords = new float[4];
        for (int i = 0; i < 4; i++) {
            final PdfNumber number = rect.getAsNumber(i);
            coords[i] = number == null ? 0f : number.floatValue();
        }
        final Rectangle result = new Rectangle(coords[0], coords[1], coords[2], coords[3]);
        result.normalize();
        return result;
    }

    /**
     * Hidden and NoView widgets are still listed and signable - an invisible pre-placed field is a legitimate
     * authoring choice - but the flag is reported so the listing explains why nothing shows up in a viewer.
     */
    private static boolean isHidden(PdfDictionary widget) {
        final PdfNumber flags = widget.getAsNumber(PdfName.F);
        if (flags == null) {
            return false;
        }
        final int value = flags.intValue();
        return (value & ANNOT_FLAG_HIDDEN) != 0 || (value & ANNOT_FLAG_NO_VIEW) != 0;
    }

    private static final int ANNOT_FLAG_HIDDEN = 2;
    private static final int ANNOT_FLAG_NO_VIEW = 32;

    private record Widget(String name, int page, int tabOrder, Rectangle rect, boolean signed, boolean hidden) {
    }
}

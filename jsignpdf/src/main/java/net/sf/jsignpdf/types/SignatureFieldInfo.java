package net.sf.jsignpdf.types;

/**
 * A signature form field (AcroForm {@code /FT /Sig}) found in a PDF document.
 *
 * <p>The {@link #number()} is the position of the field in document order (pages in order, and within a page
 * the order of the widget in the page's {@code /Annots} array), starting at 1. Both blank and already signed
 * fields are numbered, so a number does not shift under the user as earlier fields get filled. It is the
 * number printed by {@code --list-sig-fields} and accepted by {@code --sig-field #N}.
 *
 * <p>The rectangle is the field widget's {@code /Rect} in PDF user space.
 *
 * @author Josef Cacek
 */
public record SignatureFieldInfo(int number, String name, int page, float llx, float lly, float urx, float ury,
        boolean signed, boolean hidden) {

    public float width() {
        return urx - llx;
    }

    public float height() {
        return ury - lly;
    }

    /**
     * Returns true when the field has no value yet, i.e. it can be signed.
     */
    public boolean blank() {
        return !signed;
    }

    /**
     * Returns true when the field has a non-empty rectangle, i.e. an appearance can be drawn into it. A
     * zero-size rectangle means the field author wanted an invisible signature.
     */
    public boolean hasVisibleRect() {
        return width() > 0f && height() > 0f;
    }
}

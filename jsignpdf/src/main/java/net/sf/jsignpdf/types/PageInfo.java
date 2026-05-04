package net.sf.jsignpdf.types;

/**
 * PDF page info.
 * 
 * @author Josef Cacek
 */
public class PageInfo {

    private final float width;
    private final float height;

    public PageInfo(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}

/*
 * JSignPdf custom DSS PDFBox signature drawer
 *
 * Provides background-image layering, text auto-scaling, and flexible field placement
 * on top of the DSS PAdES signing pipeline.
 *
 * This file is derived from DSS's NativePdfBoxVisibleSignatureDrawer
 * (https://github.com/esig/dss), which is licensed under the GNU Lesser General
 * Public License version 2.1 (LGPL-2.1). JSignPdf is also LGPL-2.1 licensed.
 *
 * Copyright (C) 2025 JSignPdf contributors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.sf.jsignpdf.engine.dss.pdfbox;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.pades.DSSFileFont;
import eu.europa.esig.dss.pades.DSSFont;
import eu.europa.esig.dss.pades.PAdESUtils;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pdf.AnnotationBox;
import eu.europa.esig.dss.pdf.pdfbox.PdfBoxUtils;
import eu.europa.esig.dss.pdf.pdfbox.visible.AbstractPdfBoxSignatureDrawer;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxNativeFont;
import eu.europa.esig.dss.pdf.pdfbox.visible.nativedrawer.PdfBoxDSSFontMetrics;
import eu.europa.esig.dss.pdf.pdfbox.visible.nativedrawer.PdfBoxFontMapper;
import eu.europa.esig.dss.pdf.visible.DSSFontMetrics;
import eu.europa.esig.dss.pdf.visible.ImageRotationUtils;
import eu.europa.esig.dss.pdf.visible.ImageUtils;
import eu.europa.esig.dss.pdf.visible.SignatureFieldDimensionAndPosition;
import eu.europa.esig.dss.spi.signature.resources.DSSResourcesHandler;
import eu.europa.esig.dss.spi.signature.resources.DSSResourcesHandlerBuilder;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceGray;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class JSignPdfOverlaySignatureDrawer extends AbstractPdfBoxSignatureDrawer {

    private static final Logger LOG = LoggerFactory.getLogger(JSignPdfOverlaySignatureDrawer.class);

    private PDFont pdFont;

    private DSSResourcesHandlerBuilder resourcesHandlerBuilder = PAdESUtils.DEFAULT_RESOURCES_HANDLER_BUILDER;

    public void setResourcesHandlerBuilder(DSSResourcesHandlerBuilder resourcesHandlerBuilder) {
        this.resourcesHandlerBuilder = resourcesHandlerBuilder;
    }

    /**
     * Custom init that allows background-only signatures.
     * The DSS {@code super.init()} calls {@code assertSignatureParametersAreValid()} which
     * checks {@code getImage() == null && text.isEmpty()} directly — it never consults the
     * overridden {@link JSignPdfSignatureImageParameters#isEmpty()}, so a signature with only
     * a background image (no foreground graphic, no text) would be rejected. This override
     * replicates the setup from {@code AbstractPdfBoxSignatureDrawer.init()} but applies a
     * validation that respects the background-image field.
     */
    @Override
    public void init(SignatureImageParameters parameters, PDDocument document, SignatureOptions signatureOptions)
            throws IOException {
        boolean hasBackground = parameters instanceof JSignPdfSignatureImageParameters
                && ((JSignPdfSignatureImageParameters) parameters).getBackgroundImage() != null;
        if (parameters.getImage() == null && parameters.getTextParameters().isEmpty() && !hasBackground) {
            throw new IllegalArgumentException("Neither image nor text parameters are defined!");
        }
        this.parameters = parameters;
        this.document = document;
        this.signatureOptions = signatureOptions;
        if (!parameters.getTextParameters().isEmpty()) {
            this.pdFont = initFont();
        }
    }

    private PDFont initFont() throws IOException {
        DSSFont dssFont = parameters.getTextParameters().getFont();
        if (dssFont instanceof PdfBoxNativeFont) {
            PdfBoxNativeFont nativeFont = (PdfBoxNativeFont) dssFont;
            return nativeFont.getFont();
        }
        if (dssFont instanceof DSSFileFont) {
            DSSFileFont fileFont = (DSSFileFont) dssFont;
            try (InputStream is = fileFont.getInputStream()) {
                return PDType0Font.load(document, is, fileFont.isEmbedFontSubset());
            }
        }
        return PdfBoxFontMapper.getPDFont(dssFont.getJavaFont());
    }

    @Override
    protected DSSFontMetrics getDSSFontMetrics() {
        return new PdfBoxDSSFontMetrics(pdFont);
    }

    /**
     * Caps the DSS-calculated text size at the user's preferred font size.
     * With {@link TextWrapping#FILL_BOX_AND_LINEBREAK}, DSS ignores the font
     * size and may produce oversized text on large boxes. This override uses
     * the user's font size as an upper bound: text still auto-scales DOWN for
     * small boxes, but never exceeds the preferred size.
     */
    @Override
    public SignatureFieldDimensionAndPosition buildSignatureFieldBox() {
        SignatureFieldDimensionAndPosition dim = super.buildSignatureFieldBox();
        SignatureImageTextParameters textParams = parameters.getTextParameters();
        if (textParams != null && textParams.getFont() != null) {
            float preferredSize = textParams.getFont().getSize();
            if (preferredSize > 0f && dim.getTextSize() > preferredSize) {
                dim.setTextSize(preferredSize);
            }
        }
        return dim;
    }

    @Override
    public void draw() throws IOException {
        try (DSSResourcesHandler resourcesHandler = resourcesHandlerBuilder.createResourcesHandler();
             OutputStream os = resourcesHandler.createOutputStream();
             PDDocument doc = new PDDocument()) {

            int pageNumber = parameters.getFieldParameters().getPage() - ImageUtils.DEFAULT_FIRST_PAGE;
            PDPage originalPage = document.getPage(pageNumber);
            SignatureFieldDimensionAndPosition dimensionAndPosition = buildSignatureFieldBox();

            PDPage page = new PDPage(originalPage.getMediaBox());
            doc.addPage(page);
            PDAcroForm acroForm = new PDAcroForm(doc);
            doc.getDocumentCatalog().setAcroForm(acroForm);
            PDSignatureField signatureField = new PDSignatureField(acroForm);
            PDAnnotationWidget widget = signatureField.getWidgets().get(0);
            List<PDField> acroFormFields = acroForm.getFields();
            acroForm.setSignaturesExist(true);
            acroForm.setAppendOnly(true);
            acroForm.getCOSObject().setDirect(true);
            acroFormFields.add(signatureField);

            PDRectangle rectangle = getPdRectangle(dimensionAndPosition);
            widget.setRectangle(rectangle);

            PDAppearanceDictionary appearance = PdfBoxUtils.createSignatureAppearanceDictionary(doc, rectangle);
            widget.setAppearance(appearance);

            PDAppearanceStream appearanceStream = appearance.getNormalAppearance().getAppearanceStream();
            try (PDPageContentStream cs = new PDPageContentStream(doc, appearanceStream)) {
                rotateSignature(cs, rectangle, dimensionAndPosition);
                setFieldBackground(cs, parameters.getBackgroundColor());
                DSSDocument background = null;
                float backgroundScale = 0f;
                if (parameters instanceof JSignPdfSignatureImageParameters) {
                    JSignPdfSignatureImageParameters jsignParams = (JSignPdfSignatureImageParameters) parameters;
                    background = jsignParams.getBackgroundImage();
                    backgroundScale = jsignParams.getBackgroundScale();
                }

                if (background != null) {
                    setBackgroundImage(cs, doc, dimensionAndPosition, background, backgroundScale);
                }
                setImageScaledToBox(cs, doc, dimensionAndPosition, parameters.getImage());
                setText(cs, dimensionAndPosition, parameters);
            }

            doc.save(os);
            DSSDocument document = resourcesHandler.writeToDSSDocument();
            try (InputStream is = document.openStream()) {
                signatureOptions.setVisualSignature(is);
            }
        }
    }

    private void rotateSignature(PDPageContentStream cs, PDRectangle rectangle,
            SignatureFieldDimensionAndPosition dimensionAndPosition) throws IOException {
        switch (dimensionAndPosition.getGlobalRotation()) {
        case ImageRotationUtils.ANGLE_90:
            cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_270), 0, 0));
            cs.transform(Matrix.getTranslateInstance(-rectangle.getHeight(), 0));
            break;
        case ImageRotationUtils.ANGLE_180:
            cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_180), 0, 0));
            cs.transform(Matrix.getTranslateInstance(-rectangle.getWidth(), -rectangle.getHeight()));
            break;
        case ImageRotationUtils.ANGLE_270:
            cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_90), 0, 0));
            cs.transform(Matrix.getTranslateInstance(0, -rectangle.getWidth()));
            break;
        case ImageRotationUtils.ANGLE_360:
        case ImageRotationUtils.ANGLE_0:
            break;
        default:
            throw new IllegalStateException(ImageRotationUtils.SUPPORTED_ANGLES_ERROR_MESSAGE);
        }
    }

    private void setFieldBackground(PDPageContentStream cs, Color color) throws IOException {
        setBackground(cs, color, new PDRectangle(-5000, -5000, 10000, 10000));
    }

    private void setBackground(PDPageContentStream cs, Color color, PDRectangle rect) throws IOException {
        if (color != null) {
            setAlphaChannel(cs, color);
            setNonStrokingColor(cs, color);
            cs.addRect(rect.getLowerLeftX(), rect.getLowerLeftY(), rect.getWidth(), rect.getHeight());
            cs.fill();
            cleanTransparency(cs, color);
        }
    }

    private void setImageScaledToBox(PDPageContentStream cs, PDDocument doc,
            SignatureFieldDimensionAndPosition dimensionAndPosition, DSSDocument image) throws IOException {
        if (image == null) {
            return;
        }

        AnnotationBox annotationBox = dimensionAndPosition.getAnnotationBox();
        float boxWidth = annotationBox.getWidth();
        float boxHeight = annotationBox.getHeight();
        AnnotationBox imageBoundaryBox = ImageUtils.getImageBoundaryBox(image);
        float imageWidth = imageBoundaryBox.getWidth();
        float imageHeight = imageBoundaryBox.getHeight();
        if (imageWidth <= 0f || imageHeight <= 0f) {
            return;
        }

        int imageRotation = dimensionAndPosition.getCurrentImageRotation();
        boolean rotated = imageRotation == ImageRotationUtils.ANGLE_90
                || imageRotation == ImageRotationUtils.ANGLE_270;
        if (rotated) {
            float tmp = imageWidth;
            imageWidth = imageHeight;
            imageHeight = tmp;
        }

        float imageRatio = imageWidth / imageHeight;
        float boxRatio = boxWidth / boxHeight;
        float drawWidth;
        float drawHeight;
        float drawX;
        float drawY;
        if (imageRatio < boxRatio) {
            drawHeight = boxHeight;
            drawWidth = boxHeight * imageRatio;
            drawX = (boxWidth - drawWidth) / 2f;
            drawY = 0f;
        } else {
            drawWidth = boxWidth;
            drawHeight = boxWidth / imageRatio;
            drawX = 0f;
            drawY = (boxHeight - drawHeight) / 2f;
        }

        try (InputStream is = image.openStream()) {
            cs.saveGraphicsState();
            applyImageRotation(cs, boxWidth, boxHeight, drawWidth, drawHeight, drawX, drawY, imageRotation);
            byte[] bytes = IOUtils.toByteArray(is);
            PDImageXObject imageXObject = PDImageXObject.createFromByteArray(doc, bytes, image.getName());
            cs.drawImage(imageXObject, drawX, drawY, drawWidth, drawHeight);
            cs.restoreGraphicsState();
        }
    }

    /**
     * Applies a per-image rotation transform so rotated fields render the graphic with
     * the same orientation. Mirror of {@code NativePdfBoxVisibleSignatureDrawer}'s
     * image-rotation handling.
     */
    private void applyImageRotation(PDPageContentStream cs, float boxWidth, float boxHeight,
            float drawWidth, float drawHeight, float drawX, float drawY, int imageRotation) throws IOException {
        switch (imageRotation) {
        case ImageRotationUtils.ANGLE_90:
            cs.transform(Matrix.getTranslateInstance(drawX + drawWidth, drawY));
            cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_90), 0, 0));
            cs.transform(Matrix.getTranslateInstance(-drawX - drawWidth, -drawY));
            break;
        case ImageRotationUtils.ANGLE_180:
            cs.transform(Matrix.getTranslateInstance(drawX + drawWidth, drawY + drawHeight));
            cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_180), 0, 0));
            cs.transform(Matrix.getTranslateInstance(-drawX - drawWidth, -drawY - drawHeight));
            break;
        case ImageRotationUtils.ANGLE_270:
            cs.transform(Matrix.getTranslateInstance(drawX, drawY + drawHeight));
            cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_270), 0, 0));
            cs.transform(Matrix.getTranslateInstance(-drawX, -drawY - drawHeight));
            break;
        case ImageRotationUtils.ANGLE_0:
        case ImageRotationUtils.ANGLE_360:
        default:
            break;
        }
    }

    private void setBackgroundImage(PDPageContentStream cs, PDDocument doc,
            SignatureFieldDimensionAndPosition dimensionAndPosition, DSSDocument image, float bgScale)
            throws IOException {
        AnnotationBox annotationBox = dimensionAndPosition.getAnnotationBox();
        float boxWidth = annotationBox.getWidth();
        float boxHeight = annotationBox.getHeight();
        AnnotationBox imageBoundaryBox = ImageUtils.getImageBoundaryBox(image);
        float imageWidth = imageBoundaryBox.getWidth();
        float imageHeight = imageBoundaryBox.getHeight();
        if (imageWidth <= 0f || imageHeight <= 0f) {
            return;
        }

        float drawWidth;
        float drawHeight;
        float drawX;
        float drawY;

        if (bgScale == 0f) {
            drawWidth = boxWidth;
            drawHeight = boxHeight;
            drawX = 0f;
            drawY = 0f;
        } else {
            float scale = bgScale > 0f ? bgScale : Math.min(boxWidth / imageWidth, boxHeight / imageHeight);
            drawWidth = imageWidth * scale;
            drawHeight = imageHeight * scale;
            drawX = (boxWidth - drawWidth) / 2f;
            drawY = (boxHeight - drawHeight) / 2f;
        }

        try (InputStream is = image.openStream()) {
            cs.saveGraphicsState();
            byte[] bytes = IOUtils.toByteArray(is);
            PDImageXObject imageXObject = PDImageXObject.createFromByteArray(doc, bytes, image.getName());
            cs.drawImage(imageXObject, drawX, drawY, drawWidth, drawHeight);
            cs.restoreGraphicsState();
        }
    }

    private void setText(PDPageContentStream cs, SignatureFieldDimensionAndPosition dimensionAndPosition,
            SignatureImageParameters parameters) throws IOException {
        SignatureImageTextParameters textParameters = parameters.getTextParameters();
        if (!textParameters.isEmpty()) {
            setTextBackground(cs, textParameters, dimensionAndPosition);
            float fontSize = dimensionAndPosition.getTextSize();
            cs.beginText();
            cs.setFont(pdFont, fontSize);
            setNonStrokingColor(cs, textParameters.getTextColor());
            setAlphaChannel(cs, textParameters.getTextColor());

            PdfBoxDSSFontMetrics pdfBoxFontMetrics = new PdfBoxDSSFontMetrics(pdFont);

            String text = dimensionAndPosition.getText();
            String[] strings = pdfBoxFontMetrics.getLines(text);

            float lineHeight = pdfBoxFontMetrics.getHeight(text, dimensionAndPosition.getTextSize());
            cs.setLeading(lineHeight);

            cs.newLineAtOffset(dimensionAndPosition.getTextX(),
                    dimensionAndPosition.getTextHeight() + dimensionAndPosition.getTextY() - fontSize);

            float previousOffset = 0;
            for (String str : strings) {
                float stringWidth = pdfBoxFontMetrics.getWidth(str, fontSize);
                float offsetX = 0;
                switch (textParameters.getSignerTextHorizontalAlignment()) {
                case RIGHT:
                    offsetX = dimensionAndPosition.getTextWidth() - stringWidth - previousOffset;
                    break;
                case CENTER:
                    offsetX = (dimensionAndPosition.getTextWidth() - stringWidth) / 2 - previousOffset;
                    break;
                default:
                    break;
                }
                previousOffset += offsetX;
                cs.newLineAtOffset(offsetX, 0);
                cs.showText(str);
                cs.newLine();
            }
            cs.endText();
            cleanTransparency(cs, textParameters.getTextColor());
        }
    }

    private void setTextBackground(PDPageContentStream cs, SignatureImageTextParameters textParameters,
            SignatureFieldDimensionAndPosition dimensionAndPosition) throws IOException {
        if (textParameters.getBackgroundColor() != null) {
            PDRectangle rect = new PDRectangle(
                    dimensionAndPosition.getTextBoxX(), dimensionAndPosition.getTextBoxY(),
                    dimensionAndPosition.getTextBoxWidth(), dimensionAndPosition.getTextBoxHeight());
            setBackground(cs, textParameters.getBackgroundColor(), rect);
        }
    }

    private void setNonStrokingColor(PDPageContentStream cs, Color color) throws IOException {
        if (color != null) {
            cs.setNonStrokingColor(toPDColor(color));
        }
    }

    private PDColor toPDColor(Color color) {
        float[] components;
        PDColorSpace pdColorSpace;
        if (ImageUtils.isGrayscale(color)) {
            components = new float[] { color.getRed() / 255f };
            pdColorSpace = PDDeviceGray.INSTANCE;
        } else {
            components = new float[] { color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f };
            pdColorSpace = PDDeviceRGB.INSTANCE;
        }
        return new PDColor(components, pdColorSpace);
    }

    private void setAlphaChannel(PDPageContentStream cs, Color color) throws IOException {
        if (color != null) {
            float alpha = color.getAlpha();
            if (alpha < ImageUtils.OPAQUE_VALUE) {
                LOG.warn("Transparency detected and enabled (Be aware: not valid with PDF/A !)");
                setAlpha(cs, alpha);
            }
        }
    }

    private void setAlpha(PDPageContentStream cs, float alpha) throws IOException {
        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(alpha / ImageUtils.OPAQUE_VALUE);
        cs.setGraphicsStateParameters(gs);
    }

    private void cleanTransparency(PDPageContentStream cs, Color color) throws IOException {
        if (color != null) {
            float alpha = color.getAlpha();
            if (alpha < ImageUtils.OPAQUE_VALUE) {
                setAlpha(cs, ImageUtils.OPAQUE_VALUE);
            }
        }
    }

    private PDRectangle getPdRectangle(SignatureFieldDimensionAndPosition dimensionAndPosition) {
        AnnotationBox annotationBox = dimensionAndPosition.getAnnotationBox();

        PDRectangle pdRectangle = new PDRectangle();
        pdRectangle.setLowerLeftX(annotationBox.getMinX());
        pdRectangle.setLowerLeftY(annotationBox.getMinY());
        pdRectangle.setUpperRightX(annotationBox.getMaxX());
        pdRectangle.setUpperRightY(annotationBox.getMaxY());
        return pdRectangle;
    }

    @Override
    protected String getExpectedColorSpaceName() throws IOException {
        // Check the foreground image first
        if (parameters.getImage() != null) {
            try (InputStream is = parameters.getImage().openStream()) {
                byte[] bytes = IOUtils.toByteArray(is);
                PDImageXObject imageXObject = PDImageXObject.createFromByteArray(document, bytes, parameters.getImage().getName());
                PDColorSpace colorSpace = imageXObject.getColorSpace();
                return colorSpace.getName();
            }
        }
        // Also inspect the background image (otherwise a background-only signature with
        // an RGB/CMYK image can get a DEVICEGRAY output intent injected, producing wrong
        // output and a PDF/A inconsistency)
        if (parameters instanceof JSignPdfSignatureImageParameters) {
            JSignPdfSignatureImageParameters jsignParams = (JSignPdfSignatureImageParameters) parameters;
            DSSDocument background = jsignParams.getBackgroundImage();
            if (background != null) {
                try (InputStream is = background.openStream()) {
                    byte[] bytes = IOUtils.toByteArray(is);
                    PDImageXObject imageXObject = PDImageXObject.createFromByteArray(document, bytes, background.getName());
                    PDColorSpace colorSpace = imageXObject.getColorSpace();
                    return colorSpace.getName();
                }
            }
        }
        return ImageUtils.containRGBColor(parameters) ? COSName.DEVICERGB.getName() : COSName.DEVICEGRAY.getName();
    }
}

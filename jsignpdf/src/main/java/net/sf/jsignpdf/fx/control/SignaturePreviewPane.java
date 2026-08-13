package net.sf.jsignpdf.fx.control;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import javafx.beans.value.ObservableValue;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import net.sf.jsignpdf.utils.FontUtils;
import net.sf.jsignpdf.utils.FontUtils.L2Font;
import org.openpdf.text.pdf.BaseFont;

/**
 * Purely visual live preview of the visible signature contents.
 *
 * <p>This pane never participates in mouse picking. Placement, moving and
 * resizing remain entirely owned by {@link SignatureOverlay}.</p>
 */
final class SignaturePreviewPane extends Pane {
    private static final double DEFAULT_FONT_SIZE = 10.0;
    private static final Pattern FORMATTED_TIMESTAMP_PATTERN =
            Pattern.compile("\\$\\{timestamp:([^}]+)}");
    private static final Pattern COORD_PATTERN = Pattern.compile(
            "\\((-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\)\\s*[—–-]\\s*\\((-?\\d+(?:\\.\\d+)?)\\s*,\\s*(-?\\d+(?:\\.\\d+)?)\\)");

    private final ImageView imageView = new ImageView();
    private final Pane textPane = new Pane();
    private final Rectangle clip = new Rectangle();

    private Node l2TextControl;
    private Node fontSizeControl;
    private Node bgImagePathControl;
    private Node sigCoordsControl;
    private Node signerNameControl;
    private Node reasonControl;
    private Node locationControl;
    private Node contactControl;
    private Node keystoreTypeControl;
    private Node keystoreFileControl;
    private Node keystorePasswordControl;
    private Node keyAliasControl;
    private boolean listenersAttached;

    private String loadedImagePath = "";
    private Image loadedImage;
    private String cachedSignerKey = "";
    private String cachedCertificateSigner = "";

    // Same font source and metrics used by the OpenPDF signing engine.
    private byte[] l2FontData;
    private String l2FontName;
    private String l2FontEncoding;
    private String fxFontFaceName;
    private Font cachedFxFont;
    private double cachedFxFontPx = -1;
    private BaseFont openPdfBaseFont;

    SignaturePreviewPane() {
        setMouseTransparent(true);
        setPickOnBounds(false);
        setVisible(false);
        setClip(clip);

        imageView.setMouseTransparent(true);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        textPane.setMouseTransparent(true);

        getChildren().add(imageView);
        getChildren().add(textPane);

        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                bindControls();
                refresh();
            }
        });
    }

    void updateBounds(double x, double y, double width, double height) {
        if (width <= 0 || height <= 0) return;
        resizeRelocate(x, y, width, height);
        clip.setX(0);
        clip.setY(0);
        clip.setWidth(width);
        clip.setHeight(height);
        textPane.resizeRelocate(0, 0, width, height);
        layoutImage(width, height);
        renderText(width, height);
    }

    void refresh() {
        bindControls();
        refreshImage();
        renderText(getWidth(), getHeight());
    }

    private void layoutImage(double width, double height) {
        if (loadedImage == null || loadedImage.isError()
                || loadedImage.getWidth() <= 0 || loadedImage.getHeight() <= 0) {
            imageView.setFitWidth(0);
            imageView.setFitHeight(0);
            imageView.setX(0);
            imageView.setY(0);
            return;
        }
        double scale = Math.min(width / loadedImage.getWidth(), height / loadedImage.getHeight());
        double imageWidth = loadedImage.getWidth() * scale;
        double imageHeight = loadedImage.getHeight() * scale;
        imageView.setFitWidth(imageWidth);
        imageView.setFitHeight(imageHeight);
        imageView.setX((width - imageWidth) / 2.0);
        imageView.setY((height - imageHeight) / 2.0);
    }

    private void renderText(double width, double height) {
        textPane.getChildren().clear();
        if (width <= 0 || height <= 0) return;

        String rawText = readText(l2TextControl);
        String text = rawText == null || rawText.isEmpty()
                ? buildAutomaticText()
                : expandPlaceholders(rawText);
        if (text == null || text.isEmpty()) return;

        // Reproduce the OpenPDF Layer-2 text layout instead of approximating it
        // with the JavaFX system font. OpenPDF uses the configured L2 font in
        // PDF points, ColumnText leading equal to that font size, and the exact
        // BaseFont widths for wrapping.
        double fontPt = parsePositiveDouble(readText(fontSizeControl), DEFAULT_FONT_SIZE);
        double pointScale = getActualPointScale(width, height);
        if (!(pointScale > 0.0) || !Double.isFinite(pointScale)) pointScale = 1.0;
        double fontPx = Math.max(1.0, fontPt * pointScale);

        ensureExactFont(fontPx);
        Font fxFont = createFxFont(fontPx);
        double maxWidthPt = width / pointScale;
        List<String> lines = wrapForOpenPdf(text, maxWidthPt, fontPt);

        // PdfSignatureAppearance -> ColumnText.setSimpleColumn(..., leading=fontSize).
        // First baseline is one leading below the top, then advances by exactly
        // one leading for every row.
        double baseline = fontPx;
        for (String line : lines) {
            if (!line.isEmpty()) {
                Text node = new Text(line);
                node.setMouseTransparent(true);
                node.setTextOrigin(VPos.BASELINE);
                node.setFont(fxFont);
                node.setX(0);
                node.setY(baseline);
                node.setStyle("-fx-fill: #000000;");
                textPane.getChildren().add(node);
            }
            baseline += fontPx;
            if (baseline - fontPx > height + fontPx) break;
        }
    }

    private List<String> wrapForOpenPdf(String text, double maxWidthPt, double fontPt) {
        List<String> out = new ArrayList<>();
        String[] paragraphs = text.replace("\r\n", "\n").replace('\r', '\n').split("\\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                out.add("");
                continue;
            }
            if (openPdfBaseFont == null || maxWidthPt <= 1.0) {
                out.add(paragraph);
                continue;
            }
            String remaining = paragraph;
            while (!remaining.isEmpty()) {
                if (widthPoint(remaining, fontPt) <= maxWidthPt) {
                    out.add(remaining);
                    break;
                }
                int best = -1;
                int search = remaining.length();
                while (search > 0) {
                    int space = remaining.lastIndexOf(' ', search - 1);
                    if (space < 0) break;
                    String candidate = remaining.substring(0, space);
                    if (widthPoint(candidate, fontPt) <= maxWidthPt) {
                        best = space;
                        break;
                    }
                    search = space;
                }
                if (best > 0) {
                    out.add(remaining.substring(0, best));
                    remaining = remaining.substring(best + 1);
                    continue;
                }
                int cut = 1;
                while (cut < remaining.length()
                        && widthPoint(remaining.substring(0, cut + 1), fontPt) <= maxWidthPt) {
                    cut++;
                }
                out.add(remaining.substring(0, cut));
                remaining = remaining.substring(cut);
            }
        }
        return out;
    }

    private double widthPoint(String text, double fontPt) {
        try {
            return openPdfBaseFont.getWidthPoint(text, (float) fontPt);
        } catch (Exception ignored) {
            return text.length() * fontPt * 0.55;
        }
    }

    private double getActualPointScale(double width, double height) {
        String coords = readText(sigCoordsControl);
        if (coords != null && !coords.isEmpty()) {
            Matcher matcher = COORD_PATTERN.matcher(coords);
            if (matcher.find()) {
                try {
                    double x1 = Double.parseDouble(matcher.group(1));
                    double y1 = Double.parseDouble(matcher.group(2));
                    double x2 = Double.parseDouble(matcher.group(3));
                    double y2 = Double.parseDouble(matcher.group(4));
                    double pdfWidth = Math.abs(x2 - x1);
                    double pdfHeight = Math.abs(y2 - y1);
                    double sx = pdfWidth > 0.5 ? width / pdfWidth : Double.NaN;
                    double sy = pdfHeight > 0.5 ? height / pdfHeight : Double.NaN;
                    if (pdfWidth >= pdfHeight && Double.isFinite(sx) && sx > 0.0) return sx;
                    if (Double.isFinite(sy) && sy > 0.0) return sy;
                    if (Double.isFinite(sx) && sx > 0.0) return sx;
                } catch (Exception ignored) {
                    // Use zoom fallback below.
                }
            }
        }
        Scene scene = getScene();
        Node zoom = scene == null ? null : scene.lookup("#cmbZoom");
        String z = readValue(zoom);
        if (z != null && z.endsWith("%")) {
            try {
                return Math.max(0.05, Double.parseDouble(z.substring(0, z.length() - 1).trim()) / 100.0);
            } catch (Exception ignored) {
                // Safe fallback below.
            }
        }
        return 1.0;
    }

    private void ensureExactFont(double fontPx) {
        if (l2FontData != null && fxFontFaceName != null && openPdfBaseFont != null) return;
        try {
            L2Font l2 = FontUtils.getL2Font();
            if (l2 != null) {
                l2FontData = l2.getData();
                l2FontName = l2.getName();
                l2FontEncoding = l2.getEncoding();
                Font loaded = Font.loadFont(new ByteArrayInputStream(l2FontData), Math.max(1.0, fontPx));
                if (loaded != null) {
                    fxFontFaceName = loaded.getName();
                    cachedFxFont = loaded;
                    cachedFxFontPx = Math.max(1.0, fontPx);
                }
                openPdfBaseFont = BaseFont.createFont(l2FontName, l2FontEncoding,
                        BaseFont.EMBEDDED, BaseFont.CACHED, l2FontData, null);
            }
        } catch (Exception ignored) {
            // Fall back to the same built-in face OpenPDF uses.
        }
        if (openPdfBaseFont == null) {
            try {
                openPdfBaseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception ignored) {
                // Width fallback remains available.
            }
        }
        if (fxFontFaceName == null) fxFontFaceName = "Arial";
    }

    private Font createFxFont(double fontPx) {
        try {
            if (l2FontData != null && (cachedFxFont == null || Math.abs(cachedFxFontPx - fontPx) > 0.02)) {
                Font loaded = Font.loadFont(new ByteArrayInputStream(l2FontData), fontPx);
                if (loaded != null) {
                    cachedFxFont = loaded;
                    cachedFxFontPx = fontPx;
                    fxFontFaceName = loaded.getName();
                }
            }
            if (cachedFxFont != null) return cachedFxFont;
            return Font.font(fxFontFaceName, fontPx);
        } catch (Exception ignored) {
            return Font.font(fontPx);
        }
    }

    private void refreshImage() {
        String path = safeTrim(readText(bgImagePathControl));
        if (path.equals(loadedImagePath)) return;
        loadedImagePath = path;
        loadedImage = null;
        if (!path.isEmpty()) {
            try {
                File file = new File(path);
                if (file.isFile()) loadedImage = new Image(file.toURI().toString());
            } catch (Exception ignored) {
                loadedImage = null;
            }
        }
        imageView.setImage(loadedImage);
        layoutImage(getWidth(), getHeight());
    }

    private void bindControls() {
        if (listenersAttached) return;
        Scene scene = getScene();
        if (scene == null) return;

        l2TextControl = scene.lookup("#txtL2Text");
        fontSizeControl = scene.lookup("#txtFontSize");
        bgImagePathControl = scene.lookup("#txtBgImgPath");
        sigCoordsControl = scene.lookup("#lblSigCoords");
        signerNameControl = scene.lookup("#txtSignerName");
        reasonControl = scene.lookup("#txtReason");
        locationControl = scene.lookup("#txtLocation");
        contactControl = scene.lookup("#txtContact");
        keystoreTypeControl = scene.lookup("#cmbKeystoreType");
        keystoreFileControl = scene.lookup("#txtKeystoreFile");
        keystorePasswordControl = scene.lookup("#txtKeystorePassword");
        keyAliasControl = scene.lookup("#cmbKeyAlias");

        // The three appearance controls must exist before we consider binding complete.
        if (l2TextControl == null || fontSizeControl == null || bgImagePathControl == null) return;

        attachListener(l2TextControl, "textProperty");
        attachListener(fontSizeControl, "textProperty");
        attachListener(bgImagePathControl, "textProperty");
        attachListener(sigCoordsControl, "textProperty");
        attachListener(signerNameControl, "textProperty");
        attachListener(reasonControl, "textProperty");
        attachListener(locationControl, "textProperty");
        attachListener(contactControl, "textProperty");
        attachListener(keystoreTypeControl, "valueProperty");
        attachListener(keystoreFileControl, "textProperty");
        attachListener(keystorePasswordControl, "textProperty");
        attachListener(keyAliasControl, "valueProperty");
        listenersAttached = true;
    }

    private void attachListener(Node node, String propertyMethod) {
        if (node == null) return;
        try {
            Method method = node.getClass().getMethod(propertyMethod);
            Object property = method.invoke(node);
            if (property instanceof ObservableValue) {
                @SuppressWarnings("unchecked")
                ObservableValue<Object> observable = (ObservableValue<Object>) property;
                observable.addListener((obs, oldValue, newValue) -> refresh());
            }
        } catch (Exception ignored) {
            // A missing optional control must never affect signature placement.
        }
    }

    private String expandPlaceholders(String text) {
        String signer = safeTrim(readText(signerNameControl));
        if (signer.isEmpty()) signer = resolveCertificateSigner();
        String reason = safeTrim(readText(reasonControl));
        String location = safeTrim(readText(locationControl));
        String contact = safeTrim(readText(contactControl));
        Date now = new Date();
        String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z").format(now);
        String expanded = expandFormattedTimestamps(text, now);
        return expanded.replace("${timestamp}", timestamp)
                .replace("${signer}", signer)
                .replace("${reason}", reason)
                .replace("${location}", location)
                .replace("${contact}", contact);
    }

    private String expandFormattedTimestamps(String text, Date date) {
        Matcher matcher = FORMATTED_TIMESTAMP_PATTERN.matcher(text);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(0);
            try {
                replacement = new SimpleDateFormat(matcher.group(1)).format(date);
            } catch (IllegalArgumentException ignored) {
                // Keep invalid patterns visible.
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private String buildAutomaticText() {
        String signer = safeTrim(readText(signerNameControl));
        if (signer.isEmpty()) signer = resolveCertificateSigner();
        if (signer.isEmpty()) signer = "...";
        String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z").format(new Date());
        StringBuilder out = new StringBuilder();
        out.append(resource("default.l2text.signedBy", "Digitally signed by:")).append(' ').append(signer).append('\n');
        out.append(resource("default.l2text.date", "Date:")).append(' ').append(timestamp);
        String reason = safeTrim(readText(reasonControl));
        if (!reason.isEmpty()) out.append('\n').append(resource("default.l2text.reason", "Reason:")).append(' ').append(reason);
        String location = safeTrim(readText(locationControl));
        if (!location.isEmpty()) out.append('\n').append(resource("default.l2text.location", "Location:")).append(' ').append(location);
        return out.toString();
    }

    private String resource(String key, String fallback) {
        try {
            Class<?> constants = Class.forName("net.sf.jsignpdf.Constants");
            Object resources = constants.getField("RES").get(null);
            Object value = resources.getClass().getMethod("get", String.class).invoke(resources, key);
            String text = value == null ? "" : value.toString();
            return text.isEmpty() ? fallback : text;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String resolveCertificateSigner() {
        String type = safeTrim(readValue(keystoreTypeControl));
        String path = safeTrim(readText(keystoreFileControl));
        String password = readText(keystorePasswordControl);
        String alias = safeTrim(readValue(keyAliasControl));
        String key = type + "\n" + path + "\n" + password + "\n" + alias;
        if (key.equals(cachedSignerKey)) return cachedCertificateSigner;
        cachedSignerKey = key;
        cachedCertificateSigner = "";
        try {
            if (type.isEmpty()) type = "PKCS12";
            KeyStore keyStore = KeyStore.getInstance(type);
            if (path.isEmpty()) {
                keyStore.load(null, null);
            } else {
                try (FileInputStream input = new FileInputStream(path)) {
                    keyStore.load(input, password == null ? null : password.toCharArray());
                }
            }
            if (alias.isEmpty() || !keyStore.containsAlias(alias)) {
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String candidate = aliases.nextElement();
                    if (keyStore.isKeyEntry(candidate) || keyStore.getCertificate(candidate) != null) {
                        alias = candidate;
                        break;
                    }
                }
            }
            Certificate certificate = alias.isEmpty() ? null : keyStore.getCertificate(alias);
            if (certificate instanceof X509Certificate x509) {
                LdapName name = new LdapName(x509.getSubjectX500Principal().getName());
                for (Rdn rdn : name.getRdns()) {
                    if ("CN".equalsIgnoreCase(rdn.getType())) {
                        cachedCertificateSigner = String.valueOf(rdn.getValue());
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
            cachedCertificateSigner = "";
        }
        return cachedCertificateSigner;
    }

    private static String readText(Node node) {
        if (node == null) return "";
        try {
            Object value = node.getClass().getMethod("getText").invoke(node);
            return value == null ? "" : value.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String readValue(Node node) {
        if (node == null) return "";
        try {
            Object value = node.getClass().getMethod("getValue").invoke(node);
            return value == null ? "" : value.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static double parsePositiveDouble(String value, double fallback) {
        if (value == null) return fallback;
        try {
            double parsed = Double.parseDouble(value.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}

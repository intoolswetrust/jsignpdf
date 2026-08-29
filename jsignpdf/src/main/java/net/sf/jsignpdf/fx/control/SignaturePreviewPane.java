package net.sf.jsignpdf.fx.control;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.geometry.VPos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.fx.viewmodel.SigningOptionsViewModel;
import net.sf.jsignpdf.utils.FontUtils;
import net.sf.jsignpdf.utils.FontUtils.L2Font;
import net.sf.jsignpdf.utils.TextTimestampSubstitutor;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPKCS7;
import org.openpdf.text.pdf.PdfSignatureAppearance;
import org.openpdf.text.pdf.PdfWriter;

/**
 * Purely visual live preview of the visible signature contents.
 *
 * <p>All inputs come from {@link SigningOptionsViewModel} observables - the same
 * source the signing engines read - so the preview never scrapes the UI via
 * {@code scene.lookup}, reflection, or the localized coordinates label, and it
 * repaints only when a value it depends on actually changes.</p>
 *
 * <p>This pane never participates in mouse picking. Placement, moving and
 * resizing remain entirely owned by {@link SignatureOverlay}. The reproduced
 * text layout mirrors the OpenPDF description-only appearance
 * ({@code OpenPdfSigningEngine.configureDescriptionLayer2}); the DSS drawer lays
 * text out slightly differently, which the preview does not reproduce.</p>
 */
final class SignaturePreviewPane extends Pane {
    private static final double DEFAULT_FONT_SIZE = 10.0;

    private final SigningOptionsViewModel signingVM;

    private final ImageView imageView = new ImageView();
    private final Pane textPane = new Pane();
    private final Rectangle clip = new Rectangle();

    private double overridePointWidth;
    private double overridePointHeight;

    private String loadedImagePath = "";
    private Image loadedImage;
    private String cachedSignerKey = "";
    private CertificateInfo cachedCertificate = CertificateInfo.EMPTY;

    // Signer-name resolution is a preview convenience only. It runs off the FX
    // thread, debounced, and never touches a hardware keystore (see
    // resolveCertificateSigner), so a half-typed PIN can never lock a token.
    private final PauseTransition signerDebounce = new PauseTransition(Duration.millis(400));
    private String pendingSignerKey;
    private String pendingType = "";
    private File pendingFile;
    private String pendingPassword;
    private String pendingAlias = "";

    // Same font source and metrics used by the OpenPDF signing engine.
    private byte[] l2FontData;
    private String l2FontName;
    private String l2FontEncoding;
    private String fxFontFullName;
    private BaseFont openPdfBaseFont;

    SignaturePreviewPane(SigningOptionsViewModel signingVM) {
        this.signingVM = signingVM;
        setMouseTransparent(true);
        setPickOnBounds(false);
        setVisible(false);
        setClip(clip);

        imageView.setMouseTransparent(true);
        imageView.setSmooth(true);
        textPane.setMouseTransparent(true);

        signerDebounce.setOnFinished(e -> startSignerResolve());

        getChildren().add(imageView);
        getChildren().add(textPane);

        bindToViewModel();
    }

    private void bindToViewModel() {
        InvalidationListener rerender = o -> refresh();
        signingVM.l2TextProperty().addListener(rerender);
        signingVM.l2TextFontSizeProperty().addListener(rerender);
        signingVM.bgImgPathProperty().addListener(rerender);
        signingVM.bgImgScaleProperty().addListener(rerender);
        signingVM.signerNameProperty().addListener(rerender);
        signingVM.reasonProperty().addListener(rerender);
        signingVM.locationProperty().addListener(rerender);
        signingVM.contactProperty().addListener(rerender);
        signingVM.ksTypeProperty().addListener(rerender);
        signingVM.ksFileProperty().addListener(rerender);
        signingVM.ksPasswordProperty().addListener(rerender);
        signingVM.keyAliasProperty().addListener(rerender);
        signingVM.positionLLXProperty().addListener(rerender);
        signingVM.positionLLYProperty().addListener(rerender);
        signingVM.positionURXProperty().addListener(rerender);
        signingVM.positionURYProperty().addListener(rerender);
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

    /**
     * Overrides the PDF-point size the preview scales against, for a rectangle the signing view model does not
     * hold - the {@code /Rect} of a selected existing signature field. Zero restores the view-model coordinates.
     */
    void setPointSizeOverride(double widthPt, double heightPt) {
        overridePointWidth = widthPt;
        overridePointHeight = heightPt;
    }

    void refresh() {
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
        // Mirror OpenPdfSigningEngine.configureDescriptionLayer2 background scaling:
        //   scale == 0  -> stretch to fill the whole rectangle
        //   scale <  0  -> best-fit, preserving the aspect ratio, centered
        //   scale >  0  -> multiplier on the image's natural size, centered
        double scale = signingVM.bgImgScaleProperty().get();
        if (scale == 0) {
            imageView.setPreserveRatio(false);
            imageView.setFitWidth(width);
            imageView.setFitHeight(height);
            imageView.setX(0);
            imageView.setY(0);
            return;
        }
        imageView.setPreserveRatio(true);
        double factor;
        if (scale < 0) {
            factor = Math.min(width / loadedImage.getWidth(), height / loadedImage.getHeight());
        } else {
            double pointScale = getActualPointScale(width, height);
            if (!(pointScale > 0.0) || !Double.isFinite(pointScale)) pointScale = 1.0;
            factor = scale * pointScale;
        }
        double imageWidth = loadedImage.getWidth() * factor;
        double imageHeight = loadedImage.getHeight() * factor;
        imageView.setFitWidth(imageWidth);
        imageView.setFitHeight(imageHeight);
        imageView.setX((width - imageWidth) / 2.0);
        imageView.setY((height - imageHeight) / 2.0);
    }

    private void renderText(double width, double height) {
        textPane.getChildren().clear();
        if (width <= 0 || height <= 0) return;

        String rawText = signingVM.l2TextProperty().get();
        String text = rawText == null || rawText.isEmpty()
                ? buildAutomaticText()
                : expandPlaceholders(rawText);
        if (text == null || text.isEmpty()) return;

        // Reproduce the OpenPDF Layer-2 text layout instead of approximating it
        // with the JavaFX system font. OpenPDF uses the configured L2 font in
        // PDF points, ColumnText leading equal to that font size, and the exact
        // BaseFont widths for wrapping.
        double pointScale = getActualPointScale(width, height);
        if (!(pointScale > 0.0) || !Double.isFinite(pointScale)) pointScale = 1.0;

        ensureExactFont();
        double maxWidthPt = width / pointScale;

        // A configured size of 0 means auto-fit; reproduce OpenPDF's fitText over
        // the same point-sized rectangle so the preview matches the output size.
        double fontPt = signingVM.l2TextFontSizeProperty().get();
        if (fontPt <= 0) {
            fontPt = autoFitFontPt(text, maxWidthPt, height / pointScale);
        }
        double fontPx = Math.max(1.0, fontPt * pointScale);

        Font fxFont = createFxFont(fontPx);
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

    private double autoFitFontPt(String text, double rectWidthPt, double rectHeightPt) {
        try {
            org.openpdf.text.Font font = openPdfBaseFont != null
                    ? new org.openpdf.text.Font(openPdfBaseFont)
                    : new org.openpdf.text.Font();
            org.openpdf.text.Rectangle fitRect =
                    new org.openpdf.text.Rectangle((float) rectWidthPt, (float) rectHeightPt);
            float size = PdfSignatureAppearance.fitText(font, text, fitRect, 12f,
                    PdfWriter.RUN_DIRECTION_NO_BIDI);
            return size > 0 ? size : DEFAULT_FONT_SIZE;
        } catch (Exception ignored) {
            return DEFAULT_FONT_SIZE;
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

    /**
     * Points-to-pixels scale of the preview, derived from the signature rectangle
     * held on the signing view model (PDF points) against this pane's pixel size.
     * Both are kept live by the placement listeners in {@code MainWindowController}.
     */
    private double getActualPointScale(double width, double height) {
        double pdfWidth = overridePointWidth > 0.5 ? overridePointWidth
                : signingVM.positionURXProperty().get() - signingVM.positionLLXProperty().get();
        double pdfHeight = overridePointHeight > 0.5 ? overridePointHeight
                : signingVM.positionURYProperty().get() - signingVM.positionLLYProperty().get();
        double sx = pdfWidth > 0.5 ? width / pdfWidth : Double.NaN;
        double sy = pdfHeight > 0.5 ? height / pdfHeight : Double.NaN;
        if (pdfWidth >= pdfHeight && Double.isFinite(sx) && sx > 0.0) return sx;
        if (Double.isFinite(sy) && sy > 0.0) return sy;
        if (Double.isFinite(sx) && sx > 0.0) return sx;
        return 1.0;
    }

    /**
     * Registers the L2 font once, because {@link Font#loadFont(java.io.InputStream, double)} re-parses the
     * face on every call and repaints resize the text continuously while the rectangle is dragged.
     */
    private void ensureExactFont() {
        if (l2FontData != null && fxFontFullName != null && openPdfBaseFont != null) return;
        try {
            L2Font l2 = FontUtils.getL2Font();
            if (l2 != null) {
                l2FontData = l2.getData();
                l2FontName = l2.getName();
                l2FontEncoding = l2.getEncoding();
                Font loaded = Font.loadFont(new ByteArrayInputStream(l2FontData), DEFAULT_FONT_SIZE);
                if (loaded != null) {
                    fxFontFullName = loaded.getName();
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
        if (fxFontFullName == null) fxFontFullName = "Arial";
    }

    /**
     * Returns the registered L2 face at the given size. The full font name is used because a family-only
     * lookup would drop the weight and posture of the loaded face.
     */
    private Font createFxFont(double fontPx) {
        try {
            return new Font(fxFontFullName, fontPx);
        } catch (Exception ignored) {
            return Font.font(fontPx);
        }
    }

    private void refreshImage() {
        String path = safeTrim(signingVM.bgImgPathProperty().get());
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

    /**
     * Expands the placeholders through the same {@link TextTimestampSubstitutor} the engines use, so
     * escaping, default values and timestamp patterns render identically here and in the signed PDF.
     */
    private String expandPlaceholders(String text) {
        CertificateInfo certificate = resolveCertificate();
        String signer = safeTrim(signingVM.signerNameProperty().get());
        if (signer.isEmpty()) signer = certificate.commonName();

        Date now = new Date();
        Map<String, String> replacements = new HashMap<>();
        replacements.put(Constants.L2TEXT_PLACEHOLDER_SIGNER, signer);
        replacements.put(Constants.L2TEXT_PLACEHOLDER_CERTIFICATE, certificate.subjectFields());
        replacements.put(Constants.L2TEXT_PLACEHOLDER_TIMESTAMP,
                new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z").format(now));
        replacements.put(Constants.L2TEXT_PLACEHOLDER_LOCATION, safeTrim(signingVM.locationProperty().get()));
        replacements.put(Constants.L2TEXT_PLACEHOLDER_REASON, safeTrim(signingVM.reasonProperty().get()));
        replacements.put(Constants.L2TEXT_PLACEHOLDER_CONTACT, safeTrim(signingVM.contactProperty().get()));
        return TextTimestampSubstitutor.replace(text, replacements, now);
    }

    private String buildAutomaticText() {
        String signer = safeTrim(signingVM.signerNameProperty().get());
        if (signer.isEmpty()) signer = resolveCertificate().commonName();
        if (signer.isEmpty()) signer = "...";
        String timestamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z").format(new Date());
        StringBuilder out = new StringBuilder();
        out.append(resource("default.l2text.signedBy", "Digitally signed by:")).append(' ').append(signer).append('\n');
        out.append(resource("default.l2text.date", "Date:")).append(' ').append(timestamp);
        String reason = safeTrim(signingVM.reasonProperty().get());
        if (!reason.isEmpty()) out.append('\n').append(resource("default.l2text.reason", "Reason:")).append(' ').append(reason);
        String location = safeTrim(signingVM.locationProperty().get());
        if (!location.isEmpty()) out.append('\n').append(resource("default.l2text.location", "Location:")).append(' ').append(location);
        return out.toString();
    }

    private String resource(String key, String fallback) {
        try {
            String text = Constants.RES.get(key);
            return text == null || text.isEmpty() ? fallback : text;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * Returns the last resolved certificate details, scheduling an off-thread reload when
     * the file-based keystore inputs change. Never blocks the FX thread and
     * never probes a hardware or system keystore, whose token could be locked by
     * repeated loads with a partially typed PIN.
     */
    private CertificateInfo resolveCertificate() {
        String type = safeTrim(signingVM.ksTypeProperty().get());
        String path = safeTrim(signingVM.ksFileProperty().get());
        String password = signingVM.ksPasswordProperty().get();
        String alias = safeTrim(signingVM.keyAliasProperty().get());

        File file = path.isEmpty() ? null : new File(path);
        if (file == null || !file.isFile() || isHardwareKeystore(type)) {
            return cachedCertificate;
        }

        String key = type + "\n" + path + "\n" + hash(password) + "\n" + alias;
        if (key.equals(cachedSignerKey) || key.equals(pendingSignerKey)) {
            return cachedCertificate;
        }

        pendingSignerKey = key;
        pendingType = type;
        pendingFile = file;
        pendingPassword = password;
        pendingAlias = alias;
        signerDebounce.playFromStart();
        return cachedCertificate;
    }

    private void startSignerResolve() {
        final String key = pendingSignerKey;
        final String type = pendingType;
        final File file = pendingFile;
        final String password = pendingPassword;
        final String alias = pendingAlias;
        pendingPassword = null;
        Thread worker = new Thread(() -> {
            CertificateInfo certificate = loadCertificate(type, file, password, alias);
            Platform.runLater(() -> {
                cachedSignerKey = key;
                cachedCertificate = certificate;
                if (key.equals(pendingSignerKey)) pendingSignerKey = null;
                renderText(getWidth(), getHeight());
            });
        }, "sig-preview-signer");
        worker.setDaemon(true);
        worker.start();
    }

    private static CertificateInfo loadCertificate(String type, File file, String password, String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance(type.isEmpty() ? "PKCS12" : type);
            try (FileInputStream input = new FileInputStream(file)) {
                keyStore.load(input, password == null ? null : password.toCharArray());
            }
            String selected = alias;
            if (selected.isEmpty() || !keyStore.containsAlias(selected)) {
                Enumeration<String> aliases = keyStore.aliases();
                while (aliases.hasMoreElements()) {
                    String candidate = aliases.nextElement();
                    if (keyStore.isKeyEntry(candidate) || keyStore.getCertificate(candidate) != null) {
                        selected = candidate;
                        break;
                    }
                }
            }
            Certificate certificate = selected.isEmpty() ? null : keyStore.getCertificate(selected);
            if (certificate instanceof X509Certificate x509) {
                return new CertificateInfo(extractCommonName(x509),
                        PdfPKCS7.getSubjectFields(x509).toString());
            }
        } catch (Exception ignored) {
            // Preview convenience only; a failed load simply shows no signer name.
        }
        return CertificateInfo.EMPTY;
    }

    private static String extractCommonName(X509Certificate certificate) {
        try {
            LdapName name = new LdapName(certificate.getSubjectX500Principal().getName());
            for (Rdn rdn : name.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return String.valueOf(rdn.getValue());
                }
            }
        } catch (Exception ignored) {
            // No usable CN; the preview falls back to an empty signer name.
        }
        return "";
    }

    /**
     * Signer CN and the {@code ${certificate}} description of the preview's signing certificate.
     */
    private record CertificateInfo(String commonName, String subjectFields) {
        static final CertificateInfo EMPTY = new CertificateInfo("", "");
    }

    private static boolean isHardwareKeystore(String type) {
        String t = type.toUpperCase(Locale.ROOT);
        return t.contains("PKCS11") || t.contains("WINDOWS") || t.contains("KEYCHAIN");
    }

    private static String hash(String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}

package net.sf.jsignpdf.signing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.SignerLogic;
import net.sf.jsignpdf.TestPdfFields;
import net.sf.jsignpdf.TestPdfFields.FieldSpec;
import net.sf.jsignpdf.signing.validation.PdfSignatureValidator.ValidationResult;
import net.sf.jsignpdf.types.PDFEncryption;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.junit.Test;

/**
 * Signs into pre-placed empty signature fields (issue #223) with both bundled engines. The field's own
 * rectangle and page decide where the signature goes, so the assertions are about the output landing in the
 * right field, on the right page, at the field's coordinates - and about the other fields staying empty.
 */
public class ExistingFieldSigningTest extends SigningTestBase {

    private static final String OPENPDF = "openpdf";
    private static final String DSS = "dss";

    @Test
    public void openPdfSignsIntoNamedField() throws Exception {
        signsIntoNamedField(OPENPDF);
    }

    @Test
    public void dssSignsIntoNamedField() throws Exception {
        signsIntoNamedField(DSS);
    }

    /** The field sits on page 3 while the position options point at page 1 - the field wins. */
    @Test
    public void openPdfSignsIntoFieldOnAnotherPage() throws Exception {
        signsIntoFieldOnAnotherPage(OPENPDF);
    }

    @Test
    public void dssSignsIntoFieldOnAnotherPage() throws Exception {
        signsIntoFieldOnAnotherPage(DSS);
    }

    /**
     * DSS rejects a signature box that overlaps an existing annotation. Filling a pre-placed field must not go
     * through that check against the wrong page - a field on page 3 whose rectangle happens to match an
     * annotation on page 1 is perfectly legal.
     */
    @Test
    public void dssSignsIntoFieldWhoseRectMatchesAnAnnotationOnPageOne() throws Exception {
        BasicSignerOptions options = optionsFor(DSS, 3, FieldSpec.blank("OnPageOne", 1, 70, 500, 300, 560),
                FieldSpec.blank("OnPageThree", 3, 70, 500, 300, 560));
        options.setSigFieldName("OnPageThree");

        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals(3, result.signaturePage);
        assertEquals(List.of("OnPageThree"), signedFieldNames(new File(options.getOutFileX())));
    }

    /**
     * A long signature text has to fit the box the form author drew. There are no configured coordinates to
     * take that box from, so its size is read off the existing widget and drives the same text-wrapping mode
     * the coordinate path uses.
     */
    @Test
    public void dssFitsLongTextIntoASmallFieldBox() throws Exception {
        BasicSignerOptions options = optionsFor(DSS, 1, FieldSpec.blank("Tiny", 1, 70, 700, 170, 730));
        options.setSigFieldName("Tiny");
        options.setL2Text("Signed by a signer with a rather long distinguished name, for a rather long reason, "
                + "at a rather long location, on a rather precise timestamp");

        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertNotNull("The text must be rendered into the field box", result.appearanceText);
    }

    /** An author who drew a signature box wants something drawn in it, with or without -V. */
    @Test
    public void openPdfDrawsAppearanceWithoutVisibleFlag() throws Exception {
        drawsAppearanceWithoutVisibleFlag(OPENPDF);
    }

    @Test
    public void dssDrawsAppearanceWithoutVisibleFlag() throws Exception {
        drawsAppearanceWithoutVisibleFlag(DSS);
    }

    /** A zero-size field rectangle stays an invisible signature - that is the field's decision. */
    @Test
    public void zeroSizeFieldRectProducesInvisibleSignature() throws Exception {
        BasicSignerOptions options = optionsFor(OPENPDF, 1, FieldSpec.zeroSize("Invisible", 1));
        options.setSigFieldName("Invisible");

        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertFalse("A zero-size field rect means an invisible signature", result.hasVisibleRect);
    }

    /** {@code auto} takes the first blank field in document order, skipping the ones already signed. */
    @Test
    public void autoSelectorSkipsSignedFields() throws Exception {
        BasicSignerOptions options = optionsFor(OPENPDF, 1, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760),
                FieldSpec.blank("Signature2", 1, 70, 600, 300, 660));
        options.setSigFieldName("auto");
        assertTrue(new SignerLogic(options).signFile());
        assertEquals("Signature1", signedFieldNames(new File(options.getOutFileX())).get(0));

        // Sign the result again, still with "auto": the first field is taken now, so the second one is next.
        BasicSignerOptions second = options.createCopy();
        second.setInFile(options.getOutFileX());
        second.setOutFile(new File(tempFolder.getRoot(), "output2.pdf").getAbsolutePath());
        second.setSigFieldName("auto");
        assertTrue(new SignerLogic(second).signFile());

        List<String> signed = signedFieldNames(new File(second.getOutFileX()));
        assertEquals("both fields are filled now", List.of("Signature1", "Signature2"), signed);
    }

    /**
     * A selector is re-resolved for every file, so the same {@code --sig-field} value can be used for a batch
     * of documents built from the same template.
     */
    @Test
    public void selectorIsReResolvedPerInputFile() throws Exception {
        BasicSignerOptions first = optionsFor(OPENPDF, 1, FieldSpec.blank("Alpha", 1, 70, 700, 300, 760),
                FieldSpec.blank("Beta", 1, 70, 600, 300, 660));
        first.setSigFieldName("#2");
        assertTrue(new SignerLogic(first).signFile());
        assertEquals(List.of("Beta"), signedFieldNames(new File(first.getOutFileX())));

        // Same options object, next input file - exactly what Signer.signFiles() does for a batch.
        File other = TestPdfFields.create(new File(tempFolder.getRoot(), "other.pdf"), 1,
                List.of(FieldSpec.blank("Gamma", 1, 70, 700, 300, 760),
                        FieldSpec.blank("Delta", 1, 70, 600, 300, 660)));
        first.setInFile(other.getAbsolutePath());
        first.setOutFile(new File(tempFolder.getRoot(), "other-signed.pdf").getAbsolutePath());
        assertTrue(new SignerLogic(first).signFile());
        assertEquals("#2 means the second field of *this* document", List.of("Delta"),
                signedFieldNames(new File(first.getOutFileX())));
    }

    /** A typo must not silently create a stray new field. */
    @Test
    public void unknownFieldNameFailsWithoutWritingOutput() throws Exception {
        BasicSignerOptions options = optionsFor(OPENPDF, 1, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760));
        options.setSigFieldName("Signatur1");

        assertFalse("Signing should fail", new SignerLogic(options).signFile());
        assertFalse("No output should be written", new File(options.getOutFileX()).exists());
    }

    /**
     * A non-incremental rewrite contradicts signing into a pre-placed field (and drops the signatures already
     * in the document), so the combination is refused - before any keystore access, which the deliberately
     * broken keystore path proves.
     */
    @Test
    public void overwriteModeIsRefusedBeforeKeystoreAccess() throws Exception {
        BasicSignerOptions options = optionsFor(OPENPDF, 1, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760));
        options.setSigFieldName("Signature1");
        options.setAppend(false);
        options.setKsFile("/no/such/keystore.p12");

        List<String> messages = captureSevere(() -> assertFalse(new SignerLogic(options).signFile()));

        assertTrue("The refusal must name --overwrite: " + messages,
                messages.stream().anyMatch(m -> m.contains("--overwrite")));
    }

    /** Encryption also switches the document to a non-incremental rewrite, and the message must say so. */
    @Test
    public void encryptionIsRefusedAndNamed() throws Exception {
        BasicSignerOptions options = optionsFor(OPENPDF, 1, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760));
        options.setSigFieldName("Signature1");
        options.setPdfEncryption(PDFEncryption.PASSWORD);
        options.setPdfOwnerPwd("owner".toCharArray());
        options.setPdfUserPwd("user".toCharArray());
        options.setKsFile("/no/such/keystore.p12");

        List<String> messages = captureSevere(() -> assertFalse(new SignerLogic(options).signFile()));

        assertTrue("The refusal must name encryption, not --overwrite: " + messages,
                messages.stream().anyMatch(m -> m.contains("encryption")));
    }

    /** The position options are ignored (with a warning) - the field's rectangle decides. */
    @Test
    public void positionOptionsAreIgnoredWithWarning() throws Exception {
        BasicSignerOptions options = optionsFor(OPENPDF, 1, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760));
        options.setSigFieldName("Signature1");
        options.setVisible(true);
        options.setPage(1);
        options.setPositionLLX(10);
        options.setPositionLLY(20);
        options.setPositionURX(120);
        options.setPositionURY(70);

        List<String> warnings = new ArrayList<>();
        ValidationResult result;
        Handler handler = capturing(Level.WARNING, warnings);
        Constants.LOGGER.addHandler(handler);
        try {
            result = signAndValidate(options);
        } finally {
            Constants.LOGGER.removeHandler(handler);
        }

        assertTrue("The user should be told the position is ignored: " + warnings,
                warnings.stream().anyMatch(m -> m.contains("Signature1")));
        assertEquals("The field rectangle wins", 70f, result.rectLLX, 1f);
        assertEquals(700f, result.rectLLY, 1f);
    }

    private void signsIntoNamedField(String engine) throws Exception {
        BasicSignerOptions options = optionsFor(engine, 1, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760),
                FieldSpec.blank("Signature2", 1, 70, 600, 300, 660));
        options.setSigFieldName("Signature2");

        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("Only one field should be signed", 1, result.signatureCount);
        assertEquals(List.of("Signature2"), signedFieldNames(new File(options.getOutFileX())));
        assertTrue("The field has a rectangle, so the appearance is drawn", result.hasVisibleRect);
        assertEquals("The appearance lands at the field rectangle", 70f, result.rectLLX, 1f);
        assertEquals(600f, result.rectLLY, 1f);
        assertEquals(300f, result.rectURX, 1f);
        assertEquals(660f, result.rectURY, 1f);
    }

    private void signsIntoFieldOnAnotherPage(String engine) throws Exception {
        BasicSignerOptions options = optionsFor(engine, 3, FieldSpec.blank("OnPageOne", 1, 70, 700, 300, 760),
                FieldSpec.blank("OnPageThree", 3, 70, 500, 300, 560));
        options.setSigFieldName("OnPageThree");
        options.setPage(1);

        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertEquals("The signature must be on the field's page, not the configured one", 3, result.signaturePage);
        assertEquals(70f, result.rectLLX, 1f);
        assertEquals(500f, result.rectLLY, 1f);
        assertNotNull("The appearance must be drawn into the field on page 3", result.appearanceText);
        assertTrue("The appearance must not be empty", result.appearanceText.trim().length() > 0);
        assertEquals(List.of("OnPageThree"), signedFieldNames(new File(options.getOutFileX())));
    }

    private void drawsAppearanceWithoutVisibleFlag(String engine) throws Exception {
        BasicSignerOptions options = optionsFor(engine, 1, FieldSpec.blank("Signature1", 1, 70, 700, 300, 760));
        options.setSigFieldName("Signature1");
        options.setVisible(false);

        ValidationResult result = signAndValidate(options);

        assertTrue("Signature should be valid", result.signatureValid);
        assertTrue("The appearance is drawn into the field even without -V", result.hasVisibleRect);
        assertNotNull("An appearance stream should have been generated", result.appearanceText);
    }

    /** Builds signing options for a freshly created PDF with the given signature fields. */
    private BasicSignerOptions optionsFor(String engine, int pageCount, FieldSpec... fields) throws Exception {
        BasicSignerOptions options = createDefaultOptions();
        File inFile = TestPdfFields.create(new File(tempFolder.getRoot(), "fields.pdf"), pageCount, List.of(fields));
        options.setInFile(inFile.getAbsolutePath());
        options.setEngine(engine);
        return options;
    }

    /** Names of the fields that carry a signature in the given PDF, in AcroForm order. */
    private static List<String> signedFieldNames(File pdf) throws Exception {
        List<String> names = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            for (PDField field : doc.getDocumentCatalog().getAcroForm().getFields()) {
                if (field instanceof PDSignatureField sigField && sigField.getSignature() != null
                        && sigField.getSignature().getContents() != null) {
                    names.add(field.getFullyQualifiedName());
                }
            }
        }
        return names;
    }

    private static List<String> captureSevere(Runnable action) {
        List<String> messages = new ArrayList<>();
        Handler handler = capturing(Level.SEVERE, messages);
        Constants.LOGGER.addHandler(handler);
        try {
            action.run();
        } finally {
            Constants.LOGGER.removeHandler(handler);
        }
        return messages;
    }

    private static Handler capturing(Level level, List<String> sink) {
        return new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= level.intValue() && record.getMessage() != null) {
                    sink.add(record.getMessage());
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
    }
}

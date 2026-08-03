#!/usr/bin/env bash
#
# Manual smoke test for buffering.mode=temp (issue #178, design-doc/3.2-large-file-signing.md).
#
# Too slow and too disk-hungry for CI: it generates a multi-hundred-megabyte PDF and signs it four
# times. Run it by hand when touching the buffering code, on a machine with a few GB free.
#
# What it proves that the unit tests cannot: the unit tests use a few-KB document, far below the
# engines' 64 MB spill threshold, so they only show the wiring is in place. This one actually
# exceeds the heap, so `memory` mode is expected to FAIL and `temp` mode to SUCCEED.
#
# Usage:
#   mvn clean install -DskipTests            # needs distribution/target/appassembler
#   design-doc/3.2-large-file-signing-smoke.sh [size-in-MB] [heap]
#
# Defaults: a ~600 MB document under -Xmx512m.

set -u -o pipefail

SIZE_MB="${1:-600}"
HEAP="${2:-512m}"

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LIB="$ROOT/distribution/target/appassembler/lib"
WORK="${TMPDIR:-/tmp}/jsignpdf-large-smoke"
KEYSTORE="$ROOT/jsignpdf/src/test/resources/test-keystore.jks"

if [ ! -d "$LIB" ]; then
    echo "Missing $LIB - run 'mvn clean install -DskipTests' first." >&2
    exit 1
fi

mkdir -p "$WORK"
IN="$WORK/large.pdf"
STAGING="$WORK/staging"
mkdir -p "$STAGING"

CP="$LIB/*"

# ---------------------------------------------------------------- generate

if [ ! -s "$IN" ]; then
    echo "== generating a ~${SIZE_MB} MB PDF at $IN (this takes a while)"
    cat > "$WORK/Gen.java" <<'EOF'
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

/**
 * Pages of incompressible noise, so the file size is driven by real stream payload rather than by
 * something a filter can collapse. One save at the end; the document itself is staged on disk, so
 * generating a 1 GB input does not need a 1 GB heap either.
 */
public class Gen {

    private static final int SIDE = 1000;
    /** 1000x1000 RGB noise deflates to almost exactly its raw size. */
    private static final long BYTES_PER_PAGE = (long) SIDE * SIDE * 3L;

    public static void main(String[] a) throws Exception {
        long targetBytes = Long.parseLong(a[0]) * 1024L * 1024L;
        File out = new File(a[1]);
        int pages = (int) Math.max(1, (targetBytes + BYTES_PER_PAGE - 1) / BYTES_PER_PAGE);
        Random rnd = new Random(42);
        try (PDDocument doc = new PDDocument(IOUtils.createTempFileOnlyStreamCache())) {
            for (int p = 0; p < pages; p++) {
                BufferedImage img = new BufferedImage(SIDE, SIDE, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < SIDE; y++) {
                    for (int x = 0; x < SIDE; x++) {
                        img.setRGB(x, y, rnd.nextInt());
                    }
                }
                PDPage page = new PDPage();
                doc.addPage(page);
                PDImageXObject xo = LosslessFactory.createFromImage(doc, img);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    cs.drawImage(xo, 0, 0, 600, 600);
                }
                if ((p + 1) % 10 == 0) {
                    System.out.println("  page " + (p + 1) + "/" + pages + " ...");
                }
            }
            doc.save(out);
        }
    }
}
EOF
    javac -cp "$CP" -d "$WORK" "$WORK/Gen.java" || exit 1
    # Generation is not what we are measuring, but it should not need a huge heap either.
    java -Xmx1g -cp "$CP:$WORK" Gen "$SIZE_MB" "$IN" || exit 1
fi

echo "== input: $(du -h "$IN" | cut -f1)"

# ---------------------------------------------------------------- sign

run() {
    local engine="$1" mode="$2" expected="$3"
    # Output name is <out-directory>/<prefix><input basename><suffix>.pdf; only the suffix is set here.
    local out="$WORK/large_${engine}_${mode}.pdf"
    rm -f "$out"
    rm -rf "${STAGING:?}"/*

    echo
    echo "== engine=$engine buffering.mode=$mode -Xmx$HEAP (expecting: $expected)"
    local start=$SECONDS
    java "-Xmx$HEAP" -cp "$CP" net.sf.jsignpdf.Signer \
        -kst JKS -ksf "$KEYSTORE" -ksp keystorepass -ka rsa2048 -kp RSA2048pass \
        -eng "$engine" \
        -o "buffering.mode=$mode" -o "buffering.tempDir=$STAGING" \
        -d "$WORK" -os "_${engine}_${mode}" \
        "$IN" > "$WORK/log-$engine-$mode.txt" 2>&1
    local rc=$?
    local secs=$((SECONDS - start))

    local actual="FAIL"
    if [ $rc -eq 0 ] && [ -s "$out" ]; then
        actual="OK"
    fi

    local leftovers
    leftovers=$(find "$STAGING" -type f | wc -l)

    local why=""
    if [ "$actual" = "FAIL" ]; then
        if grep -qi "out of memory" "$WORK/log-$engine-$mode.txt"; then
            why=" (OutOfMemoryError)"
        else
            # A failure for any other reason is not evidence about buffering; say so loudly.
            why=" (NOT an OutOfMemoryError -- check the log)"
        fi
    fi

    if [ "$actual" = "$expected" ]; then
        echo "   -> $actual$why in ${secs}s (as expected), staging leftovers: $leftovers"
    else
        echo "   -> $actual$why in ${secs}s -- EXPECTED $expected; see $WORK/log-$engine-$mode.txt"
    fi
    if [ "$leftovers" -ne 0 ]; then
        echo "   -> LEAK: $leftovers file(s) left in $STAGING"
        find "$STAGING" -type f -exec ls -lh {} \;
    fi
}

# memory mode is expected to die with an OutOfMemoryError; that is the point of the test.
run openpdf memory FAIL
run openpdf temp   OK
run dss     memory FAIL
run dss     temp   OK

echo
echo "== done. Artefacts in $WORK (delete it to reclaim the space)."

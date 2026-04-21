/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is 'JSignPdf, a free application for PDF signing'.
 *
 * The Initial Developer of the Original Code is Josef Cacek.
 * Portions created by Josef Cacek are Copyright (C) Josef Cacek. All Rights Reserved.
 *
 * Contributor(s): Josef Cacek.
 *
 * Alternatively, the contents of this file may be used under the terms
 * of the GNU Lesser General Public License, version 2.1 (the  "LGPL License"), in which case the
 * provisions of LGPL License are applicable instead of those
 * above. If you wish to allow use of your version of this file only
 * under the terms of the LGPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the LGPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the LGPL License.
 */
package net.sf.jsignpdf;

import java.io.BufferedReader;
import java.io.Console;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Reads passwords from stdin (or the interactive console) for CLI password options that were
 * marked with the {@link Constants#STDIN_PWD_SENTINEL} sentinel.
 *
 * <p>See {@code design-doc/3.0.0-stdin-passwords.md}.
 */
final class StdinPasswordReader {

    private static final String PROGRESS_PREFIX = "[jsignpdf] ";

    private final BufferedReader reader;
    private final Console console;
    private final PrintStream progressOut;
    private final boolean quiet;

    StdinPasswordReader(BufferedReader reader, Console console, PrintStream progressOut, boolean quiet) {
        this.reader = reader;
        this.console = console;
        this.progressOut = progressOut;
        this.quiet = quiet;
    }

    /**
     * Default reader bound to {@link System#in}, {@link System#console()} and {@link System#err}.
     */
    static StdinPasswordReader systemDefault(boolean quiet) {
        return new StdinPasswordReader(new BufferedReader(new InputStreamReader(System.in)), System.console(),
                System.err, quiet);
    }

    /**
     * Reads the next password. Emits a progress line (unless quiet) before blocking on input.
     *
     * @param longOptionName the long option name (e.g. {@code keystore-password}), used in progress/error messages
     * @param index 1-based index of this read among the sentinel-marked options in the current invocation
     * @param total total count of sentinel-marked options in the current invocation
     * @return the password characters; never {@code null}
     * @throws IOException on EOF before a line is read, or on underlying I/O failure
     */
    char[] readNext(String longOptionName, int index, int total) throws IOException {
        if (!quiet && progressOut != null) {
            progressOut.println(PROGRESS_PREFIX + "Reading password for --" + longOptionName + " (" + index + "/" + total
                    + ") from " + (console != null ? "console" : "stdin") + "...");
        }
        if (console != null) {
            char[] pwd = console.readPassword("Enter password for --" + longOptionName + ": ");
            if (pwd == null) {
                throw new EOFException(
                        "Unexpected end of input while reading password for --" + longOptionName);
            }
            return pwd;
        }
        String line;
        try {
            line = reader.readLine();
        } catch (IOException e) {
            throw new IOException("Failed to read password for --" + longOptionName + ": " + e.getMessage(), e);
        }
        if (line == null) {
            throw new EOFException("Unexpected end of input while reading password for --" + longOptionName);
        }
        return line.toCharArray();
    }
}

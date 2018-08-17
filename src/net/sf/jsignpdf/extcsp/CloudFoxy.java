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
 * Contributor(s): CloudFoxy.
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
package net.sf.jsignpdf.extcsp;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.types.HashAlgorithm;
import org.apache.log4j.Logger;
import org.bouncycastle.util.encoders.Base64;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.MessageFormat;
import java.util.LinkedList;

import static net.sf.jsignpdf.Constants.RES;

/**
 * This class implements a connector to CloudFoxy (https://gitlab.com/cloudfoxy) - a remote API for
 * smart cards.
 */
public class CloudFoxy implements IExternalCryptoProvider {
	private final static Logger LOGGER = Logger.getLogger(CloudFoxy.class);

	/**
	 * Creates an instance of the class
	 * @return CloudFoxy instance
	 */
	public static CloudFoxy getInstance() {
		return new CloudFoxy();
	}

	/**
	 * A short method, which simply returns the CSP name for GUI
	 * @return String - the CSP name
	 */
	public String getName() {
		return Constants.KEYSTORE_TYPE_CLOUDFOXY;
	}

	/**
	 * The method returns a certificate chain for the provided alias
	 * @param options - command line / GUI provided options like keystore, PIN/password, alias, ...
	 * @return Certificate[] - a list of certificates, or null if there was an error
	 */
	public Certificate[] getChain(BasicSignerOptions options) {
		Certificate[] chain = null;

		String remoteAddress = options.getKsFile();
		if ((remoteAddress == null) || (!remoteAddress.contains(":")) || (options.getKeyAlias() == null)) {
			return null;
		}

		int cmdId = (int)(Math.random() * 100000);
		String cert_chain_request = MessageFormat.format(">{0}|\n>{1}:CHAIN|",
				options.getKeyAlias(), cmdId);

		String address [] = options.getKsFile().split(":");
		String hostname = address[0];
		int port = Integer.parseInt(address[1]);

		try {
			Socket socket = new Socket(hostname, port);

			OutputStream output = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(output, true);
			writer.println(cert_chain_request);

			InputStream input = socket.getInputStream();

			BufferedReader reader_sock = new BufferedReader(new InputStreamReader(input));

			String line = reader_sock.readLine();

			socket.close();

			if (line == null) {
				return null;
			}
			String []certChain = line.split(":");
			if (certChain.length < 2) {
				LOGGER.error(RES.get("extcsp.nocert", "-"));
				return null;
			} else if (certChain[1].length() < 5) {
				LOGGER.error(RES.get("extcsp.nocert", certChain[1]));
				return null;
			}
			chain = new Certificate[certChain.length - 1];
			CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
			InputStream in;
			for (int i = 1; i<certChain.length; i++) {
				// convert the certificate
				byte[] decodedBytes = Base64.decode(certChain[i]);
				in = new ByteArrayInputStream(decodedBytes);
				chain[i - 1] = certFactory.generateCertificate(in);
			}
		} catch (UnknownHostException ex) {
			LOGGER.error(RES.get("extcsp.nohost", hostname, ex.getMessage()));
			return null;
		} catch (IOException ex) {
			LOGGER.error(RES.get("extcsp.iohost", ex.getMessage()));
			return null;
		} catch (CertificateException ex) {
			LOGGER.error(RES.get("extcsp.certfactory", ex.getMessage()));
		}

		return chain;
	}

	/**
	 * The method takes an initial fingerprint of the document, and creates and external signature,
	 * which can be used for the 'setExternalDigest' method.
	 * @param options - command line / GUI provided options like keystore, PIN/password, alias, ...
	 * @param fingerprint - byte array containing the document fingerprint (only SHA1 and SHA256 are supported)
	 * @return byte[] with the signature, null if there was an error
	 */
	public byte[] getSignature(BasicSignerOptions options, byte[] fingerprint) {
		byte[] signature = null;
		HashAlgorithm hashAlgorithm = options.getHashAlgorithmX();

		if ((hashAlgorithm != HashAlgorithm.SHA1) && (hashAlgorithm != HashAlgorithm.SHA256)) {
			LOGGER.error(RES.get("extcsp.unknownhashalg", options.getHashAlgorithm().getAlgorithmName()));
			return null;
		}

		// we first need to compute a new hash - of the authenticated attribute
		final MessageDigest messageDigestInner;
		try {
			messageDigestInner = MessageDigest.getInstance(hashAlgorithm.getAlgorithmName());
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		messageDigestInner.update(fingerprint, 0, fingerprint.length);
		byte hashInner[] = messageDigestInner.digest();

		// we need to encode the hash into he
		StringBuilder hex = new StringBuilder();
		for (byte one_byte : hashInner) {
			hex.append(String.format("%02X", one_byte));
		}
		int cmdId = (int)(Math.random() * 100000);

		String signing_request;
		if (options.getKeyPasswdStr() != null) {
			signing_request = MessageFormat.format(">{0}:{1}|\n>{2}:SIGN:{3}|",
					options.getKeyAlias(), options.getKeyPasswdStr(),
					cmdId, hex.toString());
		} else {
			signing_request = MessageFormat.format(">{0}|\n>{2}:SIGN:{3}|",
					options.getKeyAlias(),
					cmdId, hex.toString());
		}

		String address [] = options.getKsFile().split(":");
		String hostname = address[0];
		int port = Integer.parseInt(address[1]);

		try {
			Socket socket = new Socket(hostname, port);

			OutputStream output = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(output, true);
			writer.println(signing_request);

			InputStream input = socket.getInputStream();

			BufferedReader reader_sock = new BufferedReader(new InputStreamReader(input));

			String line = reader_sock.readLine();
			String []signatureParts = line.split(":");

			socket.close();
			if (signatureParts.length < 2) {
				LOGGER.error(RES.get("extcsp.nosignature"));
			} else if (signatureParts[1].length() < 5) {
				LOGGER.error(RES.get("extcsp.nosignatureerr", signatureParts[1]));
			} else {
				// convert the signature from hex to binary
				int len = signatureParts[1].length(); // there's "@@" at the end - now on the new line
				signature = new byte[len / 2];
				for (int i = 0; i < len; i += 2) {
					signature[i / 2] = (byte) ((Character.digit(signatureParts[1].charAt(i), 16) << 4)
							+ Character.digit(signatureParts[1].charAt(i + 1), 16));
				}
			}
		} catch (UnknownHostException ex) {
			LOGGER.error(RES.get("extcsp.nohost", hostname, ex.getMessage()));
		} catch (IOException ex) {
			LOGGER.error(RES.get("extcsp.iohost", ex.getMessage()));
		}
		return signature;
	}

	/**
	 * Query the crypto provider and return a list of aliases available.
	 * @param options - command line / GUI provided options like keystore, PIN/password, alias, ...
	 * @return LinkedList<String> - a list of names
	 * @throws NullPointerException - when the list can't be created
	 */
	public LinkedList<String> getAliasesList(BasicSignerOptions options) throws NullPointerException {

		LinkedList<String> aliasList;

		if ((options.getKsFile() == null) || (!options.getKsFile().contains(":"))) {
			throw new NullPointerException(RES.get("error.keystoreNull"));
		} else {
			int cmdId = (int) (Math.random() * 100000);
			String address[] = options.getKsFile().split(":");
			try {
				Socket socket = new Socket(address[0], Integer.parseInt(address[1]));

				String alias_request = ">all readers\n>" + cmdId + ":ALIASES";
				OutputStream output = socket.getOutputStream();
				PrintWriter writer = new PrintWriter(output, true);
				writer.println(alias_request);
				InputStream input = socket.getInputStream();
				BufferedReader reader_sock = new BufferedReader(new InputStreamReader(input));
				String aliasesRaw = reader_sock.readLine();
				socket.close();

				String aliases_response[] = aliasesRaw.split(":");
				aliasList = new LinkedList<String>();
				if (aliases_response.length >= 2) {
					String aliases64[] = aliases_response[1].split("\\|");
					int partCounter = 0;
					for (String each_alias : aliases64) {
						Charset utf8Charset = Charset.forName("UTF-8");
						String name = utf8Charset.decode(ByteBuffer.wrap(Base64.decode(each_alias))).toString();
						aliasList.add(name);
					}
				}
			} catch (UnknownHostException ex) {
				LOGGER.error(RES.get("extcsp.nohost", address[0], ex.getMessage()));
				throw new NullPointerException(RES.get("error.keystoreNull"));
			} catch (IOException ex) {
				LOGGER.error(RES.get("extcsp.iohost", ex.getMessage()));
				throw new NullPointerException(RES.get("error.keystoreNull"));
			} catch (Exception ex) {
				throw new NullPointerException(RES.get("error.keystoreNull"));
			}
		}
		return aliasList;
	}
}

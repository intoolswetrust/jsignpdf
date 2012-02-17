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
package net.sf.jsignpdf.verify;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model used for displaying X509Certificates in Verifier GUI.
 * @author Josef Cacek
 */
public class CertificatesTableModel extends AbstractTableModel implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final int COLUMN_ALIAS = 0;
	public static final int COLUMN_DN = 1;
	public static final int COLUMN_VALID_FROM = 2;
	public static final int COLUMN_VALID_TO = 3;

	private List<CertificateWithAlias> data = 
		Collections.synchronizedList(new ArrayList<CertificateWithAlias>());

	/**
	 * Adds one certificate to the model.
	 * @param anAlias Alias from keystore
	 * @param aCertif X509 certificate
	 */
	public void addRow(String anAlias, X509Certificate aCertif) {
		if (aCertif==null) {
			throw new NullPointerException("Certificate can't be null.");
		}
		data.add(new CertificateWithAlias(anAlias, aCertif));
		final int tmpPos = data.size()-1;
		fireTableRowsInserted(tmpPos, tmpPos);
	}
	
	/**
	 * Deletes all entries from model.
	 */
	public void clearModel() {
		if (!data.isEmpty()) {
			final int tmpLastPos = data.size() - 1;
			data.clear();
			fireTableRowsDeleted(0, tmpLastPos);
		}
	}
	
	/**
	 * Return unmodifiable list of Certificates in model.
	 * @return list of Certificates and their aliases
	 */
	public List<CertificateWithAlias> getCertificates() {
		return Collections.unmodifiableList(data);
	}
	
	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	public int getColumnCount() {
		return 4;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	public int getRowCount() {
		return data.size();
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	public Object getValueAt(int rowIndex, int columnIndex) {
		CertificateWithAlias tmpCert = data.get(rowIndex);
		Object tmpResult = null;
		switch (columnIndex) {
		case COLUMN_ALIAS:
			tmpResult = tmpCert.getAlias();
			break;
		case COLUMN_DN:
			tmpResult = tmpCert.getCertificate().getIssuerX500Principal().getName();
			break;
		case COLUMN_VALID_FROM:
			tmpResult = tmpCert.getCertificate().getNotBefore();
			break;
		case COLUMN_VALID_TO:
			tmpResult = tmpCert.getCertificate().getNotAfter();
			break;

		}
		return tmpResult;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		Class<?> tmpResult;
		switch (columnIndex) {
		case COLUMN_VALID_FROM:
		case COLUMN_VALID_TO:
			tmpResult = Date.class;
			break;
		default:
			tmpResult = String.class;
			break;
		}
		return tmpResult;
	}

	/* (non-Javadoc)
	 * @see javax.swing.table.AbstractTableModel#getColumnName(int)
	 */
	@Override
	public String getColumnName(int column) {
		String tmpResult = null;
		switch (column) {
		case COLUMN_ALIAS:
			tmpResult = "Alias";
			break;
		case COLUMN_DN:
			tmpResult = "DN";
			break;
		case COLUMN_VALID_FROM:
			tmpResult = "Valid from";
			break;
		case COLUMN_VALID_TO:
			tmpResult = "Valid to";
			break;

		}
		return tmpResult;
	}

	
}

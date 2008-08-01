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

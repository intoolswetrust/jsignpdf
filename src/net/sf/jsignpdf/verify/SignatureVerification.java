package net.sf.jsignpdf.verify;

import java.util.Calendar;

/**
 * This class represents a result of a single signature verification.
 * @author Josef Cacek
 * @author $Author: kwart $
 * @version $Revision: 1.1 $
 * @created $Date: 2008/06/13 13:27:45 $
 */
public class SignatureVerification {
	
	private String name;
	private String subject;
	private int revision;
	private boolean wholeDocument;
	private Calendar date;
	private boolean modified;
	private Object[] fails;
	
	
	/**
	 * Default constructore
	 */
	public SignatureVerification() {
		//nothing to do here
	}
	
	/**
	 * Constructor, which fills name of signature
	 * @param aName name of signature
	 */
	public SignatureVerification(final String aName) {
		name = aName;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public int getRevision() {
		return revision;
	}
	public void setRevision(int revision) {
		this.revision = revision;
	}
	public boolean isWholeDocument() {
		return wholeDocument;
	}
	public void setWholeDocument(boolean wholeDocument) {
		this.wholeDocument = wholeDocument;
	}
	public Calendar getDate() {
		return date;
	}
	public void setDate(Calendar date) {
		this.date = date;
	}
	public boolean isModified() {
		return modified;
	}
	public void setModified(boolean modified) {
		this.modified = modified;
	}
	public Object[] getFails() {
		return fails;
	}
	public void setFails(Object[] fails) {
		this.fails = fails;
	}
}

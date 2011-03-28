package net.sf.jsignpdf.verify;

import java.util.Arrays;
import java.util.Calendar;

/**
 * This class represents a result of a single signature verification.
 * 
 * @author Josef Cacek
 * @author $Author: kwart $
 * @version $Revision: 1.4 $
 * @created $Date: 2011/03/28 14:19:34 $
 */
public class SignatureVerification {

	private String signName;
	private String name;
	private String subject;
	private int revision;
	private boolean wholeDocument;
	private Calendar date;
	private boolean modified;
	private boolean ocspPresent;
	private boolean ocspValid;
	private Object[] fails;
	private String reason;
	private String location;

	/**
	 * Default constructore
	 */
	public SignatureVerification() {
		//nothing to do here
	}

	/**
	 * Constructor, which fills name of signature
	 * 
	 * @param aName
	 *            name of signature
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

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public boolean isOcspPresent() {
		return ocspPresent;
	}

	public void setOcspPresent(boolean ocspPresent) {
		this.ocspPresent = ocspPresent;
	}

	public boolean isOcspValid() {
		return ocspValid;
	}

	public void setOcspValid(boolean ocspValid) {
		this.ocspValid = ocspValid;
	}

	public String getSignName() {
		return signName;
	}

	public void setSignName(String signName) {
		this.signName = signName;
	}

	public String toString() {
		return "Signature verification [" + "\n signName=" + signName + "\n name=" + name + "\n subject=" + subject
				+ "\n date=" + date.getTime() + "\n reason=" + reason + "\n location=" + location + "\n revision="
				+ revision + "\n wholeDocument=" + wholeDocument + "\n modified=" + modified + "\n ocspPresent="
				+ ocspPresent + "\n ocspValid=" + ocspValid + "\n fails="
				+ (fails == null ? "no" : Arrays.asList(fails)) + "\n]";
	}

}

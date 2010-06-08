package net.sf.jsignpdf;

/**
 * Listener class for "signing finished" event.
 * 
 * @author Josef Cacek
 */
public interface SignResultListener {

	/**
	 * Method fired when signer finishes. Parameter says if it was successful.
	 * 
	 * @param e
	 *            null if finished succesfully or the reason (Exception)
	 */
	void signerFinishedEvent(Exception e);
}

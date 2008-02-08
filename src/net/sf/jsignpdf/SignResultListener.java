package net.sf.jsignpdf;

/**
 * Listener class for "signing finished" event.
 * @author Josef Cacek
 */
public interface SignResultListener {

	/**
	 * Method fired when signer finishes. Parameter says if it was
	 * successful.
	 * @param success flag which says if signing was successful.
	 */
	void signerFinishedEvent(boolean success);
}

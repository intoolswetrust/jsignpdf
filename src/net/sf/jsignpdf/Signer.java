package net.sf.jsignpdf;

import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * Simple main class - it sets system Look&Feel and creates SignPdfForm GUI
 * @author Josef Cacek
 */
public class Signer {

	/**
	 * Main.
	 * @param args
	 */
	public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Can't set Look&Feel.");
        }
		SignPdfForm tmpForm = new SignPdfForm(WindowConstants.EXIT_ON_CLOSE);
		tmpForm.pack();
		tmpForm.setVisible(true);
	}

}

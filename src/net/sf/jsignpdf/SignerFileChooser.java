package net.sf.jsignpdf;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicFileChooserUI;

/**
 * Improved JFileChooser. It contains some small bug-fixes.
 * @author Josef Cacek
 */
public class SignerFileChooser extends JFileChooser {

	private static final long serialVersionUID = 1L;

	/**
	 * File filter for PDF files
	 */
	public static final FileFilter FILEFILTER_PDF = new FileFilter() {
		public boolean accept(File f) {
			return f.isDirectory() || f.getName().toLowerCase().endsWith(".pdf");
		}
		public String getDescription() {
			return ResourceProvider.getInstance().get("filefilter.pdf");
		}
	};

	/**
	 * Shows rewrite confirmation dialog for SAVE_DIALOGs, when the selected
	 * file already exists.
	 * @see javax.swing.JFileChooser#approveSelection()
	 */
	@Override
	public void approveSelection() {
		if (getDialogType() == SAVE_DIALOG) {
			File file = getSelectedFile();
			if (file != null && file.exists()) {
				if (!confirmOverwrite(file)) {
					// User doesn't want to overwrite the file
					return;
				}
			}
		}
		super.approveSelection();
	}

	/**
	 * Shows question dialog "File exists. Overwrite?"
	 * @param file file
	 * @return
	 */
	private boolean confirmOverwrite(File file) {
		final ResourceProvider res = ResourceProvider.getInstance();
		final String tmpMessage = res.get(
			"filechooser.overwrite.question",
			new String[] {file.getAbsolutePath()});
		return JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(this,
			tmpMessage,
			res.get("filechooser.save.warning"),
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE,
			null,
			null,
			null);
	}

	/**
	 * Clears file name when null file is provided.
	 * See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4893572
	 * @see javax.swing.JFileChooser#setSelectedFile(java.io.File)
	 */
	@Override
	public void setSelectedFile(File file) {
		super.setSelectedFile(file);
		// safety check
		if (getUI() instanceof BasicFileChooserUI) {
			// grab the ui and set the filename
			BasicFileChooserUI tmpUi = (BasicFileChooserUI) getUI();
			tmpUi.setFileName(file == null ? "" : file.getName());
		}
	}

	/**
	 * Displays file chooser dialog of given type and with givet FileFilter.
	 * @param aFileField assigned textfield
	 * @param aFilter filefilter
	 * @param aType dialog type (SAVE_DIALOG, OPEN_DIALOG)
	 */
	public void showFileChooser(final JTextField aFileField, final FileFilter aFilter, final int aType) {
		setDialogType(aType);
		resetChoosableFileFilters();
		if (aFilter!=null) {
			setFileFilter(aFilter);
		}
		String tmpFileName = aFileField.getText();
		if (tmpFileName==null || tmpFileName.length()==0) {
			setSelectedFile(null);
		} else {
			File tmpFile = new File(tmpFileName);
			setSelectedFile(tmpFile);
		}
		if (JFileChooser.APPROVE_OPTION == showDialog(this, null)) {
			aFileField.setText(getSelectedFile().getAbsolutePath());
		}
	}
}

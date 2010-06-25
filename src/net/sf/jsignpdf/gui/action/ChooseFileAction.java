package net.sf.jsignpdf.gui.action;

import java.awt.event.ActionEvent;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import net.sf.jsignpdf.SignerFileChooser;
import net.sf.jsignpdf.gui.GuiConstants;

import com.jgoodies.binding.value.ValueHolder;

/**
 * This action displays "Choose file dialog" and stores selected value to a
 * value holder.
 * 
 * @author Josef Cacek
 */
public class ChooseFileAction extends I18nAbstractAction {

	private static final long serialVersionUID = 1L;

	private ValueHolder pathValueHolder;
	private FileFilter filter;
	private boolean openDlg;

	/**
	 * Ctor.
	 */
	public ChooseFileAction(FileFilter aFileFilter, boolean anOpenDlg) {
		super(GuiConstants.Actions.CHOOSEFILE_NAME, GuiConstants.Actions.CHOOSEFILE_TOOLTIP,
				GuiConstants.Actions.CHOOSEFILE_ICON);
		filter = aFileFilter;
		pathValueHolder = new ValueHolder();
		openDlg = anOpenDlg;
	}

	/**
	 * Loads aliases to combobox model.
	 */
	public void actionPerformed(ActionEvent e) {
		pathValueHolder.setValue(SignerFileChooser.getInstance().showFileChooser(pathValueHolder.getString(), filter,
				openDlg ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG));
	}

	/**
	 * Returns value holder for the path.
	 * 
	 * @return
	 */
	public ValueHolder getPathValueHolder() {
		return pathValueHolder;
	}
}

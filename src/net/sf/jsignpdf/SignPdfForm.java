package net.sf.jsignpdf;

import java.awt.Toolkit;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;

import javax.swing.AbstractButton;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

/**
 * GUI for PDFSigner.
 * @author  Josef Cacek
 */
public class SignPdfForm extends javax.swing.JFrame implements SignResultListener {

	private static final long serialVersionUID = 1L;

	private JFileChooser fc = new SignerFileChooser();

	protected final PropertyProvider props = PropertyProvider.getInstance();
	protected final ResourceProvider res = ResourceProvider.getInstance();

	private PrintWriter infoWriter;
	private TextAreaStream infoStream;
	private boolean autoclose = false;
	private SignerOptions options = new SignerOptions();
	private SignerLogic signerLogic = new SignerLogic(options);
	private KeyStoreUtils ksUtils = new KeyStoreUtils(options);

	/** Creates new form SignPdfForm */
	public SignPdfForm(int aCloseOperation) {
		initComponents();
		options.loadOptions();
		translateLabels();

		setDefaultCloseOperation(aCloseOperation);

		infoStream = new TextAreaStream(infoTextArea);
		infoWriter = new PrintWriter(infoStream, true);

		//set Icon of frames
		URL tmpImgUrl = getClass().getClassLoader().getResource("signedpdf32.png");
		setIconImage(Toolkit.getDefaultToolkit().getImage(tmpImgUrl));
		infoDialog.setIconImage(getIconImage());

		infoDialog.pack();

		options.setPrintWriter(infoWriter);
		options.setListener(this);

		cbKeystoreType.setModel(new DefaultComboBoxModel(ksUtils.getKeyStrores()));
		cbCertLevel.setModel(new DefaultComboBoxModel(CertificationLevel.values()));

		updateFromOptions();

		chkbAdvancedActionPerformed(null);
		chkbPdfEncryptedActionPerformed(null);
	}

	/**
	 * Application translations.
	 */
	private void translateLabels() {
		setTitle(res.get("gui.title", new String[] {Constants.VERSION}));
		setLabelAndMnemonic(lblKeystoreType, "gui.keystoreType.label");
		setLabelAndMnemonic(chkbAdvanced, "gui.advancedView.checkbox");
		setLabelAndMnemonic(lblKeystoreFile, "gui.keystoreFile.label");
		setLabelAndMnemonic(lblKeystorePwd, "gui.keystorePassword.label");
		setLabelAndMnemonic(chkbStorePwd, "gui.storePasswords.checkbox");
		setLabelAndMnemonic(lblAlias, "gui.alias.label");
		setLabelAndMnemonic(btnLoadAliases, "gui.loadAliases.button");
		setLabelAndMnemonic(lblKeyPwd, "gui.keyPassword.label");
		setLabelAndMnemonic(lblInPdfFile, "gui.inPdfFile.label");
		setLabelAndMnemonic(chkbPdfEncrypted, "gui.pdfEncrypted.checkbox");
		setLabelAndMnemonic(lblPdfOwnerPwd, "gui.pdfOwnerPwd.label");
		setLabelAndMnemonic(lblPdfUserPwd, "gui.pdfUserPwd.label");
		setLabelAndMnemonic(lblOutPdfFile, "gui.outPdfFile.label");
		setLabelAndMnemonic(lblReason, "gui.reason.label");
		setLabelAndMnemonic(lblLocation, "gui.location.label");
		setLabelAndMnemonic(lblCertLevel, "gui.certLevel.label");
		setLabelAndMnemonic(chkbAppendSignature, "gui.appendSignature.checkbox");

		btnKeystoreFile.setText(res.get("gui.browse.button"));
		btnInPdfFile.setText(res.get("gui.browse.button"));
		btnOutPdfFile.setText(res.get("gui.browse.button"));

		setLabelAndMnemonic(btnSignIt,"gui.signIt.button");

		infoDialog.setTitle(res.get("gui.info.title"));
		btnInfoClose.setText(res.get("gui.info.close.button"));
	}

	/**
	 * Sets translations and mnemonics for labels and different kind of buttons
	 * @param aComponent component in which should be label set
	 * @param aKey message key
	 */
	private void setLabelAndMnemonic(final JComponent aComponent, final String aKey) {
		final String tmpLabelText = res.get(aKey);
		final int tmpMnemIndex = res.getMnemonicIndex(aKey);
		if (aComponent instanceof JLabel) {
			final JLabel tmpLabel = (JLabel) aComponent;
			tmpLabel.setText(tmpLabelText);
			if (tmpMnemIndex>-1) {
				tmpLabel.setDisplayedMnemonic(tmpLabelText.toLowerCase().charAt(tmpMnemIndex));
				tmpLabel.setDisplayedMnemonicIndex(tmpMnemIndex);
			}
		} else if (aComponent instanceof AbstractButton) {
			//handles Buttons, Checkboxes and Radiobuttons
			final AbstractButton tmpBtn = (AbstractButton) aComponent;
			tmpBtn.setText(tmpLabelText);
			if (tmpMnemIndex>-1) {
				tmpBtn.setMnemonic(tmpLabelText.toLowerCase().charAt(tmpMnemIndex));
			}
		} else {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Loads properties saved by previous run of application
	 */
	private void updateFromOptions() {
		cbKeystoreType.setSelectedItem(options.getKsType());
		chkbAdvanced.setSelected(options.isAdvanced());
		tfKeystoreFile.setText(options.getKsFile());
		pfKeystorePwd.setText(options.getKsPasswdStr());
		chkbStorePwd.setSelected(options.isStorePasswords());
		cbAlias.setSelectedItem(options.getKeyAlias());
		pfKeyPwd.setText(options.getKeyPasswdStr());
		tfInPdfFile.setText(options.getInFile());
		chkbPdfEncrypted.setSelected(options.isEncrypted());
		pfPdfOwnerPwd.setText(options.getPdfOwnerPwdStr());
		pfPdfUserPwd.setText(options.getPdfUserPwdStr());
		tfOutPdfFile.setText(options.getOutFile());
		tfReason.setText(options.getReason());
		tfLocation.setText(options.getLocation());
		cbCertLevel.setSelectedItem(options.getCertLevel());
		chkbAppendSignature.setSelected(options.isAppend());
		switchAdvancedView(options.isAdvanced());
		switchEncryptedPdf(options.isEncrypted());
		pack();
	}

	/**
	 * stores values from this Form to the instance of {@link SignerOptions}
	 */
	private void storeToOptions() {
		options.setKsType((String) cbKeystoreType.getSelectedItem());
		options.setAdvanced(chkbAdvanced.isSelected());
		options.setKsFile(tfKeystoreFile.getText());
		options.setKsPasswd(pfKeystorePwd.getPassword());
		options.setStorePasswords(chkbStorePwd.isSelected());
		options.setKeyAlias((String) cbAlias.getSelectedItem());
		options.setKeyPasswd(pfKeyPwd.getPassword());
		options.setInFile(tfInPdfFile.getText());
		options.setEncrypted(chkbPdfEncrypted.isSelected());
		options.setPdfOwnerPwd(pfPdfOwnerPwd.getPassword());
		options.setPdfUserPwd(pfPdfUserPwd.getPassword());
		options.setOutFile(tfOutPdfFile.getText());
		options.setReason(tfReason.getText());
		options.setLocation(tfLocation.getText());
		options.setCertLevel((CertificationLevel) cbCertLevel.getSelectedItem());
		options.setAppend(chkbAppendSignature.isSelected());
	}

	/**
	 * Handles switching Advanced checkbox. Sets some components visible/hidden
	 * depending on given status flag.
	 * @param anAdvanced flag - advanced view is enabled
	 */
	private void switchAdvancedView(boolean anAdvanced) {
		btnLoadAliases.setVisible(anAdvanced);
		lblAlias.setVisible(anAdvanced);
		cbAlias.setVisible(anAdvanced);
		lblKeyPwd.setVisible(anAdvanced);
		pfKeyPwd.setVisible(anAdvanced);
		chkbPdfEncrypted.setVisible(anAdvanced);
		lblPdfOwnerPwd.setVisible(anAdvanced);
		pfPdfOwnerPwd.setVisible(anAdvanced);
		lblPdfUserPwd.setVisible(anAdvanced);
		pfPdfUserPwd.setVisible(anAdvanced);
		lblCertLevel.setVisible(anAdvanced);
		cbCertLevel.setVisible(anAdvanced);
		chkbAppendSignature.setVisible(anAdvanced);
	}

	/**
	 * Handles switching Encrypted checkbox.
	 * Sets some components enabled/disabled
	 * depending on given status flag.
	 * @param anAdvanced flag - encrypted view is enabled
	 */
	private void switchEncryptedPdf(boolean anEnabled) {
		pfPdfOwnerPwd.setEnabled(anEnabled);
		pfPdfUserPwd.setEnabled(anEnabled);
		chkbAppendSignature.setEnabled(!anEnabled);
	}

	/**
	 * Displays file chooser dialog of given type and with givet FileFilter.
	 * @param aFileField assigned textfield
	 * @param aFilter filefilter
	 * @param aType dialog type (SAVE_DIALOG, OPEN_DIALOG)
	 */
	void showFileChooser(final JTextField aFileField, final FileFilter aFilter, final int aType) {
		fc.setDialogType(aType);
		fc.resetChoosableFileFilters();
		if (aFilter!=null) {
			fc.setFileFilter(aFilter);
		}
		String tmpFileName = aFileField.getText();
		if (tmpFileName==null || tmpFileName.length()==0) {
			fc.setSelectedFile(null);
		} else {
			File tmpFile = new File(tmpFileName);
			fc.setSelectedFile(tmpFile);
		}
		if (JFileChooser.APPROVE_OPTION == fc.showDialog(this, null)) {
			aFileField.setText(fc.getSelectedFile().getAbsolutePath());
		}
	}


	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		infoDialog = new javax.swing.JFrame();
		infoScrollPane = new javax.swing.JScrollPane();
		infoTextArea = new javax.swing.JTextArea();
		btnInfoClose = new javax.swing.JButton();
		lblKeystoreType = new javax.swing.JLabel();
		cbKeystoreType = new javax.swing.JComboBox();
		lblKeystoreFile = new javax.swing.JLabel();
		tfKeystoreFile = new javax.swing.JTextField();
		btnKeystoreFile = new javax.swing.JButton();
		lblKeystorePwd = new javax.swing.JLabel();
		pfKeystorePwd = new javax.swing.JPasswordField();
		chkbStorePwd = new javax.swing.JCheckBox();
		lblAlias = new javax.swing.JLabel();
		cbAlias = new javax.swing.JComboBox();
		btnLoadAliases = new javax.swing.JButton();
		lblKeyPwd = new javax.swing.JLabel();
		pfKeyPwd = new javax.swing.JPasswordField();
		lblInPdfFile = new javax.swing.JLabel();
		tfInPdfFile = new javax.swing.JTextField();
		btnInPdfFile = new javax.swing.JButton();
		chkbPdfEncrypted = new javax.swing.JCheckBox();
		lblPdfOwnerPwd = new javax.swing.JLabel();
		pfPdfOwnerPwd = new javax.swing.JPasswordField();
		lblPdfUserPwd = new javax.swing.JLabel();
		pfPdfUserPwd = new javax.swing.JPasswordField();
		lblOutPdfFile = new javax.swing.JLabel();
		tfOutPdfFile = new javax.swing.JTextField();
		btnOutPdfFile = new javax.swing.JButton();
		lblReason = new javax.swing.JLabel();
		tfReason = new javax.swing.JTextField();
		lblLocation = new javax.swing.JLabel();
		tfLocation = new javax.swing.JTextField();
		lblCertLevel = new javax.swing.JLabel();
		cbCertLevel = new javax.swing.JComboBox();
		chkbAppendSignature = new javax.swing.JCheckBox();
		btnSignIt = new javax.swing.JButton();
		chkbAdvanced = new javax.swing.JCheckBox();

		infoDialog.setTitle("PDF Signer Output");
		infoDialog.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				infoDialogWindowClosing(evt);
			}
		});

		infoTextArea.setColumns(80);
		infoTextArea.setEditable(false);
		infoTextArea.setFont(new java.awt.Font("Courier New", 1, 10));
		infoTextArea.setRows(25);
		infoTextArea.setMinimumSize(new java.awt.Dimension(200, 180));
		infoScrollPane.setViewportView(infoTextArea);

		infoDialog.getContentPane().add(infoScrollPane, java.awt.BorderLayout.CENTER);

		btnInfoClose.setText("Close");
		btnInfoClose.setMinimumSize(new java.awt.Dimension(50, 20));
		btnInfoClose.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnInfoCloseActionPerformed(evt);
			}
		});

		infoDialog.getContentPane().add(btnInfoClose, java.awt.BorderLayout.SOUTH);

		getContentPane().setLayout(new java.awt.GridBagLayout());

		setTitle("SignPdf");
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				formWindowClosing(evt);
			}
		});

		lblKeystoreType.setLabelFor(cbKeystoreType);
		lblKeystoreType.setText("Keystore type");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblKeystoreType, gridBagConstraints);

		cbKeystoreType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "PKCS#12", "JKS" }));
		cbKeystoreType.setMinimumSize(new java.awt.Dimension(150, 20));
		cbKeystoreType.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(cbKeystoreType, gridBagConstraints);

		lblKeystoreFile.setLabelFor(tfKeystoreFile);
		lblKeystoreFile.setText("Keystore file");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblKeystoreFile, gridBagConstraints);

		tfKeystoreFile.setMinimumSize(new java.awt.Dimension(250, 20));
		tfKeystoreFile.setPreferredSize(new java.awt.Dimension(250, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 4.0;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfKeystoreFile, gridBagConstraints);

		btnKeystoreFile.setText("Browse...");
		btnKeystoreFile.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnKeystoreFileActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnKeystoreFile, gridBagConstraints);

		lblKeystorePwd.setLabelFor(pfKeystorePwd);
		lblKeystorePwd.setText("Keystore password");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblKeystorePwd, gridBagConstraints);

		pfKeystorePwd.setMinimumSize(new java.awt.Dimension(150, 20));
		pfKeystorePwd.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(pfKeystorePwd, gridBagConstraints);

		chkbStorePwd.setText("Store passwords");
		chkbStorePwd.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbStorePwd.setMargin(new java.awt.Insets(0, 0, 0, 0));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 2;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(chkbStorePwd, gridBagConstraints);

		lblAlias.setLabelFor(cbAlias);
		lblAlias.setText("Key alias");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblAlias, gridBagConstraints);

		cbAlias.setEditable(true);
		cbAlias.setMinimumSize(new java.awt.Dimension(150, 20));
		cbAlias.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(cbAlias, gridBagConstraints);

		btnLoadAliases.setText("Load keys");
		btnLoadAliases.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnLoadAliasesActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnLoadAliases, gridBagConstraints);

		lblKeyPwd.setLabelFor(pfKeyPwd);
		lblKeyPwd.setText("Key password");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblKeyPwd, gridBagConstraints);

		pfKeyPwd.setMinimumSize(new java.awt.Dimension(150, 20));
		pfKeyPwd.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 4;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(pfKeyPwd, gridBagConstraints);

		lblInPdfFile.setLabelFor(tfInPdfFile);
		lblInPdfFile.setText("Input PDF file");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblInPdfFile, gridBagConstraints);

		tfInPdfFile.setMinimumSize(new java.awt.Dimension(150, 20));
		tfInPdfFile.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfInPdfFile, gridBagConstraints);

		btnInPdfFile.setText("Browse...");
		btnInPdfFile.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnInPdfFileActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 5;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnInPdfFile, gridBagConstraints);

		chkbPdfEncrypted.setText("Encrypted");
		chkbPdfEncrypted.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbPdfEncrypted.setMargin(new java.awt.Insets(0, 0, 0, 0));
		chkbPdfEncrypted.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				chkbPdfEncryptedActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 6;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(chkbPdfEncrypted, gridBagConstraints);

		lblPdfOwnerPwd.setLabelFor(pfPdfOwnerPwd);
		lblPdfOwnerPwd.setText("Owner password");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblPdfOwnerPwd, gridBagConstraints);

		pfPdfOwnerPwd.setEnabled(false);
		pfPdfOwnerPwd.setMinimumSize(new java.awt.Dimension(150, 20));
		pfPdfOwnerPwd.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 7;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(pfPdfOwnerPwd, gridBagConstraints);

		lblPdfUserPwd.setLabelFor(pfPdfUserPwd);
		lblPdfUserPwd.setText("User password");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblPdfUserPwd, gridBagConstraints);

		pfPdfUserPwd.setEnabled(false);
		pfPdfUserPwd.setMinimumSize(new java.awt.Dimension(150, 20));
		pfPdfUserPwd.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 8;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(pfPdfUserPwd, gridBagConstraints);

		lblOutPdfFile.setLabelFor(tfOutPdfFile);
		lblOutPdfFile.setText("Output PDF file");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 9;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblOutPdfFile, gridBagConstraints);

		tfOutPdfFile.setMinimumSize(new java.awt.Dimension(150, 20));
		tfOutPdfFile.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 9;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfOutPdfFile, gridBagConstraints);

		btnOutPdfFile.setText("Browse...");
		btnOutPdfFile.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnOutPdfFileActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 9;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(btnOutPdfFile, gridBagConstraints);

		lblReason.setLabelFor(tfReason);
		lblReason.setText("Reason");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 10;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblReason, gridBagConstraints);

		tfReason.setMinimumSize(new java.awt.Dimension(150, 20));
		tfReason.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 10;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfReason, gridBagConstraints);

		lblLocation.setLabelFor(tfLocation);
		lblLocation.setText("Location");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 11;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblLocation, gridBagConstraints);

		tfLocation.setMinimumSize(new java.awt.Dimension(150, 20));
		tfLocation.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 11;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(tfLocation, gridBagConstraints);

		lblCertLevel.setLabelFor(cbCertLevel);
		lblCertLevel.setText("Certification level");
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 12;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 10);
		getContentPane().add(lblCertLevel, gridBagConstraints);

		cbCertLevel.setMinimumSize(new java.awt.Dimension(150, 20));
		cbCertLevel.setPreferredSize(new java.awt.Dimension(150, 20));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 12;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(2, 0, 2, 0);
		getContentPane().add(cbCertLevel, gridBagConstraints);

		chkbAppendSignature.setText("Append signature");
		chkbAppendSignature.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbAppendSignature.setMargin(new java.awt.Insets(0, 0, 0, 0));
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 12;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(chkbAppendSignature, gridBagConstraints);

		btnSignIt.setFont(new java.awt.Font("Tahoma", 1, 12));
		btnSignIt.setIcon(new javax.swing.ImageIcon(getClass().getResource("/signedpdf26.png")));
		btnSignIt.setText("Sign It");
		btnSignIt.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnSignItActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 13;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
		getContentPane().add(btnSignIt, gridBagConstraints);

		chkbAdvanced.setText("Advanced view");
		chkbAdvanced.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
		chkbAdvanced.setMargin(new java.awt.Insets(0, 0, 0, 0));
		chkbAdvanced.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				chkbAdvancedActionPerformed(evt);
			}
		});

		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		gridBagConstraints.insets = new java.awt.Insets(1, 5, 1, 5);
		getContentPane().add(chkbAdvanced, gridBagConstraints);

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void btnLoadAliasesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoadAliasesActionPerformed
		storeToOptions();
		cbAlias.setModel(new DefaultComboBoxModel(ksUtils.getAliases()));
	}//GEN-LAST:event_btnLoadAliasesActionPerformed

	private void chkbPdfEncryptedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkbPdfEncryptedActionPerformed
		switchEncryptedPdf(chkbPdfEncrypted.isSelected());
	}//GEN-LAST:event_chkbPdfEncryptedActionPerformed

	private void chkbAdvancedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkbAdvancedActionPerformed
		switchAdvancedView(chkbAdvanced.isSelected());
		pack();
	}//GEN-LAST:event_chkbAdvancedActionPerformed

	private void infoDialogWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_infoDialogWindowClosing
		if (btnInfoClose.isEnabled()) {
			setVisible(true);
		}
	}//GEN-LAST:event_infoDialogWindowClosing


	/**
	 * @see net.sf.jsignpdf.SignResultListener#signerFinishedEvent(boolean)
	 */
	public synchronized void signerFinishedEvent(boolean success) {
		btnInfoClose.setEnabled(true);
		infoDialog.setDefaultCloseOperation(javax.swing.WindowConstants.HIDE_ON_CLOSE);
		if (autoclose) btnInfoCloseActionPerformed(null);
	}


	private void btnInfoCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInfoCloseActionPerformed
		infoDialog.setVisible(false);
		setVisible(true);
	}//GEN-LAST:event_btnInfoCloseActionPerformed

	/**
	 * Checks if file exists and it's possible write to it.
	 * @param aTF text field with file name filled
	 * @param aFileDescKey file description (used in error message)
	 * @return result of the check
	 */
	private boolean checkFileExists(JTextField aTF, String aFileDescKey) {
		final String tmpFileName = aTF.getText();
		try {
			if (tmpFileName!=null) {
				File tmpFile = new File(tmpFileName);
				if (tmpFile.canRead() && !tmpFile.isDirectory()) {
					return true;
				}
			}
		} catch (Exception e) {}

		final String tmpMsg = res.get("gui.fileNotExists.error",
				new String[] {res.get(aFileDescKey)});
		JOptionPane.showMessageDialog(this, tmpMsg, res.get("gui.check.error.title"), JOptionPane.ERROR_MESSAGE);
		return false;
	}

	/**
	 * Checks if is textfield filled
	 * @param aTF text field to check
	 * @param aDescKey text field description
	 * @return result of the check
	 */
	private boolean checkFilled(JTextField aTF, String aDescKey) {
		final String tmpFileName = aTF.getText();
		if (tmpFileName!=null && tmpFileName.length()>0) {
			return true;
		}
		final String tmpMsg = res.get("gui.valueNotFilled.error",
				new String[] {res.get(aDescKey)});
		JOptionPane.showMessageDialog(this,tmpMsg, res.get("gui.check.error.title"), JOptionPane.ERROR_MESSAGE);
		return false;
	}

	/**
	 * Checks if inFile and outFile are different.
	 * @return result of the check
	 */
	private boolean checkInOutDiffers() {
		final String tmpInName = tfInPdfFile.getText();
		final String tmpOutName = tfOutPdfFile.getText();
		boolean tmpResult = true;
		if (tmpInName!=null && tmpOutName!=null) {
			try {
				final File tmpInFile = (new File(tmpInName)).getAbsoluteFile();
				final File tmpOutFile = (new File(tmpOutName)).getAbsoluteFile();
				if (tmpInFile.equals(tmpOutFile)) {
					tmpResult = false;
					JOptionPane.showMessageDialog(this,
							res.get("gui.filesEqual.error"),
							res.get("gui.check.error.title"),
							JOptionPane.ERROR_MESSAGE);
				}
			} catch (Exception e) {
				tmpResult = false;
				JOptionPane.showMessageDialog(this,e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		return tmpResult;
	}

	/**
	 * Handles pressing of "Sign It" button. Creates and runs SignerLogic instance
	 * in a new thread.
	 * @param evt event
	 */
	private void btnSignItActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSignItActionPerformed
		storeToOptions();
		if (checkFileExists(tfInPdfFile, "gui.inPdfFile.label")
//				&& checkFileExists(tfKeystoreFile, "gui.keystoreFile.label")
				&& checkFilled(tfOutPdfFile, "gui.outPdfFile.label")
				&& checkInOutDiffers()) {
			infoStream.clear();
			btnInfoClose.setEnabled(false);
			infoDialog.setVisible(true);
			setVisible(false);
			infoDialog.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
			infoWriter.println(res.get("console.starting"));
			//Let's do it
			final Thread tmpST = new Thread(signerLogic);
			tmpST.start();
		}
	}//GEN-LAST:event_btnSignItActionPerformed

	private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
		storeToOptions();
		options.storeOptions();
	}//GEN-LAST:event_formWindowClosing

	private void btnOutPdfFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOutPdfFileActionPerformed
		showFileChooser(tfOutPdfFile, SignerFileChooser.FILEFILTER_PDF, JFileChooser.SAVE_DIALOG);

	}//GEN-LAST:event_btnOutPdfFileActionPerformed

	private void btnInPdfFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInPdfFileActionPerformed
		showFileChooser(tfInPdfFile, SignerFileChooser.FILEFILTER_PDF, JFileChooser.OPEN_DIALOG);
	}//GEN-LAST:event_btnInPdfFileActionPerformed

	private void btnKeystoreFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKeystoreFileActionPerformed
		showFileChooser(tfKeystoreFile, null, JFileChooser.OPEN_DIALOG);
	}//GEN-LAST:event_btnKeystoreFileActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnInPdfFile;
	private javax.swing.JButton btnInfoClose;
	private javax.swing.JButton btnKeystoreFile;
	private javax.swing.JButton btnLoadAliases;
	private javax.swing.JButton btnOutPdfFile;
	private javax.swing.JButton btnSignIt;
	private javax.swing.JComboBox cbAlias;
	private javax.swing.JComboBox cbCertLevel;
	private javax.swing.JComboBox cbKeystoreType;
	private javax.swing.JCheckBox chkbAdvanced;
	private javax.swing.JCheckBox chkbAppendSignature;
	private javax.swing.JCheckBox chkbPdfEncrypted;
	private javax.swing.JCheckBox chkbStorePwd;
	private javax.swing.JFrame infoDialog;
	private javax.swing.JScrollPane infoScrollPane;
	private javax.swing.JTextArea infoTextArea;
	private javax.swing.JLabel lblAlias;
	private javax.swing.JLabel lblCertLevel;
	private javax.swing.JLabel lblInPdfFile;
	private javax.swing.JLabel lblKeyPwd;
	private javax.swing.JLabel lblKeystoreFile;
	private javax.swing.JLabel lblKeystorePwd;
	private javax.swing.JLabel lblKeystoreType;
	private javax.swing.JLabel lblLocation;
	private javax.swing.JLabel lblOutPdfFile;
	private javax.swing.JLabel lblPdfOwnerPwd;
	private javax.swing.JLabel lblPdfUserPwd;
	private javax.swing.JLabel lblReason;
	private javax.swing.JPasswordField pfKeyPwd;
	private javax.swing.JPasswordField pfKeystorePwd;
	private javax.swing.JPasswordField pfPdfOwnerPwd;
	private javax.swing.JPasswordField pfPdfUserPwd;
	private javax.swing.JTextField tfInPdfFile;
	private javax.swing.JTextField tfKeystoreFile;
	private javax.swing.JTextField tfLocation;
	private javax.swing.JTextField tfOutPdfFile;
	private javax.swing.JTextField tfReason;
	// End of variables declaration//GEN-END:variables


}

/**
 * OutputStream wrapper for writing to TextArea component
 * @author Josef Cacek
 */
class TextAreaStream extends OutputStream {
	protected final JTextArea textArea;
	protected final ByteArrayOutputStream baos;

	public TextAreaStream(JTextArea textArea) {
		this.textArea = textArea;
		this.baos = new ByteArrayOutputStream();
	}

	public void write(int c) {
		synchronized (this) {
			this.baos.write((char) c);
			this.update();
		}
	}

	public void write(byte[] bytes, int offset, int length) {
		synchronized (this) {
			this.baos.write(bytes, offset, length);
			this.update();
		}
	}

	private void update() {
		String text = new String(this.baos.toByteArray());
		this.textArea.setText(text);
		this.textArea.setCaretPosition(text.length());
	}

	public void clear() {
		synchronized (this) {
			baos.reset();
			update();
		}
	}
}

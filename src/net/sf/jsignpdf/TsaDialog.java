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
package net.sf.jsignpdf;

import static net.sf.jsignpdf.Constants.RES;

import java.net.Proxy;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;

import net.sf.jsignpdf.types.ServerAuthentication;
import net.sf.jsignpdf.utils.ResourceProvider;

/**
 * JDialog for setting timestamp authority and enabling OCSP.
 * 
 * @author Josef Cacek
 */
public class TsaDialog extends javax.swing.JDialog {

	private static final long serialVersionUID = 1L;

	private BasicSignerOptions options;

	/** Creates new form TsaDialog */
	public TsaDialog(java.awt.Frame parent, boolean modal, BasicSignerOptions anOpts) {
		super(parent, modal);
		options = anOpts;
		initComponents();
		getRootPane().setDefaultButton(btnTsaOK);
		cbProxyType.setModel(new DefaultComboBoxModel(Proxy.Type.values()));
		cbTsaAuthn.setModel(new DefaultComboBoxModel(ServerAuthentication.values()));
		translateLabels();
	}

	/**
	 * Translates labels in this dialog.
	 */
	private void translateLabels() {
		setTitle(RES.get("gui.tsa.title"));

		setLabelAndMnemonic(chkbTsaEnabled, "gui.tsa.enabled.checkbox");

		setLabelAndMnemonic(lblTsaUrl, "gui.tsa.url.label");
		setLabelAndMnemonic(lblTsaUser, "gui.tsa.user.label");
		setLabelAndMnemonic(lblTsaAuthn, "gui.tsa.authn.label");
		setLabelAndMnemonic(lblTsaPwd, "gui.tsa.pwd.label");
		setLabelAndMnemonic(lblTsaPolicy, "gui.tsa.policy.label");
		setLabelAndMnemonic(lblTsaHashAlg, "gui.tsa.hashAlg.label");

		setLabelAndMnemonic(chkbOcspEnabled, "gui.tsa.ocspEnabled.checkbox");
		setLabelAndMnemonic(lblOcspUrl, "gui.tsa.ocspServerUrl.label");
		setLabelAndMnemonic(chkbCrlEnabled, "gui.tsa.crlEnabled.checkbox");

		setLabelAndMnemonic(pnlProxy, "gui.tsa.proxy.panel");
		setLabelAndMnemonic(lblProxyType, "gui.tsa.proxyType.label");
		setLabelAndMnemonic(lblProxyHost, "gui.tsa.proxyHost.label");
	}

	/**
	 * Handles visibility of components depending on options settings .
	 */
	private void refreshView() {
		boolean authnVisible = cbTsaAuthn.getSelectedItem() != ServerAuthentication.NONE;
		lblTsaUser.setVisible(authnVisible);
		lblTsaPwd.setVisible(authnVisible);
		tfTsaUser.setVisible(authnVisible);
		pfTsaPwd.setVisible(authnVisible);

		if (cbTsaAuthn.getSelectedItem() == ServerAuthentication.PASSWORD) {
			setLabelAndMnemonic(lblTsaUser, "gui.tsa.user.label");
			setLabelAndMnemonic(lblTsaPwd, "gui.tsa.pwd.label");
			tfTsaUser.setText(options.getTsaUser());
			pfTsaPwd.setText(options.getTsaPasswd());
		} else if (cbTsaAuthn.getSelectedItem() == ServerAuthentication.CERTIFICATE) {
			setLabelAndMnemonic(lblTsaUser, "gui.tsa.certFile.label");
			setLabelAndMnemonic(lblTsaPwd, "gui.tsa.certFilePwd.label");
			tfTsaUser.setText(options.getTsaCertFile());
			pfTsaPwd.setText(options.getTsaCertFilePwd());
		} else {
			tfTsaUser.setText(null);
			pfTsaPwd.setText(null);
		}

		boolean proxyDetailsVisible = cbProxyType.getSelectedItem() != Proxy.Type.DIRECT;
		lblProxyHost.setVisible(proxyDetailsVisible);
		tfProxyHost.setVisible(proxyDetailsVisible);
		spProxyPort.setVisible(proxyDetailsVisible);
	}

	/**
	 * Loads properties saved by previous run of application
	 */
	private void updateFromOptions() {
		chkbTsaEnabled.setSelected(options.isTimestamp());
		tfTsaUrl.setText(options.getTsaUrl());
		cbTsaAuthn.setSelectedItem(options.getTsaServerAuthn());
		pfTsaPwd.setText(options.getTsaPasswd());
		tfTsaPolicy.setText(options.getTsaPolicy());
		tfTsaHashAlg.setText(options.getTsaHashAlg());
		chkbOcspEnabled.setSelected(options.isOcspEnabled());
		tfOcspUrl.setText(options.getOcspServerUrl());
		chkbCrlEnabled.setSelected(options.isCrlEnabled());
		cbProxyType.setSelectedItem(options.getProxyType());
		tfProxyHost.setText(options.getProxyHost());
		spProxyPort.setValue(Integer.valueOf(options.getProxyPort()));
	}

	/**
	 * Enabling text field
	 */
	private void updateEnabledStatus() {
		final boolean tmpTsaEnabled = chkbTsaEnabled.isSelected();
		tfTsaUrl.setEnabled(tmpTsaEnabled);
		cbTsaAuthn.setEnabled(tmpTsaEnabled);
		tfTsaUser.setEnabled(tmpTsaEnabled);
		pfTsaPwd.setEnabled(tmpTsaEnabled);
		tfTsaPolicy.setEnabled(tmpTsaEnabled);
		tfTsaHashAlg.setEnabled(tmpTsaEnabled);

		tfOcspUrl.setEnabled(chkbOcspEnabled.isSelected());
	}

	/**
	 * stores values from this Form to the instance of {@link SignerOptions}
	 */
	private void storeToOptions() {
		options.setTimestamp(chkbTsaEnabled.isSelected());
		options.setTsaUrl(tfTsaUrl.getText());
		options.setTsaServerAuthn((ServerAuthentication) cbTsaAuthn.getSelectedItem());
		if (cbTsaAuthn.getSelectedItem() == ServerAuthentication.CERTIFICATE) {
			options.setTsaCertFile(tfTsaUser.getText());
			options.setTsaCertFilePwd(new String(pfTsaPwd.getPassword()));
		} else {
			options.setTsaUser(tfTsaUser.getText());
			options.setTsaPasswd(new String(pfTsaPwd.getPassword()));
		}
		options.setTsaPolicy(tfTsaPolicy.getText());
		options.setTsaHashAlg(tfTsaHashAlg.getText());
		options.setOcspEnabled(chkbOcspEnabled.isSelected());
		options.setOcspServerUrl(tfOcspUrl.getText());
		options.setCrlEnabled(chkbCrlEnabled.isSelected());

		options.setProxyType((Proxy.Type) cbProxyType.getSelectedItem());
		options.setProxyHost(tfProxyHost.getText());
		options.setProxyPort(((Integer) spProxyPort.getValue()).intValue());
		// if there are fixed values update them in the form;
		updateFromOptions();
	}

	/**
	 * Facade for
	 * {@link ResourceProvider#setLabelAndMnemonic(JComponent, String)}
	 * 
	 * @param aComponent
	 * @param aKey
	 */
	private void setLabelAndMnemonic(final JComponent aComponent, final String aKey) {
		RES.setLabelAndMnemonic(aComponent, aKey);
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        chkbTsaEnabled = new javax.swing.JCheckBox();
        lblTsaUrl = new javax.swing.JLabel();
        tfTsaUrl = new javax.swing.JTextField();
        lblTsaUser = new javax.swing.JLabel();
        lblTsaPwd = new javax.swing.JLabel();
        tfTsaUser = new javax.swing.JTextField();
        pfTsaPwd = new javax.swing.JPasswordField();
        chkbOcspEnabled = new javax.swing.JCheckBox();
        btnTsaOK = new javax.swing.JButton();
        chkbCrlEnabled = new javax.swing.JCheckBox();
        pnlProxy = new javax.swing.JPanel();
        lblProxyHost = new javax.swing.JLabel();
        tfProxyHost = new javax.swing.JTextField();
        lblProxyType = new javax.swing.JLabel();
        cbProxyType = new javax.swing.JComboBox();
        spProxyPort = new javax.swing.JSpinner();
        lblTsaPolicy = new javax.swing.JLabel();
        tfTsaPolicy = new javax.swing.JTextField();
        lblOcspUrl = new javax.swing.JLabel();
        tfOcspUrl = new javax.swing.JTextField();
        cbTsaAuthn = new javax.swing.JComboBox();
        lblTsaAuthn = new javax.swing.JLabel();
        lblTsaHashAlg = new javax.swing.JLabel();
        tfTsaHashAlg = new javax.swing.JTextField();

        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                formComponentHidden(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        chkbTsaEnabled.setText("Use timestamp server");
        chkbTsaEnabled.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        chkbTsaEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkbTsaEnabledActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(chkbTsaEnabled, gridBagConstraints);

        lblTsaUrl.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTsaUrl.setLabelFor(tfTsaUrl);
        lblTsaUrl.setText("TSA URL");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(lblTsaUrl, gridBagConstraints);

        tfTsaUrl.setMinimumSize(new java.awt.Dimension(200, 20));
        tfTsaUrl.setPreferredSize(new java.awt.Dimension(200, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 4.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(tfTsaUrl, gridBagConstraints);

        lblTsaUser.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTsaUser.setLabelFor(tfTsaUser);
        lblTsaUser.setText("TSA user");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(lblTsaUser, gridBagConstraints);

        lblTsaPwd.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTsaPwd.setLabelFor(pfTsaPwd);
        lblTsaPwd.setText("TSA password");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(lblTsaPwd, gridBagConstraints);

        tfTsaUser.setMinimumSize(new java.awt.Dimension(70, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(tfTsaUser, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(pfTsaPwd, gridBagConstraints);

        chkbOcspEnabled.setText("Use OCSP");
        chkbOcspEnabled.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        chkbOcspEnabled.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkbTsaEnabledActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(chkbOcspEnabled, gridBagConstraints);

        btnTsaOK.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/back16.png"))); // NOI18N
        btnTsaOK.setText("OK");
        btnTsaOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTsaOKActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(7, 2, 2, 5);
        getContentPane().add(btnTsaOK, gridBagConstraints);

        chkbCrlEnabled.setText("Use CRL");
        chkbCrlEnabled.setMaximumSize(new java.awt.Dimension(2147483647, 2147483647));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(chkbCrlEnabled, gridBagConstraints);

        pnlProxy.setBorder(javax.swing.BorderFactory.createTitledBorder("Proxy"));
        pnlProxy.setLayout(new java.awt.GridBagLayout());

        lblProxyHost.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblProxyHost.setLabelFor(tfProxyHost);
        lblProxyHost.setText("Proxy host/port");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 5);
        pnlProxy.add(lblProxyHost, gridBagConstraints);

        tfProxyHost.setMinimumSize(new java.awt.Dimension(200, 20));
        tfProxyHost.setPreferredSize(new java.awt.Dimension(200, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 4.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 2);
        pnlProxy.add(tfProxyHost, gridBagConstraints);

        lblProxyType.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblProxyType.setLabelFor(cbProxyType);
        lblProxyType.setText("Proxy type");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 5);
        pnlProxy.add(lblProxyType, gridBagConstraints);

        cbProxyType.setMinimumSize(new java.awt.Dimension(150, 20));
        cbProxyType.setPreferredSize(new java.awt.Dimension(150, 20));
        cbProxyType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbProxyTypeActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 2);
        pnlProxy.add(cbProxyType, gridBagConstraints);

        spProxyPort.setModel(new javax.swing.SpinnerNumberModel(80, 0, 65535, 1));
        spProxyPort.setMinimumSize(new java.awt.Dimension(50, 18));
        spProxyPort.setPreferredSize(new java.awt.Dimension(50, 18));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        pnlProxy.add(spProxyPort, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(pnlProxy, gridBagConstraints);

        lblTsaPolicy.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTsaPolicy.setLabelFor(tfTsaPolicy);
        lblTsaPolicy.setText("TSA policy OID");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(lblTsaPolicy, gridBagConstraints);

        tfTsaPolicy.setMinimumSize(new java.awt.Dimension(70, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(tfTsaPolicy, gridBagConstraints);

        lblOcspUrl.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblOcspUrl.setLabelFor(tfOcspUrl);
        lblOcspUrl.setText("External OCSP URL");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(lblOcspUrl, gridBagConstraints);

        tfOcspUrl.setMinimumSize(new java.awt.Dimension(200, 20));
        tfOcspUrl.setPreferredSize(new java.awt.Dimension(200, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 4.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(tfOcspUrl, gridBagConstraints);

        cbTsaAuthn.setMinimumSize(new java.awt.Dimension(150, 20));
        cbTsaAuthn.setPreferredSize(new java.awt.Dimension(150, 20));
        cbTsaAuthn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbTsaAuthnActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(cbTsaAuthn, gridBagConstraints);

        lblTsaAuthn.setLabelFor(cbTsaAuthn);
        lblTsaAuthn.setText("TSA authentication");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(lblTsaAuthn, gridBagConstraints);

        lblTsaHashAlg.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTsaHashAlg.setLabelFor(tfTsaHashAlg);
        lblTsaHashAlg.setText("TSA hash algorithm");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(lblTsaHashAlg, gridBagConstraints);

        tfTsaHashAlg.setMinimumSize(new java.awt.Dimension(70, 20));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
        getContentPane().add(tfTsaHashAlg, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

	private void cbTsaAuthnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbTsaAuthnActionPerformed
		refreshView();
		pack();
	}//GEN-LAST:event_cbTsaAuthnActionPerformed

	private void cbProxyTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbProxyTypeActionPerformed
		refreshView();
		pack();
	}//GEN-LAST:event_cbProxyTypeActionPerformed

	private void btnTsaOKActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnTsaOKActionPerformed
		setVisible(false);
	}// GEN-LAST:event_btnTsaOKActionPerformed

	private void chkbTsaEnabledActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_chkbTsaEnabledActionPerformed
		updateEnabledStatus();
	}// GEN-LAST:event_chkbTsaEnabledActionPerformed

	private void formComponentShown(java.awt.event.ComponentEvent evt) {// GEN-FIRST:event_formComponentShown
		updateFromOptions();
		updateEnabledStatus();
		refreshView();
		pack();
	}// GEN-LAST:event_formComponentShown

	private void formComponentHidden(java.awt.event.ComponentEvent evt) {// GEN-FIRST:event_formComponentHidden
		storeToOptions();
	}// GEN-LAST:event_formComponentHidden

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnTsaOK;
    private javax.swing.JComboBox cbProxyType;
    private javax.swing.JComboBox cbTsaAuthn;
    private javax.swing.JCheckBox chkbCrlEnabled;
    private javax.swing.JCheckBox chkbOcspEnabled;
    private javax.swing.JCheckBox chkbTsaEnabled;
    private javax.swing.JLabel lblOcspUrl;
    private javax.swing.JLabel lblProxyHost;
    private javax.swing.JLabel lblProxyType;
    private javax.swing.JLabel lblTsaAuthn;
    private javax.swing.JLabel lblTsaHashAlg;
    private javax.swing.JLabel lblTsaPolicy;
    private javax.swing.JLabel lblTsaPwd;
    private javax.swing.JLabel lblTsaUrl;
    private javax.swing.JLabel lblTsaUser;
    private javax.swing.JPasswordField pfTsaPwd;
    private javax.swing.JPanel pnlProxy;
    private javax.swing.JSpinner spProxyPort;
    private javax.swing.JTextField tfOcspUrl;
    private javax.swing.JTextField tfProxyHost;
    private javax.swing.JTextField tfTsaHashAlg;
    private javax.swing.JTextField tfTsaPolicy;
    private javax.swing.JTextField tfTsaUrl;
    private javax.swing.JTextField tfTsaUser;
    // End of variables declaration//GEN-END:variables

}

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
package net.sf.jsignpdf.ui;

import static net.sf.jsignpdf.Constants.RES;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ResourceBundle;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;

import net.sf.jsignpdf.BasicSignerOptions;
import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.SignerFileChooser;
import net.sf.jsignpdf.preview.Pdf2Image;
import net.sf.jsignpdf.preview.SelectionImage;
import net.sf.jsignpdf.utils.ResourceProvider;

public class MainWindow {

    private BasicSignerOptions options = new BasicSignerOptions();

    private SelectionImage previewImage;
    private JPanel settingsPane;
    private int settingsRow;

    private SignerFileChooser fc = new SignerFileChooser();

    private javax.swing.JComboBox cbAlias;
    private javax.swing.JComboBox cbCertLevel;
    private javax.swing.JComboBox cbHashAlgorithm;
    private javax.swing.JComboBox cbKeystoreType;
    private javax.swing.JComboBox cbPdfEncryption;
    private javax.swing.JComboBox cbPrinting;
    private javax.swing.JCheckBox chkbAdvanced;
    private javax.swing.JCheckBox chkbAllowAssembly;
    private javax.swing.JCheckBox chkbAllowCopy;
    private javax.swing.JCheckBox chkbAllowFillIn;
    private javax.swing.JCheckBox chkbAllowModifyAnnotations;
    private javax.swing.JCheckBox chkbAllowModifyContent;
    private javax.swing.JCheckBox chkbAllowScreenReaders;
    private javax.swing.JCheckBox chkbAppendSignature;
    private javax.swing.JCheckBox chkbStorePwd;
    private javax.swing.JCheckBox chkbVisibleSig;
    private javax.swing.JFrame infoDialog;
    private javax.swing.JScrollPane infoScrollPane;
    private javax.swing.JTextArea infoTextArea;
    private javax.swing.JLabel lblAlias;
    private javax.swing.JLabel lblCertLevel;
    private javax.swing.JLabel lblContact;
    private javax.swing.JLabel lblEncCertFile;
    private javax.swing.JLabel lblHashAlgorithm;
    private javax.swing.JLabel lblInPdfFile;
    private javax.swing.JLabel lblKeyPwd;
    private javax.swing.JLabel lblKeystoreFile;
    private javax.swing.JLabel lblKeystorePwd;
    private javax.swing.JLabel lblKeystoreType;
    private javax.swing.JLabel lblLocation;
    private javax.swing.JLabel lblOutPdfFile;
    private javax.swing.JLabel lblPdfEncryption;
    private javax.swing.JLabel lblPdfOwnerPwd;
    private javax.swing.JLabel lblPdfUserPwd;
    private javax.swing.JLabel lblPrinting;
    private javax.swing.JLabel lblReason;
    private javax.swing.JLabel lblRights;
    private javax.swing.JPasswordField pfKeyPwd;
    private javax.swing.JPasswordField pfKeystorePwd;
    private javax.swing.JPasswordField pfPdfOwnerPwd;
    private javax.swing.JPasswordField pfPdfUserPwd;
    private javax.swing.JDialog rightsDialog;
    private javax.swing.JTextField tfContact;
    private javax.swing.JTextField tfEncCertFile;
    private javax.swing.JTextField tfInPdfFile;
    private javax.swing.JTextField tfKeystoreFile;
    private javax.swing.JTextField tfLocation;
    private javax.swing.JTextField tfOutPdfFile;
    private javax.swing.JTextField tfReason;

    private javax.swing.JTextField tfPage;
    private javax.swing.JTextField tfPosLLX;
    private javax.swing.JTextField tfPosLLY;
    private javax.swing.JTextField tfPosURX;
    private javax.swing.JTextField tfPosURY;

    public JMenuBar createMenuBar() {
        JMenuBar menuBar;
        JMenu menu;
        JMenuItem menuItem;

        // Create the menu bar.
        menuBar = new JMenuBar();

        // Build the first menu.
        menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        // menu.getAccessibleContext().setAccessibleDescription(
        // "The only menu in this program that has menu items");
        menuBar.add(menu);

        menuItem = new JMenuItem("Open PDF");
        menuItem.setMnemonic(KeyEvent.VK_O);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(
                e -> fc.showFileChooser(SignerFileChooser.FILEFILTER_PDF, JFileChooser.OPEN_DIALOG, file -> openFile(file)));
        menu.add(menuItem);

        menuItem = new JMenuItem("Load certificate", KeyEvent.VK_C);
        menuItem.setMnemonic(KeyEvent.VK_C);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
//        menuItem.addActionListener(e->new);
        menu.add(menuItem);

        menuItem = new JMenuItem("Generate test keystore", KeyEvent.VK_G);
        menu.add(menuItem);

        menuBar.add(menu);

        return menuBar;
    }

    /**
     * Create the GUI and show it. For thread safety, this method should be invoked from the event-dispatching thread.
     */
    private static void createAndShowGUI() {
        JFrame frame = new JFrame(RES.get("gui.title", new String[] { Constants.VERSION }));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create and set up the content pane.
        MainWindow mainWindow = new MainWindow();
        frame.setJMenuBar(mainWindow.createMenuBar());
        frame.setContentPane(mainWindow.createContentPane());

        // Display the window.
        frame.setSize(450, 260);
        frame.setVisible(true);
    }

    private Container createContentPane() {
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setOpaque(true);

        JPanel previewPanel = createPreviewPanel();

        settingsPane = new JPanel(new GridBagLayout());
        /*
         * setLabelAndMnemonic(lblKeystoreType, "gui.keystoreType.label"); setLabelAndMnemonic(chkbAdvanced,
         * "gui.advancedView.checkbox"); setLabelAndMnemonic(lblKeystoreFile, "gui.keystoreFile.label");
         * setLabelAndMnemonic(lblKeystorePwd, "gui.keystorePassword.label"); setLabelAndMnemonic(chkbStorePwd,
         * "gui.storePasswords.checkbox"); setLabelAndMnemonic(lblAlias, "gui.alias.label"); setLabelAndMnemonic(btnLoadAliases,
         * "gui.loadAliases.button"); setLabelAndMnemonic(lblKeyPwd, "gui.keyPassword.label"); setLabelAndMnemonic(lblInPdfFile,
         * "gui.inPdfFile.label"); setLabelAndMnemonic(btnRights, "gui.rights.button"); setLabelAndMnemonic(lblPdfEncryption,
         * "gui.pdfEncryption.label"); setLabelAndMnemonic(lblPdfOwnerPwd, "gui.pdfOwnerPwd.label");
         * setLabelAndMnemonic(lblPdfUserPwd, "gui.pdfUserPwd.label"); setLabelAndMnemonic(lblEncCertFile,
         * "gui.encryptionCertFile.label"); setLabelAndMnemonic(lblOutPdfFile, "gui.outPdfFile.label");
         * setLabelAndMnemonic(lblReason, "gui.reason.label"); setLabelAndMnemonic(lblLocation, "gui.location.label");
         * setLabelAndMnemonic(lblContact, "gui.contact.label"); setLabelAndMnemonic(lblCertLevel, "gui.certLevel.label");
         * setLabelAndMnemonic(chkbAppendSignature, "gui.appendSignature.checkbox"); setLabelAndMnemonic(lblHashAlgorithm,
         * "gui.hashAlgorithm.label");
         *
         * btnKeystoreFile.setText(RES.get("gui.browse.button")); btnEncCertFile.setText(RES.get("gui.browse.button"));
         * btnInPdfFile.setText(RES.get("gui.browse.button")); btnOutPdfFile.setText(RES.get("gui.browse.button"));
         *
         * setLabelAndMnemonic(btnSignIt, "gui.signIt.button");
         *
         * infoDialog.setTitle(RES.get("gui.info.title")); btnInfoClose.setText(RES.get("gui.info.close.button"));
         *
         * rightsDialog.setTitle(RES.get("gui.rights.title")); setLabelAndMnemonic(lblPrinting, "gui.rights.printing.label");
         * setLabelAndMnemonic(lblRights, "gui.rights.rights.label"); setLabelAndMnemonic(chkbAllowCopy,
         * "gui.rights.copy.checkbox"); setLabelAndMnemonic(chkbAllowAssembly, "gui.rights.assembly.checkbox");
         * setLabelAndMnemonic(chkbAllowFillIn, "gui.rights.fillIn.checkbox"); setLabelAndMnemonic(chkbAllowScreenReaders,
         * "gui.rights.screenReaders.checkbox"); setLabelAndMnemonic(chkbAllowModifyAnnotations,
         * "gui.rights.modifyAnnotations.checkbox"); setLabelAndMnemonic(chkbAllowModifyContent,
         * "gui.rights.modifyContents.checkbox");
         *
         * setLabelAndMnemonic(chkbVisibleSig, "gui.visibleSignature.checkbox"); setLabelAndMnemonic(btnVisibleSigSettings,
         * "gui.visibleSignatureSettings.button");
         *
         * setLabelAndMnemonic(btnTsaOcsp, "gui.tsaOcsp.button");
         *
         */
        tfInPdfFile = addRowToSettingsPane(new JTextField(), "gui.inPdfFile.label");

        /*
        setLabelAndMnemonic(lblPage, "gui.vs.page.label");
        setLabelAndMnemonic(lblPosLLX, "gui.vs.llx.label");
        setLabelAndMnemonic(lblPosLLY, "gui.vs.lly.label");
        setLabelAndMnemonic(lblPosURX, "gui.vs.urx.label");
        setLabelAndMnemonic(lblPosURY, "gui.vs.ury.label");
         */
        tfPage = addRowToSettingsPane(new JTextField(), "gui.vs.page.label");
        tfPosLLX = addRowToSettingsPane(new JTextField(), "gui.vs.llx.label");
        tfPosLLY = addRowToSettingsPane(new JTextField(), "gui.vs.lly.label");
        tfPosURX = addRowToSettingsPane(new JTextField(), "gui.vs.urx.label");
        tfPosURY = addRowToSettingsPane(new JTextField(), "gui.vs.ury.label");

        JPanel jPanelStart = new JPanel(new BorderLayout());
        jPanelStart.add(settingsPane, BorderLayout.PAGE_START);
        JScrollPane settingsScrollPane = new JScrollPane(jPanelStart);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, settingsScrollPane, previewPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(250);

        // Provide minimum sizes for the two components in the split pane
        Dimension minimumSize = new Dimension(100, 50);
        settingsScrollPane.setMinimumSize(minimumSize);
        contentPane.setMinimumSize(minimumSize);

        contentPane.add(splitPane, BorderLayout.CENTER);
        return contentPane;
    }

    private JPanel createPreviewPanel() {
        JPanel previewPanel = new JPanel(new BorderLayout());
        previewImage = new SelectionImage();
        previewImage.setPreferredSize(new Dimension(400, 400));
        previewPanel.add(previewImage, BorderLayout.CENTER);

        JButton btnSignIt = new JButton(new ImageIcon(getClass().getResource("/net/sf/jsignpdf/signedpdf26.png")));
        RES.setLabelAndMnemonic(btnSignIt, "gui.signIt.button");

        JPanel pageEndPanel = new JPanel();
//        pageEndPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        pageEndPanel.setLayout(new BoxLayout(pageEndPanel, BoxLayout.PAGE_AXIS));

        JPanel pdfNavigationPanel = new JPanel();
//        pdfNavigationPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        pdfNavigationPanel.setLayout(new FlowLayout());
        JButton btnPrevious = new JButton();
        btnPrevious.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/prev16.png"))); // NOI18N
        btnPrevious.setMinimumSize(new java.awt.Dimension(50, 20));
        pdfNavigationPanel.add(btnPrevious);
        JSpinner jspPdfPage = new JSpinner();
        JLabel lblPdfPage = new JLabel();
        RES.setLabelAndMnemonic(lblPdfPage, "gui.vs.page.label");
        pdfNavigationPanel.add(lblPdfPage);
        jspPdfPage.setModel(new SpinnerNumberModel(1, 1, 10, 1));
        pdfNavigationPanel.add(jspPdfPage);
        JButton btnNext = new JButton();
        btnNext.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/next16.png"))); // NOI18N
        btnNext.setMinimumSize(new java.awt.Dimension(50, 20));
        pdfNavigationPanel.add(btnNext);

        JPanel signItPanel = new JPanel();
//        signItPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//        jPanel1.setMinimumSize(new Dimension(0, 0));
        signItPanel.setPreferredSize(new Dimension(400, btnSignIt.getPreferredSize().height+2*12));
        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(signItPanel);
        signItPanel.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(246, Short.MAX_VALUE)
                .addComponent(btnSignIt)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(btnSignIt))
        );

        pageEndPanel.add(pdfNavigationPanel);
        pageEndPanel.add(signItPanel);

        previewPanel.add(pageEndPanel, BorderLayout.PAGE_END);
        return previewPanel;
    }

    private JTextField addRowToSettingsPane(JTextField component, String labelStr) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = settingsRow;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        JLabel label = new JLabel();
        RES.setLabelAndMnemonic(label, labelStr);
        settingsPane.add(label, labelConstraints);

        GridBagConstraints componentConstraints = new java.awt.GridBagConstraints();
        componentConstraints.gridx = 1;
        componentConstraints.gridy = settingsRow;
        componentConstraints.fill = GridBagConstraints.HORIZONTAL;
        componentConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        componentConstraints.weightx = 1.0;
        componentConstraints.weighty = 1.0;

        settingsRow++;

        component.setMinimumSize(new java.awt.Dimension(150, 20));
        component.setPreferredSize(new java.awt.Dimension(150, 20));
        settingsPane.add(component, componentConstraints);
        return component;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createAndShowGUI();
            }
        });
    }

    void openFile(File file) {
        options.setInFile(file.getAbsolutePath());
        BufferedImage img = new Pdf2Image(options).getImageForPage(1);
        previewImage.setImage(img);
    }
}

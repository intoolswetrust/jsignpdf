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

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.sf.jsignpdf.preview.Pdf2Image;
import net.sf.jsignpdf.preview.SelectionImage;
import net.sf.jsignpdf.types.FloatPoint;
import net.sf.jsignpdf.types.RelRect;
import net.sf.jsignpdf.types.RenderMode;
import net.sf.jsignpdf.utils.ConvertUtils;
import net.sf.jsignpdf.utils.GuiUtils;
import net.sf.jsignpdf.utils.ResourceProvider;

import org.apache.commons.lang3.StringUtils;

/**
 * Options dialog for Visible signature settings
 * 
 * @author Josef Cacek
 */
public class VisibleSignatureDialog extends javax.swing.JDialog {

  private static final long serialVersionUID = 1L;

  private BasicSignerOptions options;
  private SignerFileChooser fc;
  private PdfExtraInfo extraInfo;
  private Pdf2Image p2i;
  private SelectionImage selectionImage = new SelectionImage();

  private int numberOfPages = -1;
  private FloatPoint pdfPageSize;

  private boolean previewListenerDisabled;

  /**
   * Document listener which catches page number change.
   * 
   * @author Josef Cacek
   */
  class PageNrDocumentListener implements DocumentListener {
    public void changedUpdate(DocumentEvent e) {
      pageNrChanged();
    }

    public void insertUpdate(DocumentEvent e) {
      pageNrChanged();
    }

    public void removeUpdate(DocumentEvent e) {
      pageNrChanged();
    }
  }

  /** Creates new form VisibleSignatureDialog */
  public VisibleSignatureDialog(java.awt.Frame parent, boolean modal, final BasicSignerOptions anOptions,
      final SignerFileChooser aFC) {
    super(parent, modal);
    options = anOptions;
    fc = aFC;
    p2i = new Pdf2Image(options);
    initComponents();
    translateLabels();
    tfPage.getDocument().addDocumentListener(new PageNrDocumentListener());
    selectionImage.getRelRect().addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        if (previewListenerDisabled)
          return;
        final RelRect tmpRect = selectionImage.getRelRect();
        if (pdfPageSize == null || !tmpRect.isValid()) {
          // tfPosLLX.setText(null);
          // tfPosLLY.setText(null);
          // tfPosURX.setText(null);
          // tfPosURY.setText(null);
          return;
        }
        tfPosLLX.setText(String.valueOf(tmpRect.getRelLeft() * pdfPageSize.getX()));
        tfPosLLY.setText(String.valueOf((1 - tmpRect.getRelBottom()) * pdfPageSize.getY()));
        tfPosURX.setText(String.valueOf(tmpRect.getRelRight() * pdfPageSize.getX()));
        tfPosURY.setText(String.valueOf((1 - tmpRect.getRelTop()) * pdfPageSize.getY()));
      }
    });
    cbDisplayMode.setModel(new DefaultComboBoxModel(RenderMode.values()));
    extraInfo = new PdfExtraInfo(anOptions);
    previewDialog.add(selectionImage, java.awt.BorderLayout.CENTER);
    previewDialog.setModal(true);
    previewDialog.getRootPane().setDefaultButton(btnPreviewClose);
    GuiUtils.resizeAndCenter(previewDialog);
    getRootPane().setDefaultButton(btnClose);
  }

  /**
   * Event handler for page number change. It displays (or hides) position
   * bounds.
   */
  protected void pageNrChanged() {
    if (numberOfPages < 1)
      return;

    final Integer tmpPageNr = ConvertUtils.toInteger(tfPage.getText());
    if (tmpPageNr != null && tmpPageNr > 0 && tmpPageNr <= numberOfPages) {
      pdfPageSize = extraInfo.getPageSize(tmpPageNr.intValue());
    }
    if (switchBounds(pdfPageSize != null)) {
      lblPosLLYBounds.setText("0.0 - " + pdfPageSize.getY());
      lblPosLLXBounds.setText("0.0 - " + pdfPageSize.getX());
    }
  }

  /**
   * Translates labels in this dialog.
   */
  private void translateLabels() {
    setTitle(RES.get("gui.vs.title"));

    setLabelAndMnemonic(lblPosition, "gui.vs.position.label");
    setLabelAndMnemonic(lblPage, "gui.vs.page.label");
    setLabelAndMnemonic(lblPosLLX, "gui.vs.llx.label");
    setLabelAndMnemonic(lblPosLLY, "gui.vs.lly.label");
    setLabelAndMnemonic(lblPosURX, "gui.vs.urx.label");
    setLabelAndMnemonic(lblPosURY, "gui.vs.ury.label");
    setLabelAndMnemonic(lblBgImgScale, "gui.vs.bgImgScale.label");
    setLabelAndMnemonic(btnPreview, "gui.vs.preview.button");

    setLabelAndMnemonic(lblSettings, "gui.vs.settings.label");
    setLabelAndMnemonic(chkbAcro6Layers, "gui.vs.acro6layers.checkbox");
    setLabelAndMnemonic(lblDisplayMode, "gui.vs.renderMode.label");
    setLabelAndMnemonic(lblL2Text, "gui.vs.l2Text.label");
    setLabelAndMnemonic(lblL2TextFontSize, "gui.vs.l2TextFontSize.label");
    setLabelAndMnemonic(lblL4Text, "gui.vs.l4Text.label");
    setLabelAndMnemonic(lblImgPath, "gui.vs.imgPath.label");
    setLabelAndMnemonic(lblBgImgPath, "gui.vs.bgImgPath.label");
    setLabelAndMnemonic(chkbL2TextDefault, "gui.vs.default.checkbox");
    setLabelAndMnemonic(chkbL4TextDefault, "gui.vs.default.checkbox");
    setLabelAndMnemonic(btnBgImgPathBrowse, "gui.vs.browse.button");
    setLabelAndMnemonic(btnImgPathBrowse, "gui.vs.browse.button");
    setLabelAndMnemonic(btnClose, "gui.vs.close.button");

    setLabelAndMnemonic(btnPreviewClose, "gui.vs.close.button");
    previewDialog.setTitle(RES.get("gui.preview.title"));

    setToolTip(tfPage, "gui.vs.page.tooltip");
    setToolTip(tfPosLLX, "gui.vs.llx.tooltip");
    setToolTip(tfPosLLY, "gui.vs.lly.tooltip");
    setToolTip(tfPosURX, "gui.vs.urx.tooltip");
    setToolTip(tfPosURY, "gui.vs.ury.tooltip");
    setToolTip(tfBgImgScale, "gui.vs.bgImgScale.tooltip");

  }

  /**
   * Loads properties saved by previous run of application
   */
  private void updateFromOptions() {
    tfPage.setText(ConvertUtils.toString(options.getPage()));
    tfPosLLX.setText(ConvertUtils.toString(options.getPositionLLX()));
    tfPosLLY.setText(ConvertUtils.toString(options.getPositionLLY()));
    tfPosURX.setText(ConvertUtils.toString(options.getPositionURX()));
    tfPosURY.setText(ConvertUtils.toString(options.getPositionURY()));
    tfBgImgScale.setText(ConvertUtils.toString(options.getBgImgScale()));
    cbDisplayMode.setSelectedItem(options.getRenderMode());
    taL2Text.setText(options.getL2Text());
    chkbL2TextDefault.setSelected(options.getL2Text() == null);
    tfL2TextFontSize.setText(ConvertUtils.toString(options.getL2TextFontSize()));
    tfL4Text.setText(options.getL4Text());
    chkbL4TextDefault.setSelected(options.getL4Text() == null);
    tfImgPath.setText(options.getImgPath());
    tfBgImgPath.setText(options.getBgImgPath());
    chkbAcro6Layers.setSelected(options.isAcro6Layers());

    // set description fields enabled/disabled
    chkbL2TextDefaultActionPerformed(null);
    chkbL4TextDefaultActionPerformed(null);
  }

  /**
   * stores values from this Form to the instance of {@link SignerOptions}
   */
  private void storeToOptions() {
    options.setPage(ConvertUtils.toInt(tfPage.getText(), Constants.DEFVAL_PAGE));
    options.setPositionLLX(ConvertUtils.toFloat(tfPosLLX.getText(), Constants.DEFVAL_LLX));
    options.setPositionLLY(ConvertUtils.toFloat(tfPosLLY.getText(), Constants.DEFVAL_LLY));
    options.setPositionURX(ConvertUtils.toFloat(tfPosURX.getText(), Constants.DEFVAL_URX));
    options.setPositionURY(ConvertUtils.toFloat(tfPosURY.getText(), Constants.DEFVAL_URY));
    options.setBgImgScale(ConvertUtils.toFloat(tfBgImgScale.getText(), Constants.DEFVAL_BG_SCALE));
    options.setRenderMode((RenderMode) cbDisplayMode.getSelectedItem());
    options.setL2Text(chkbL2TextDefault.isSelected() ? null : StringUtils.defaultString(taL2Text.getText(), ""));
    options.setL2TextFontSize(ConvertUtils.toFloat(tfL2TextFontSize.getText(), Constants.DEFVAL_L2_FONT_SIZE));
    options.setL4Text(chkbL4TextDefault.isSelected() ? null : StringUtils.defaultString(tfL4Text.getText(), ""));
    options.setImgPath(tfImgPath.getText());
    options.setBgImgPath(tfBgImgPath.getText());
    options.setAcro6Layers(chkbAcro6Layers.isSelected());

    // if there are fixed values update them in the form;
    updateFromOptions();
  }

  /**
   * Facade for {@link ResourceProvider#setLabelAndMnemonic(JComponent, String)}
   * 
   * @param aComponent
   * @param aKey
   */
  private void setLabelAndMnemonic(final JComponent aComponent, final String aKey) {
    RES.setLabelAndMnemonic(aComponent, aKey);
  }

  /**
   * Sets tooltip with given key to given component
   * 
   * @param aComponent
   *          component to which a tooltip should be assigned
   * @param aKey
   *          tooltip key (in resource bundle)
   */
  private void setToolTip(final JComponent aComponent, final String aKey) {
    aComponent.setToolTipText(RES.get(aKey));
  }

  /**
   * Shows/hides position bounds.
   * 
   * @param aVisible
   * @return
   */
  private boolean switchBounds(final boolean aVisible) {
    lblPosLLXBounds.setVisible(aVisible);
    lblPosLLYBounds.setVisible(aVisible);
    btnPreview.setEnabled(aVisible);
    return aVisible;
  }

  /**
   * Enables/disables siganture image input for selected render mode.
   */
  private void switchImage() {
    final boolean tmpEnabled = RenderMode.GRAPHIC_AND_DESCRIPTION.equals(cbDisplayMode.getSelectedItem());
    tfImgPath.setEnabled(tmpEnabled);
    btnImgPathBrowse.setEnabled(tmpEnabled);
  }

  /**
   * Reads number of pages from PDF.
   */
  private void readPdfInfo() {
    numberOfPages = extraInfo.getNumberOfPages();
    tfPage.setEnabled(numberOfPages != 1);
    lblPageBounds.setVisible(numberOfPages > 0);
    switchBounds(false);
    if (numberOfPages > 0) {
      lblPageBounds.setText("1 - " + numberOfPages);
      final Integer tmpPageNr = ConvertUtils.toInteger(tfPage.getText());
      if (tmpPageNr == null || tmpPageNr.intValue() < 1 || tmpPageNr.intValue() > numberOfPages) {
        tfPage.setText(ConvertUtils.toString(Constants.DEFVAL_PAGE));
      }
    }
    pageNrChanged();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    previewDialog = new javax.swing.JDialog();
    jPanel1 = new javax.swing.JPanel();
    btnPreviewClose = new javax.swing.JButton();
    btnPrevious = new javax.swing.JButton();
    btnNext = new javax.swing.JButton();
    lblPosition = new javax.swing.JLabel();
    lblPage = new javax.swing.JLabel();
    tfPage = new javax.swing.JTextField();
    lblPosLLX = new javax.swing.JLabel();
    tfPosLLX = new javax.swing.JTextField();
    lblPosLLY = new javax.swing.JLabel();
    tfPosLLY = new javax.swing.JTextField();
    lblPosURX = new javax.swing.JLabel();
    tfPosURX = new javax.swing.JTextField();
    lblPosURY = new javax.swing.JLabel();
    tfPosURY = new javax.swing.JTextField();
    lblSettings = new javax.swing.JLabel();
    lblDisplayMode = new javax.swing.JLabel();
    cbDisplayMode = new javax.swing.JComboBox();
    chkbL2TextDefault = new javax.swing.JCheckBox();
    lblL4Text = new javax.swing.JLabel();
    tfL4Text = new javax.swing.JTextField();
    chkbL4TextDefault = new javax.swing.JCheckBox();
    lblImgPath = new javax.swing.JLabel();
    tfImgPath = new javax.swing.JTextField();
    btnImgPathBrowse = new javax.swing.JButton();
    lblBgImgPath = new javax.swing.JLabel();
    tfBgImgPath = new javax.swing.JTextField();
    btnBgImgPathBrowse = new javax.swing.JButton();
    lblBgImgScale = new javax.swing.JLabel();
    tfBgImgScale = new javax.swing.JTextField();
    btnClose = new javax.swing.JButton();
    lblPageBounds = new javax.swing.JLabel();
    lblPosLLXBounds = new javax.swing.JLabel();
    lblPosLLYBounds = new javax.swing.JLabel();
    lblL2Text = new javax.swing.JLabel();
    lblL2TextFontSize = new javax.swing.JLabel();
    tfL2TextFontSize = new javax.swing.JTextField();
    btnPreview = new javax.swing.JButton();
    jScrollPane1 = new javax.swing.JScrollPane();
    taL2Text = new javax.swing.JTextArea();
    chkbAcro6Layers = new javax.swing.JCheckBox();

    previewDialog.setModal(true);

    jPanel1.setLayout(new java.awt.GridBagLayout());

    btnPreviewClose.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/back16.png"))); // NOI18N
    btnPreviewClose.setText("Close");
    btnPreviewClose.setMinimumSize(new java.awt.Dimension(50, 20));
    btnPreviewClose.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnPreviewCloseActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    jPanel1.add(btnPreviewClose, gridBagConstraints);

    btnPrevious.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/prev16.png"))); // NOI18N
    btnPrevious.setMinimumSize(new java.awt.Dimension(50, 20));
    btnPrevious.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnPreviousActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    jPanel1.add(btnPrevious, gridBagConstraints);

    btnNext.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/next16.png"))); // NOI18N
    btnNext.setMinimumSize(new java.awt.Dimension(50, 20));
    btnNext.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnNextActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
    jPanel1.add(btnNext, gridBagConstraints);

    previewDialog.getContentPane().add(jPanel1, java.awt.BorderLayout.PAGE_END);

    addComponentListener(new java.awt.event.ComponentAdapter() {
      public void componentHidden(java.awt.event.ComponentEvent evt) {
        formComponentHidden(evt);
      }

      public void componentShown(java.awt.event.ComponentEvent evt) {
        formComponentShown(evt);
      }
    });
    getContentPane().setLayout(new java.awt.GridBagLayout());

    lblPosition.setText("Position");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
    getContentPane().add(lblPosition, gridBagConstraints);

    lblPage.setLabelFor(lblPage);
    lblPage.setText("Page");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblPage, gridBagConstraints);

    tfPage.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    tfPage.setText("1");
    tfPage.setMinimumSize(new java.awt.Dimension(70, 20));
    tfPage.setPreferredSize(new java.awt.Dimension(70, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(tfPage, gridBagConstraints);

    lblPosLLX.setLabelFor(tfPosLLX);
    lblPosLLX.setText("Lower Left X");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblPosLLX, gridBagConstraints);

    tfPosLLX.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    tfPosLLX.setText("0.0");
    tfPosLLX.setMinimumSize(new java.awt.Dimension(70, 20));
    tfPosLLX.setPreferredSize(new java.awt.Dimension(70, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(tfPosLLX, gridBagConstraints);

    lblPosLLY.setLabelFor(tfPosLLY);
    lblPosLLY.setText("Lower Left Y");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblPosLLY, gridBagConstraints);

    tfPosLLY.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    tfPosLLY.setText("0.0");
    tfPosLLY.setMinimumSize(new java.awt.Dimension(70, 20));
    tfPosLLY.setPreferredSize(new java.awt.Dimension(70, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(tfPosLLY, gridBagConstraints);

    lblPosURX.setLabelFor(tfPosURX);
    lblPosURX.setText("Upper Right X");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblPosURX, gridBagConstraints);

    tfPosURX.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    tfPosURX.setText("100.0");
    tfPosURX.setMinimumSize(new java.awt.Dimension(70, 20));
    tfPosURX.setPreferredSize(new java.awt.Dimension(70, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(tfPosURX, gridBagConstraints);

    lblPosURY.setLabelFor(tfPosURY);
    lblPosURY.setText("Upper Right Y");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblPosURY, gridBagConstraints);

    tfPosURY.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    tfPosURY.setText("100.0");
    tfPosURY.setMinimumSize(new java.awt.Dimension(70, 20));
    tfPosURY.setPreferredSize(new java.awt.Dimension(70, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(tfPosURY, gridBagConstraints);

    lblSettings.setText("Settings");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
    getContentPane().add(lblSettings, gridBagConstraints);

    lblDisplayMode.setLabelFor(cbDisplayMode);
    lblDisplayMode.setText("Display");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblDisplayMode, gridBagConstraints);

    cbDisplayMode
        .setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    cbDisplayMode.setMinimumSize(new java.awt.Dimension(200, 20));
    cbDisplayMode.setPreferredSize(new java.awt.Dimension(200, 20));
    cbDisplayMode.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        cbDisplayModeActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(cbDisplayMode, gridBagConstraints);

    chkbL2TextDefault.setText("Default");
    chkbL2TextDefault.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    chkbL2TextDefault.setMargin(new java.awt.Insets(0, 0, 0, 0));
    chkbL2TextDefault.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        chkbL2TextDefaultActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    getContentPane().add(chkbL2TextDefault, gridBagConstraints);

    lblL4Text.setLabelFor(tfL4Text);
    lblL4Text.setText("Status text");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblL4Text, gridBagConstraints);

    tfL4Text.setMinimumSize(new java.awt.Dimension(200, 20));
    tfL4Text.setPreferredSize(new java.awt.Dimension(200, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(tfL4Text, gridBagConstraints);

    chkbL4TextDefault.setText("Default");
    chkbL4TextDefault.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    chkbL4TextDefault.setMargin(new java.awt.Insets(0, 0, 0, 0));
    chkbL4TextDefault.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        chkbL4TextDefaultActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    getContentPane().add(chkbL4TextDefault, gridBagConstraints);

    lblImgPath.setLabelFor(tfImgPath);
    lblImgPath.setText("Image");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblImgPath, gridBagConstraints);

    tfImgPath.setMinimumSize(new java.awt.Dimension(200, 20));
    tfImgPath.setPreferredSize(new java.awt.Dimension(200, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(tfImgPath, gridBagConstraints);

    btnImgPathBrowse.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/fileopen16.png"))); // NOI18N
    btnImgPathBrowse.setText("Browse");
    btnImgPathBrowse.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
    btnImgPathBrowse.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnImgPathBrowseActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    getContentPane().add(btnImgPathBrowse, gridBagConstraints);

    lblBgImgPath.setLabelFor(tfBgImgPath);
    lblBgImgPath.setText("Background image");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblBgImgPath, gridBagConstraints);

    tfBgImgPath.setMinimumSize(new java.awt.Dimension(200, 20));
    tfBgImgPath.setPreferredSize(new java.awt.Dimension(200, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(tfBgImgPath, gridBagConstraints);

    btnBgImgPathBrowse.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/fileopen16.png"))); // NOI18N
    btnBgImgPathBrowse.setText("Browse");
    btnBgImgPathBrowse.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
    btnBgImgPathBrowse.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnBgImgPathBrowseActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
    getContentPane().add(btnBgImgPathBrowse, gridBagConstraints);

    lblBgImgScale.setLabelFor(tfBgImgScale);
    lblBgImgScale.setText("Background image scale");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblBgImgScale, gridBagConstraints);

    tfBgImgScale.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    tfBgImgScale.setText("-1.0");
    tfBgImgScale.setMinimumSize(new java.awt.Dimension(70, 20));
    tfBgImgScale.setPreferredSize(new java.awt.Dimension(70, 20));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 13;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(tfBgImgScale, gridBagConstraints);

    btnClose.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/back16.png"))); // NOI18N
    btnClose.setText("Close");
    btnClose.setHorizontalAlignment(javax.swing.SwingConstants.LEADING);
    btnClose.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnCloseActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 14;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(10, 2, 5, 2);
    getContentPane().add(btnClose, gridBagConstraints);

    lblPageBounds.setText("1 - 10");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    getContentPane().add(lblPageBounds, gridBagConstraints);

    lblPosLLXBounds.setText("0.0 - 20.0");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    getContentPane().add(lblPosLLXBounds, gridBagConstraints);

    lblPosLLYBounds.setText("0.0 - 20.0");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 3;
    getContentPane().add(lblPosLLYBounds, gridBagConstraints);

    lblL2Text.setLabelFor(taL2Text);
    lblL2Text.setText("Signature text");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblL2Text, gridBagConstraints);

    lblL2TextFontSize.setLabelFor(tfL2TextFontSize);
    lblL2TextFontSize.setText("Signature text size");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(lblL2TextFontSize, gridBagConstraints);

    tfL2TextFontSize.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
    tfL2TextFontSize.setText("10.0");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(tfL2TextFontSize, gridBagConstraints);

    btnPreview.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/jsignpdf/preview16.png"))); // NOI18N
    btnPreview.setText("Preview");
    btnPreview.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        btnPreviewActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(btnPreview, gridBagConstraints);

    jScrollPane1.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    jScrollPane1.setMinimumSize(new java.awt.Dimension(24, 48));

    taL2Text.setColumns(20);
    taL2Text.setRows(5);
    jScrollPane1.setViewportView(taL2Text);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
    getContentPane().add(jScrollPane1, gridBagConstraints);

    chkbAcro6Layers.setSelected(true);
    chkbAcro6Layers.setText("Acrobat 6 layer mode");
    chkbAcro6Layers.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
    chkbAcro6Layers.setMargin(new java.awt.Insets(0, 0, 0, 0));
    chkbAcro6Layers.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        chkbAcro6LayersActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
    gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
    getContentPane().add(chkbAcro6Layers, gridBagConstraints);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void chkbAcro6LayersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkbAcro6LayersActionPerformed
    // TODO add your handling code here:
  }//GEN-LAST:event_chkbAcro6LayersActionPerformed

  private void btnPreviousActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnPreviousActionPerformed
    tfPage.setText(String.valueOf(ConvertUtils.toInt(tfPage.getText(), 2) - 1));
    btnPreviewActionPerformed(evt);
  }// GEN-LAST:event_btnPreviousActionPerformed

  private void btnNextActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnNextActionPerformed
    tfPage.setText(String.valueOf(ConvertUtils.toInt(tfPage.getText(), 0) + 1));
    btnPreviewActionPerformed(evt);
  }// GEN-LAST:event_btnNextActionPerformed

  private void btnPreviewCloseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnPreviewCloseActionPerformed
    previewDialog.setVisible(false);
  }// GEN-LAST:event_btnPreviewCloseActionPerformed

  private int getInt(float aFloat) {
    return Math.round(aFloat * 100F);
  }

  private void btnPreviewActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnPreviewActionPerformed
    final Integer pageNr = ConvertUtils.toInteger(tfPage.getText());
    if (pageNr != null) {
      btnPrevious.setEnabled(pageNr > 1);
      btnNext.setEnabled(pageNr < numberOfPages);
      // TODO progress bar or animated image... "yes, we are working..."
      final BufferedImage buffImg = p2i.getImageForPage(pageNr.intValue());
      if (buffImg != null) {
        final RelRect tmpRect = selectionImage.getRelRect();
        previewListenerDisabled = true;
        try {
          tmpRect.scale(getInt(pdfPageSize.getX()), getInt(pdfPageSize.getY()));
          tmpRect.setStartPoint(new FloatPoint(Float.parseFloat(tfPosLLX.getText()) / pdfPageSize.getX(), 1f
              - Float.parseFloat(tfPosLLY.getText()) / pdfPageSize.getY()));
          tmpRect.setEndPoint(new FloatPoint(Float.parseFloat(tfPosURX.getText()) / pdfPageSize.getX(), 1f
              - Float.parseFloat(tfPosURY.getText()) / pdfPageSize.getY()));
        } catch (Exception e) {
          // TODO
        }
        selectionImage.setImage(buffImg);
        previewListenerDisabled = false;
        previewDialog.setVisible(true);
      } else {
        JOptionPane.showMessageDialog(this, RES.get("error.vs.previewFailed"), "Error", JOptionPane.WARNING_MESSAGE);
      }
    } else {
      JOptionPane.showMessageDialog(this, RES.get("error.vs.pageNotANumber"), "Error", JOptionPane.WARNING_MESSAGE);
    }
  }// GEN-LAST:event_btnPreviewActionPerformed

  private void btnBgImgPathBrowseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnBgImgPathBrowseActionPerformed
    fc.showFileChooser(tfBgImgPath, null, JFileChooser.OPEN_DIALOG);
  }// GEN-LAST:event_btnBgImgPathBrowseActionPerformed

  private void btnImgPathBrowseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnImgPathBrowseActionPerformed
    fc.showFileChooser(tfImgPath, null, JFileChooser.OPEN_DIALOG);
  }// GEN-LAST:event_btnImgPathBrowseActionPerformed

  private void btnCloseActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCloseActionPerformed
    setVisible(false);
  }// GEN-LAST:event_btnCloseActionPerformed

  private void chkbL4TextDefaultActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_chkbL4TextDefaultActionPerformed
    tfL4Text.setEnabled(!chkbL4TextDefault.isSelected());
  }// GEN-LAST:event_chkbL4TextDefaultActionPerformed

  private void chkbL2TextDefaultActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_chkbL2TextDefaultActionPerformed
    taL2Text.setEnabled(!chkbL2TextDefault.isSelected());
  }// GEN-LAST:event_chkbL2TextDefaultActionPerformed

  private void formComponentHidden(java.awt.event.ComponentEvent evt) {// GEN-FIRST:event_formComponentHidden
    storeToOptions();
  }// GEN-LAST:event_formComponentHidden

  private void formComponentShown(java.awt.event.ComponentEvent evt) {// GEN-FIRST:event_formComponentShown
    updateFromOptions();
    readPdfInfo();
    switchImage();
  }// GEN-LAST:event_formComponentShown

  private void cbDisplayModeActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_cbDisplayModeActionPerformed
    switchImage();
  }// GEN-LAST:event_cbDisplayModeActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton btnBgImgPathBrowse;
  private javax.swing.JButton btnClose;
  private javax.swing.JButton btnImgPathBrowse;
  private javax.swing.JButton btnNext;
  private javax.swing.JButton btnPreview;
  private javax.swing.JButton btnPreviewClose;
  private javax.swing.JButton btnPrevious;
  private javax.swing.JComboBox cbDisplayMode;
  private javax.swing.JCheckBox chkbAcro6Layers;
  private javax.swing.JCheckBox chkbL2TextDefault;
  private javax.swing.JCheckBox chkbL4TextDefault;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JLabel lblBgImgPath;
  private javax.swing.JLabel lblBgImgScale;
  private javax.swing.JLabel lblDisplayMode;
  private javax.swing.JLabel lblImgPath;
  private javax.swing.JLabel lblL2Text;
  private javax.swing.JLabel lblL2TextFontSize;
  private javax.swing.JLabel lblL4Text;
  private javax.swing.JLabel lblPage;
  private javax.swing.JLabel lblPageBounds;
  private javax.swing.JLabel lblPosLLX;
  private javax.swing.JLabel lblPosLLXBounds;
  private javax.swing.JLabel lblPosLLY;
  private javax.swing.JLabel lblPosLLYBounds;
  private javax.swing.JLabel lblPosURX;
  private javax.swing.JLabel lblPosURY;
  private javax.swing.JLabel lblPosition;
  private javax.swing.JLabel lblSettings;
  private javax.swing.JDialog previewDialog;
  private javax.swing.JTextArea taL2Text;
  private javax.swing.JTextField tfBgImgPath;
  private javax.swing.JTextField tfBgImgScale;
  private javax.swing.JTextField tfImgPath;
  private javax.swing.JTextField tfL2TextFontSize;
  private javax.swing.JTextField tfL4Text;
  private javax.swing.JTextField tfPage;
  private javax.swing.JTextField tfPosLLX;
  private javax.swing.JTextField tfPosLLY;
  private javax.swing.JTextField tfPosURX;
  private javax.swing.JTextField tfPosURY;
  // End of variables declaration//GEN-END:variables

}

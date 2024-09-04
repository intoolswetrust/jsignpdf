/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package net.sf.jsignpdf;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.apache.commons.io.FilenameUtils;

/**
 *
 * @author jansv
 */
public class BatchFilelistTableModel extends AbstractTableModel {

    private final List<BasicSignerOptions> optionsList;

    private final String[] columnNames = {
        "Path", "Filename", "New name"
    };
    private final Class[] columnTypes = new Class[]{
        java.lang.String.class, java.lang.String.class, java.lang.String.class
    };
    private final boolean[] columnCanEdit = new boolean[]{
        false, false, false
    };

    public BatchFilelistTableModel() {
        super();
        this.optionsList = new ArrayList<>();
    }

    @Override
    public int getRowCount() {
        return optionsList.size();
    }

    @Override
    public int getColumnCount() {
        return columnTypes.length;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnCanEdit[columnIndex];
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object value = "??";
        BasicSignerOptions options = this.getOptionsAt(rowIndex);
        switch (columnIndex) {
            case 0:
                value = FilenameUtils.getPath(options.getInFile());
                break;
            case 1:
                value = FilenameUtils.getName(options.getInFile());
                break;
            case 2:
                value = FilenameUtils.getName(options.getOutFile());
                break;
        }

        return value;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        super.setValueAt(aValue, rowIndex, columnIndex); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    public BasicSignerOptions getOptionsAt(int index) {
        return optionsList.get(index);
    }

    public void setOutputSuffix(String suffix) {
        if (suffix.equals("")) {
            suffix = "_signed";
        }
        for (BasicSignerOptions options : optionsList) {
            String newName = FilenameUtils.getBaseName(options.getInFile()) + suffix + ".pdf";
            String parentDir = FilenameUtils.getPath(options.getInFile());
            options.setOutFile(FilenameUtils.concat(parentDir, newName));
        }
        for (int row = 0; row < optionsList.size(); row++) {
            fireTableCellUpdated(row, 2);
        }
    }

    void removeRow(int i) {
        this.optionsList.remove(i);
//        LOGGER.log(Level.INFO, "Removed row at index {0}", i);
        this.fireTableRowsDeleted(i, i);
    }

    void addFileOptions(BasicSignerOptions options) {
        int newRowIndex = optionsList.size();
        this.optionsList.add(options);
//        LOGGER.log(Level.INFO, "Added file {0}", options.getInFile());
        fireTableRowsInserted(newRowIndex, newRowIndex);
    }

    void addFiles(File[] chosenFiles, BasicSignerOptions defaultOptions, String fileSuffix) {
//        LOGGER.log(Level.INFO, "Adding {0} entries", chosenFiles.length);
        for (File file : chosenFiles) {
            String suffix = !"".equals(fileSuffix) ? fileSuffix : "_signed";
            String newName = FilenameUtils.getBaseName(file.getName()) + suffix + ".pdf";
            String parentDir = file.getAbsoluteFile().getParent();

            BasicSignerOptions options = (BasicSignerOptions) cloneObject(defaultOptions);
            options.setInFile(file.getAbsolutePath());
            options.setOutFile(FilenameUtils.concat(parentDir, newName));
            this.addFileOptions(options);
        }
    }

    /**
     * Clones object
     *
     * @param obj Object to clone
     * @return
     */
    private static Object cloneObject(Object obj) {
        try {
            Object clone = obj.getClass().newInstance();
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                field.set(clone, field.get(obj));
            }
            return clone;
        } catch (Exception e) {
            return null;
        }
    }

}

package net.sf.jsignpdf.fx.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.jsignpdf.Constants;
import net.sf.jsignpdf.utils.PropertyProvider;
import net.sf.jsignpdf.utils.PropertyStoreFactory;

/**
 * Manages a list of recently opened PDF files, persisted via PropertyProvider.
 */
public class RecentFilesManager {

    private static final Logger LOGGER = Logger.getLogger(RecentFilesManager.class.getName());
    private static final int MAX_RECENT = 10;
    private final PropertyProvider props = PropertyStoreFactory.getInstance().mainConfig();

    public void addFile(File file) {
        List<String> files = getRecentFiles();
        String path = file.getAbsolutePath();
        files.remove(path);
        files.add(0, path);
        while (files.size() > MAX_RECENT) {
            files.remove(files.size() - 1);
        }
        saveFiles(files);
    }

    public List<String> getRecentFiles() {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < MAX_RECENT; i++) {
            String path = props.getProperty(Constants.PROPERTY_RECENT_FILE_PREFIX + i);
            if (path != null && !path.isEmpty() && new File(path).exists()) {
                result.add(path);
            }
        }
        return result;
    }

    public void clear() {
        for (int i = 0; i < MAX_RECENT; i++) {
            props.removeProperty(Constants.PROPERTY_RECENT_FILE_PREFIX + i);
        }
        try {
            props.save();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not persist cleared recent files list", e);
        }
    }

    private void saveFiles(List<String> files) {
        for (int i = 0; i < MAX_RECENT; i++) {
            if (i < files.size()) {
                props.setProperty(Constants.PROPERTY_RECENT_FILE_PREFIX + i, files.get(i));
            } else {
                props.removeProperty(Constants.PROPERTY_RECENT_FILE_PREFIX + i);
            }
        }
    }
}

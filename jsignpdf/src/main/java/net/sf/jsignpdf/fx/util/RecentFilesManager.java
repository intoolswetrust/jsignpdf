package net.sf.jsignpdf.fx.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages a list of recently opened PDF files, persisted via java.util.prefs.
 */
public class RecentFilesManager {

    private static final String PREFS_KEY_PREFIX = "recentFile_";
    private static final int MAX_RECENT = 10;
    private final Preferences prefs;

    public RecentFilesManager() {
        prefs = Preferences.userNodeForPackage(RecentFilesManager.class);
    }

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
            String path = prefs.get(PREFS_KEY_PREFIX + i, null);
            if (path != null && new File(path).exists()) {
                result.add(path);
            }
        }
        return result;
    }

    private void saveFiles(List<String> files) {
        for (int i = 0; i < MAX_RECENT; i++) {
            if (i < files.size()) {
                prefs.put(PREFS_KEY_PREFIX + i, files.get(i));
            } else {
                prefs.remove(PREFS_KEY_PREFIX + i);
            }
        }
    }
}

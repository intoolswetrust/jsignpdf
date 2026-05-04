package net.sf.jsignpdf;

/**
 * Prints major Java version.
 * 
 * @author Josef Cacek
 */
public class JavaVersion {

    public static void main(String[] args) {
        System.out.println(getJavaMajorVersion());
    }

    public static int getJavaMajorVersion() {
        Class rtVersionCl;

        try {
            rtVersionCl = Class.forName("java.lang.Runtime$Version");
        } catch (ClassNotFoundException e) {
            return 8;
        }

        try {
            Object versionObj = Runtime.class.getDeclaredMethod("version").invoke(Runtime.getRuntime());
            return (Integer) rtVersionCl.getDeclaredMethod("major").invoke(versionObj);
        } catch (Exception e) {
            return 8;
        }
    }
}

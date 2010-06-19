package net.sf.jsignpdf.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Common util(s) for IO operations. 
 * @author Josef Cacek
 */
public class IOUtils {
    
	/**
	 * line separator in this system
	 */
	public final static String lineBreak = System.getProperty("line.separator");

    /**
     * The name says it all.
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
 
    
    /**
     * Copy bytes from an <code>InputStream</code> to an <code>OutputStream</code>.
     * @param input the <code>InputStream</code> to read from
     * @param output the <code>OutputStream</code> to write to
     * @return the number of bytes copied
     * @throws IOException In case of an I/O problem
     */
    public static int copy(
            InputStream input,
            OutputStream output)
                throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    } 
    
    /**
     * Writes object to GZIPped output stream - closes stream after reading.
     * @param os output stream
     * @param obj object to write
     * @throws IOException
     */
    public static void obj2stream(OutputStream os, Object obj)
            throws IOException {
        GZIPOutputStream gzos = new GZIPOutputStream(os);
        ObjectOutputStream oos = new ObjectOutputStream(gzos);
//        oos.writeObject(new MarshalledObject(obj));
        oos.writeObject(obj);
        oos.close();
        os.close();
    }

    /**
     * Read GZIPped object from input stream - closes stream after reading.
     * @param is Input stream
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object stream2obj(InputStream is) throws IOException,
            ClassNotFoundException {
        GZIPInputStream gzis = new GZIPInputStream(is);
        ObjectInputStream ois = new ObjectInputStream(gzis);
//        Object obj = ((MarshalledObject) ois.readObject()).get();
        Object obj = ois.readObject();
        ois.close();
        is.close();
        return obj;
    }    

}

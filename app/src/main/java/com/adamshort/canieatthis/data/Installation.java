package com.adamshort.canieatthis.data;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

public class Installation {
    private static final String INSTALLATION = "INSTALLATION";

    private static String mSID = null;

    /**
     * Creates a new installation file and ID if one does not exist and returns the ID.
     *
     * @param context Context needed to write the file to the apps directory.
     * @return The unique ID of an installation.
     */
    public synchronized static String id(Context context) {
        if (mSID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists())
                    writeInstallationFile(installation);
                mSID = readInstallationFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return mSID;
    }

    /**
     * Reads the installation file in the apps directory.
     *
     * @param installation The File to read.
     * @return The ID contained in the file.
     * @throws IOException
     */
    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    /**
     * Check if the specified string is contained in the Installation file.
     *
     * @param context Context needed to get the apps file directory.
     * @param s       The string to check the existence of.
     * @return True or False depending on whether the string is in the file.
     * @throws IOException
     */
    public static boolean isInInstallationFile(Context context, String s) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File(context.getFilesDir(), INSTALLATION)));
        for (String line; (line = br.readLine()) != null; ) {
            if (line.equals(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a random UUID to a file.
     *
     * @param installation The file to write to.
     * @throws IOException
     */
    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }

    /**
     * Writes a specific string to a file.
     *
     * @param installation The file to write to.
     * @param s            The string to write into the file.
     * @param append       Whether or not to append to the file.
     * @throws IOException
     */
    public static void writeInstallationFile(File installation, String s,
                                             boolean append) throws IOException {
        FileOutputStream out = new FileOutputStream(installation, append);
        out.write(s.getBytes());
        out.close();
    }

    /**
     * Deletes the installation file. Only really used for testing.
     *
     * @param context Context needed to access the file.
     * @return True or False if both the file exists and if it was deleted successfully.
     */
    public static boolean deleteInstallationFile(Context context) {
        File file = new File(context.getFilesDir(), INSTALLATION);
        return file.exists() && file.delete();
    }

    /**
     * @return The installation string.
     */
    public static String getInstallation() {
        return INSTALLATION;
    }
}

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
    private static String sID = null;
    private static final String INSTALLATION = "INSTALLATION";

    public synchronized static String id(Context context) {
        if (sID == null) {
            File installation = new File(context.getFilesDir(), INSTALLATION);
            try {
                if (!installation.exists())
                    writeInstallationFile(installation);
                sID = readInstallationFile(installation);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return sID;
    }

    private static String readInstallationFile(File installation) throws IOException {
        RandomAccessFile f = new RandomAccessFile(installation, "r");
        byte[] bytes = new byte[(int) f.length()];
        f.readFully(bytes);
        f.close();
        return new String(bytes);
    }

    public static boolean isInInstallationFile(Context context, String s) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File(context.getFilesDir(), INSTALLATION)));
        for (String line; (line = br.readLine()) != null; ) {
            if (line.equals(s)) {
                return true;
            }
        }
        return false;
    }

    private static void writeInstallationFile(File installation) throws IOException {
        FileOutputStream out = new FileOutputStream(installation);
        String id = UUID.randomUUID().toString();
        out.write(id.getBytes());
        out.close();
    }

    public static void writeInstallationFile(File installation, String s,
                                             boolean append) throws IOException {
        FileOutputStream out = new FileOutputStream(installation, append);
        out.write(s.getBytes());
        out.close();
    }

    public static boolean deleteInstallationFile(Context context) {
        File file = new File(context.getFilesDir(), INSTALLATION);
        return file.exists() && file.delete();
    }

    public static String getInstallation() {
        return INSTALLATION;
    }
}

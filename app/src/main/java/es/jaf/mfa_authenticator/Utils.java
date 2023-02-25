/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package es.jaf.mfa_authenticator;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static byte[] readFully(File file) throws IOException {
        if (!(file).exists()) {
            return null;
        }
        try (final InputStream is = new FileInputStream(file)) {
            return readFully(is);
        }
    }

    public static byte[] readFully(InputStream is) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = is.read(buffer)) != -1) {
            bytes.write(buffer, 0, count);
        }
        return bytes.toByteArray();
    }

    public static void writeFully(File file, byte[] data) throws Exception {
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(data);
        }
    }

    public static void writeFully(OutputStream os, byte[] data) throws IOException {
        os.write(data);
    }

    static void saveException(String text, Exception ex) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

        FileOutputStream f = null;
        PrintWriter pw = null;
        try {
            File root = android.os.Environment.getExternalStorageDirectory();
            File dir = new File(root.getAbsolutePath() + "/" + BuildConfig.APPLICATION_ID);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    return;
                }
            }
            File file = new File(dir, "exceptions.txt");
            f = new FileOutputStream(file, true);
            pw = new PrintWriter(f);
            pw.println(sdf.format(new Date()) + "\t" + text);
            if (ex != null) {
                pw.println(sdf.format(new Date()) + "\t" + "Message:" + ex.getMessage());
                ex.printStackTrace(pw);
            }
            pw.flush();
            pw.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (pw != null) {
                pw.flush();
                pw.close();
            }
            if (f != null) {
                try {
                    f.close();
                } catch (Exception e) {/**/}
            }
        }
    }
}
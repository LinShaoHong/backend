package com.github.sun.qm;

import com.github.sun.foundation.sql.IdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class StorageService {
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

    @Value("${image.store.dir}")
    private String basePath;

    @SuppressWarnings("Duplicates")
    public String upload(InputStream in, String name) throws IOException {
        int i = name.lastIndexOf(".");
        String ext = ".jpg";
        if (i > 0) {
            ext = name.substring(i + 1);
        }
        String id = IdGenerator.next();
        String day = FORMATTER.format(new Date());
        String path = basePath + "/" + day + "/" + id + "." + ext;
        File file = new File(path);
        if (!file.exists()) {
            File dir = new File(file.getParent());
            dir.mkdirs();
            file.createNewFile();
        }
        OutputStream out = new FileOutputStream(file);
        byte[] bytes = new byte[2048];
        int n;
        while ((n = in.read(bytes, 0, bytes.length)) != -1) {
            out.write(bytes, 0, n);
        }
        in.close();
        out.close();
        return "/" + day + "/" + id + "." + ext;
    }

    public void delete(String path) {
        File file = new File(basePath + path);
        if (file.exists()) {
            file.delete();
        }
    }
}

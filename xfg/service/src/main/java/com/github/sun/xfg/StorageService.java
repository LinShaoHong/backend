package com.github.sun.xfg;

import com.github.sun.foundation.sql.IdGenerator;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class StorageService {
  private static final String BASE_PATH = "/opt/static/image/xfg";
  private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

  public String upload(InputStream in, String name) throws IOException {
    int i = name.lastIndexOf(".");
    String ext = ".jpg";
    if (i > 0) {
      ext = name.substring(i + 1);
    }
    String id = IdGenerator.next();
    String day = FORMATTER.format(new Date());
    String path = BASE_PATH + "/" + day + "/" + id + "." + ext;
    File file = new File(path);
    if (!file.exists()) {
      File dir = new File(file.getParent());
      dir.mkdirs();
      file.createNewFile();
    }
    OutputStream out = new FileOutputStream(file);
    byte[] bytes = new byte[2048];
    int n = -1;
    while ((n = in.read(bytes, 0, bytes.length)) != -1) {
      out.write(bytes, 0, n);
    }
    in.close();
    out.close();
    return "/" + day + "/" + id + "." + ext;
  }

  public void delete(String path) {
    File file = new File(BASE_PATH + path);
    if (file.exists()) {
      file.delete();
    }
  }
}

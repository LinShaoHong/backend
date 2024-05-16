package com.github.sun.card;

import com.github.sun.foundation.sql.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@RefreshScope
@RequiredArgsConstructor
public class CardStoreService {
  private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

  @Value("${image.store.dir}")
  private String basePath;

  @SuppressWarnings("Duplicates")
  public String put(InputStream in, String name) {
    try {
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
    } catch (Throwable ex) {
      throw new RuntimeException(ex);
    }
  }

  public void remove(String path) {
    File file = new File(basePath + path);
    if (file.exists()) {
      file.delete();
    }
  }
}
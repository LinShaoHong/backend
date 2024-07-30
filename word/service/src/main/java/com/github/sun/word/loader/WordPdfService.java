package com.github.sun.word.loader;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.fdf.FDFDocument;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@RefreshScope
public class WordPdfService {
  public void parseRoot(InputStream in) throws IOException {
    FDFDocument doc = Loader.loadFDF(in);

  }
}
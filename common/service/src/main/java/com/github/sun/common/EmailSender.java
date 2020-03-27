package com.github.sun.common;

import com.github.sun.foundation.boot.utility.Iterators;
import com.sun.mail.smtp.SMTPTransport;
import lombok.extern.slf4j.Slf4j;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@Slf4j
public class EmailSender {
  private String host;
  private final String username;
  private final String password;
  private final Properties props;

  public EmailSender(Properties properties) {
    this.username = properties.getProperty("username");
    this.password = properties.getProperty("password");
    this.props = new Properties();
    properties.forEach((k, v) -> {
      if (((String) k).startsWith("mail")) {
        this.props.put(k, v);
      }
    });
    for (Map.Entry<Object, Object> e : this.props.entrySet()) {
      if (((String) e.getKey()).endsWith(".host")) {
        this.host = (String) e.getValue();
        break;
      }
    }
    if (this.host == null) {
      throw new IllegalArgumentException("Missing Email Server");
    }
  }

  public void sendMessage(String subject, String content, String email) {
    sendMessage(null, subject, content, email);
  }

  public void sendMessage(String from, String subject, String content, String email) {
    sendMessage(from, subject, content, Collections.singleton(email));
  }

  public void sendMessage(String from, String subject, String content, Set<String> emails) {
    Session session = Session.getInstance(this.props, null);
    Message msg = new MimeMessage(session);
    try {
      msg.setFrom(new InternetAddress(from == null ? username : (from + "  <" + username + ">")));
      msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(Iterators.mkString(emails, ", "), false));
      msg.setSubject(subject);
      msg.setText(content);
      msg.setSentDate(new Date());
      SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
      t.connect(host, username, password);
      t.sendMessage(msg, msg.getAllRecipients());
      t.close();
    } catch (MessagingException ex) {
      log.error("send email failed:\n", ex);
    }
  }

  public void sendHTML(String subject, String html, String email) {
    sendHTML(null, subject, html, email);
  }

  public void sendHTML(String from, String subject, String html, String email) {
    sendHTML(from, subject, html, Collections.singleton(email));
  }

  public void sendHTML(String from, String subject, String html, Set<String> emails) {
    Session session = Session.getInstance(this.props, null);
    Message msg = new MimeMessage(session);
    try {
      msg.setFrom(new InternetAddress(from == null ? username : (from + "  <" + username + ">")));
      msg.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(Iterators.mkString(emails, ", "), false));
      msg.setSubject(subject);
      msg.setDataHandler(new DataHandler(new HTMLDataSource(html)));
      SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
      t.connect(host, username, password);
      t.sendMessage(msg, msg.getAllRecipients());
      t.close();
    } catch (MessagingException ex) {
      log.error("send email failed:\n", ex);
    }
  }

  private static class HTMLDataSource implements DataSource {
    private String html;

    private HTMLDataSource(String html) {
      this.html = html;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      if (html == null) throw new IOException("html message is null!");
      return new ByteArrayInputStream(html.getBytes());
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      throw new IOException("This DataHandler cannot write HTML");
    }

    @Override
    public String getContentType() {
      return "text/html";
    }

    @Override
    public String getName() {
      return "HTMLDataSource";
    }
  }
}

package com.github.sun.qm;

import com.sun.mail.smtp.SMTPTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
import java.util.Date;
import java.util.Properties;

@Slf4j
@Service
public class MailService {
  private static final String SMTP_SERVER = "smtp.gmail.com";
  private static Properties prop;

  @Value("${gmail.username}")
  private String username;
  @Value("${gmail.password}")
  private String password;

  static {
    prop = System.getProperties();
    prop.put("mail.transport.protocol", "smtp");
    prop.put("mail.smtp.host", "smtp.gmail.com");
    prop.put("mail.smtp.auth", "true");
    prop.put("mail.smtp.port", "25");
    prop.put("mail.smtp.starttls.enable", "true");
  }

  public void sendMessage(String email, String subject, String content) {
    Session session = Session.getInstance(prop, null);
    Message msg = new MimeMessage(session);
    try {
      msg.setFrom(new InternetAddress(username));
      msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(email, false));
      msg.setSubject(subject);
      msg.setText(content);
      msg.setSentDate(new Date());
      SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
      t.connect(SMTP_SERVER, username, password);
      t.sendMessage(msg, msg.getAllRecipients());
      t.close();
    } catch (MessagingException ex) {
      log.error("send email failed:\n", ex);
    }
  }

  public void sendHTML(String email, String subject, String html) {
    Session session = Session.getInstance(prop, null);
    Message msg = new MimeMessage(session);
    try {
      msg.setFrom(new InternetAddress(username));
      msg.setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(email, false));
      msg.setSubject(subject);
      msg.setDataHandler(new DataHandler(new HTMLDataSource(html)));
      SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
      t.connect(SMTP_SERVER, username, password);
      t.sendMessage(msg, msg.getAllRecipients());
      t.close();
    } catch (MessagingException ex) {
      log.error("send email failed:\n", ex);
    }
  }

  static class HTMLDataSource implements DataSource {
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

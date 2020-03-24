package com.github.sun.qm;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.boot.utility.Throws;
import com.github.sun.scheduler.SchedulerJob;
import com.github.sun.scheduler.SchedulerJobMapper;
import com.github.sun.scheduler.SchedulerTask;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service("QM_BACKUP" + SchedulerTask.SUFFIX)
public class BackupTask implements SchedulerTask {
  private static final int MAX_LATEST_SIZE = 7;
  private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
  private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private final List<Progress> latest = new ArrayList<>();
  private Progress progress = null;
  private AtomicBoolean running = new AtomicBoolean(false);

  @Resource
  private SchedulerJobMapper mapper;

  @Override
  public void start() {
    Session session = null;
    SchedulerJob job = mapper.findById("QM_BACKUP");
    if (job != null) {
      Throwable e = null;
      try {
        this.running.set(true);
        long startTime = System.currentTimeMillis();
        this.progress = Progress.builder()
          .running(true)
          .startTime(TIME_FORMATTER.format(new Date()))
          .errors(new ArrayList<>())
          .build();

        JsonNode profiles = job.getProfiles();
        String ip = profiles.get("ip").asText();
        String username = profiles.get("username").asText();
        String password = profiles.get("password").asText();
        String tar = profiles.get("tar").asText();
        String from = profiles.get("from").asText();
        String to = profiles.get("to").asText();

        session = createSession(ip, username, password);
        execute(session, tar);
        copyRemoteToLocal(session, from, to, startTime);
      } catch (Throwable ex) {
        e = ex;
      } finally {
        if (session != null) {
          session.disconnect();
        }
        if (e != null) {
          progress.getErrors().add(Throws.stackTraceOf(e));
        }
        this.running.set(false);
        progress.setRunning(false);
        progress.setEndTime(DATE_FORMATTER.format(new Date()));
        pushProgress();
      }
    }
  }

  @Override
  public void stop() {
    this.running.set(false);
  }

  private Session createSession(String ip, String username, String password) {
    try {
      JSch jsch = new JSch();
      Session session = jsch.getSession(username, ip, 22);
      session.setPassword(password);
      session.setConfig("StrictHostKeyChecking", "no");
      session.connect();
      return session;
    } catch (JSchException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void copyRemoteToLocal(Session session, String from, String to, long startTime) throws JSchException, IOException {
    int index = to.lastIndexOf(File.separator);
    to = to.substring(0, index) + File.separator + DATE_FORMATTER.format(new Date()) + "-" + to.substring(index + 1);
    Channel channel = session.openChannel("exec");
    ((ChannelExec) channel).setCommand(from);
    OutputStream out = channel.getOutputStream();
    InputStream in = channel.getInputStream();
    channel.connect();
    byte[] buf = new byte[1024];
    // send '\0'
    buf[0] = 0;
    out.write(buf, 0, 1);
    out.flush();
    long fileSize = 0L;
    while (this.running.get()) {
      int c = checkAck(in);
      if (c != 'C') {
        break;
      }

      // read '0644 '
      in.read(buf, 0, 5);
      fileSize = 0L;
      while (true) {
        if (in.read(buf, 0, 1) < 0) {
          // error
          break;
        }
        if (buf[0] == ' ') break;
        fileSize = fileSize * 10L + (long) (buf[0] - '0');
        this.progress.setTotal(fileSize / (1024 * 1024));
      }

      for (int i = 0; ; i++) {
        in.read(buf, i, 1);
        if (buf[i] == (byte) 0x0a) {
          break;
        }
      }
      // send '\0'
      buf[0] = 0;
      out.write(buf, 0, 1);
      out.flush();

      FileOutputStream fos = new FileOutputStream(to);
      int foo;
      while (this.running.get()) {
        if (buf.length < fileSize) foo = buf.length;
        else foo = (int) fileSize;
        foo = in.read(buf, 0, foo);
        if (foo < 0) {
          // error
          break;
        }
        fos.write(buf, 0, foo);
        fileSize -= foo;
        progress.setFinished((progress.getTotal() - fileSize / (1024 * 1024)));
        progress.setUsedTime(Dates.formatTime(System.currentTimeMillis() - startTime));
        if (fileSize == 0L) break;
      }

      if (checkAck(in) == 0) {
        // send '\0'
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        try {
          fos.close();
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    }
    channel.disconnect();
  }

  private int checkAck(InputStream in) throws IOException {
    int b = in.read();
    // b may be 0 for success,
    //          1 for error,
    //          2 for fatal error,
    //         -1
    if (b == 0) return b;
    if (b == -1) return b;

    if (b == 1 || b == 2) {
      StringBuilder sb = new StringBuilder();
      int c;
      do {
        c = in.read();
        sb.append((char) c);
      }
      while (c != '\n');
      this.progress.getErrors().add(sb.toString());
    }
    return b;
  }

  public void execute(Session session, String command) throws JSchException, IOException {
    Channel channel = session.openChannel("exec");
    ((ChannelExec) channel).setCommand(command);
    InputStream commandOutput = channel.getInputStream();
    channel.connect();
    int readByte = commandOutput.read();
    while (readByte != 0xffffffff) {
      readByte = commandOutput.read();
    }
    channel.disconnect();
  }

  private void pushProgress() {
    if (latest.size() >= MAX_LATEST_SIZE) {
      latest.remove(0);
    }
    latest.add(this.progress.clone());
    this.progress = Progress.builder().errors(new ArrayList<>()).build();
  }

  @Override
  public Progress progress() {
    return this.progress;
  }

  @Override
  public List<Progress> latestProgress() {
    return this.latest;
  }
}

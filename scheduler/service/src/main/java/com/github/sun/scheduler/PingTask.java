package com.github.sun.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.common.EmailSender;
import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.Throws;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service("PING" + SchedulerTask.SUFFIX)
public class PingTask implements SchedulerTask {
    private static final int MAX_LATEST_SIZE = 7;
    private static final int TIME_OUT = 2000;
    private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Resource
    private SchedulerJobMapper mapper;
    private final EmailSender emailSender;

    private final List<Progress> latest = new ArrayList<>();
    private Progress progress = null;

    @Autowired
    public PingTask(@Qualifier("qmail") EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void start() {
        SchedulerJob job = mapper.findById("PING");
        if (job != null) {
            JsonNode profiles = job.getProfiles();
            Set<String> ips = new HashSet<>();
            JSON.newValuer(profiles.get("ips")).asArray().forEach(v -> ips.add(v.asText()));
            long startTime = System.currentTimeMillis();
            this.progress = Progress.builder()
                    .running(true)
                    .total(ips.size())
                    .startTime(TIME_FORMATTER.format(new Date()))
                    .errors(new ArrayList<>())
                    .build();
            int count = 3;
            int finished = 1;
            for (String ip : ips) {
                boolean reachable = false;
                for (int i = 0; i < count; i++) {
                    try {
                        if (InetAddress.getByName(ip).isReachable(TIME_OUT)) {
                            reachable = true;
                            break;
                        }
                    } catch (IOException e) {
                        if (i >= count - 1) {
                            progress.getErrors().add(Throws.stackTraceOf(e));
                        }
                    }
                }
                progress.setFinished(finished++);
                if (!reachable) {
                    String msg = "IP=" + ip + " IS Unconnected ";
                    progress.getErrors().add(msg);
                    emailSender.sendMessage("IP连通性", "IP不可达", msg, "lin3404@126.com");
                }
            }
            progress.setRunning(false);
            progress.setEndTime(TIME_FORMATTER.format(new Date()));
            progress.setUsedTime(Dates.formatTime(System.currentTimeMillis() - startTime));
            pushProgress();
        }
    }

    @Override
    public void stop() {
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

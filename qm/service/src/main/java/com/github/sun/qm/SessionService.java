package com.github.sun.qm;

import com.github.sun.common.EmailSender;
import com.github.sun.foundation.boot.exception.Message;
import com.github.sun.foundation.boot.utility.AES;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

@Service
public class SessionService {
  public static final String TOKEN_NAME = "QM-TOKEN";
  private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

  @Resource
  private UserMapper mapper;
  @Resource
  private CommentMapper commentMapper;
  @Resource
  private ViewStatMapper viewStatMapper;
  @Resource(name = "mysql")
  private SqlBuilder.Factory factory;
  @Value("${base64.secret.key}")
  private String secretKey;
  @Value("${notice.mail}")
  private String noticeMail;

  private final EmailSender mailService;

  @Autowired
  public SessionService(@Qualifier("gmail") EmailSender mailService) {
    this.mailService = mailService;
  }

  @Transactional
  public String register(String username, String password, String email, String ip) {
    if (mapper.countByUsername(username) > 0) {
      throw new Message(1001);
    }
    if (mapper.countByEmail(email) > 0) {
      throw new Message(1003);
    }
    String id = IdGenerator.next();
    User user = User.builder()
      .id(id)
      .avatar("/avatar.jpg")
      .username(username)
      .password(User.hashPassword(password))
      .email(email)
      .lastLoginIp(ip)
      .lastLoginTime(new Date())
      .readSystemMessageIds(Collections.emptySet())
      .build();
    mapper.insert(user);
    commentMapper.insert(Comment.builder()
      .id(IdGenerator.next())
      .commentatorId(Comment.SYSTEM)
      .replierId(id)
      .time(System.currentTimeMillis())
      .content(username + " 恭喜您注册成功，快去個人中心簽到吧，可賺取1金幣哦~~~")
      .build());
    new Thread(() -> sendEmail(user)).start();
    return AES.encrypt(id, secretKey);
  }

  private void sendEmail(User user) {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException ex) {
      // do nothing
    }
    String username = user.getUsername();
    String email = user.getEmail();
    String content = "邮箱: " + email;
    String today = FORMATTER.format(new Date());
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(User.class)
      .where(sb.field("substr").call(sb.field("createTime"), 1, 10).eq(today))
      .select(sb.field("id").distinct().count())
      .template();
    int todayRegister = ((Long) mapper.findOneByTemplateAsMap(template).values().iterator().next()).intValue();
    content += "\n今日: " + todayRegister;
    content += "\n总计: " + mapper.count();
    ViewStat viewStat = viewStatMapper.findById("QM:" + today);
    if (viewStat != null) {
      content += "\n访问: " + viewStat.getVisits();
    }
    mailService.sendMessage("寻芳阁注册", username, content, noticeMail);
  }

  @Transactional
  public String login(String username, String password, String ip) {
    String hashPassword = User.hashPassword(password);
    User user = mapper.findByUsernameAndPassword(username, hashPassword);
    if (user == null) {
      if (mapper.countByUsername(username) > 0) {
        throw new Message(1002);
      } else {
        throw new Message(1000);
      }
    }
    if (ip != null) {
      user.setLastLoginIp(ip);
    }
    user.setLastLoginTime(new Date());
    mapper.update(user);
    return AES.encrypt(user.getId(), secretKey);
  }
}

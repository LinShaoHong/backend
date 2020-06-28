package com.github.sun.qm;

import com.github.sun.common.EmailSender;
import com.github.sun.foundation.boot.exception.BadRequestException;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;

@Service
public class CommentService {
  private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  @Resource
  private CommentMapper mapper;
  @Resource
  private UserMapper userMapper;
  @Resource
  private PayLogMapper payLogMapper;
  @Resource
  private GirlMapper girlMapper;
  @Resource(name = "mysql")
  private SqlBuilder.Factory factory;
  @Value("${notice.mail}")
  private String noticeMail;

  private final EmailSender mailService;

  @Autowired
  public CommentService(@Qualifier("gmail") EmailSender mailService) {
    this.mailService = mailService;
  }

  @Transactional
  public String comment(String userId, String girlId, String content) {
    String id = IdGenerator.next();
    Comment comment = Comment.builder()
      .id(id)
      .commentatorId(userId)
      .sessionId(id)
      .girlId(girlId)
      .content(content)
      .time(System.currentTimeMillis())
      .build();
    mapper.insert(comment);
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Girl.class)
      .where(sb.field("id").eq(girlId))
      .update()
      .set("comments", sb.field("comments").plus(1))
      .template();
    girlMapper.updateByTemplate(template);
    new Thread(() -> sendEmail(userId, girlId, content)).start();
    return id;
  }

  private void sendEmail(String userId, String girlId, String content) {
    User user = userMapper.findById(userId);
    Girl girl = girlMapper.findById(girlId);
    if (user != null && girl != null) {
      String girlInfo = girl.getCity() == null ? girl.getName() : girl.getCity() + " " + girl.getName();
      content = user.getUsername() + " 在【" + girlInfo + "】下评论:\n" + content;
      if (girl.getContact() != null && !girl.getContact().isEmpty()) {
        content += "\n\n联系方式:\n" + girl.getContact();
      }
      PayLog paylog = payLogMapper.findByUserIdAndGirlId(userId, girlId);
      if (paylog != null) {
        content += "\n\n消耗金币: " + paylog.getAmount().intValue();
        content += "\n购买时间: " + FORMATTER.format(paylog.getCreateTime());
      }
      mailService.sendMessage("寻芳阁评论", user.getUsername(), content, noticeMail);
    }
  }

  @Transactional
  public String reply(String id, String content, String userId) {
    Comment comment = mapper.findById(id);
    if (comment != null && !comment.getCommentatorId().equals(userId)) {
      String replyId = IdGenerator.next();
      Comment replay = Comment.builder()
        .id(replyId)
        .commentatorId(userId)
        .replierId(comment.getCommentatorId())
        .sessionId(comment.getSessionId())
        .content(content)
        .girlId(comment.getGirlId())
        .time(System.currentTimeMillis())
        .build();
      mapper.insert(replay);
      if (userId.equals(comment.getReplierId())) {
        mapper.read(comment.getId());
      }
      return replyId;
    }
    throw new BadRequestException("不予评论");
  }

  @Transactional
  public void read(User user, String id) {
    Comment comment = mapper.findById(id);
    if (comment == null) {
      return;
    }
    if (comment.isSystem() && comment.getReplierId() == null) {
      Set<String> set = user.getReadSystemMessageIds();
      set = set == null ? new HashSet<>() : set;
      set.add(id);
      user.setReadSystemMessageIds(set);
      userMapper.update(user);
    } else {
      mapper.read(id);
    }
  }

  @Transactional
  public void readAll(User user) {
    mapper.readAll(user.getId());
    Set<String> set = user.getReadSystemMessageIds();
    set = set == null ? new HashSet<>() : set;
    Set<String> ids = mapper.findAllSystemMessageId();
    if (!set.containsAll(ids)) {
      set.addAll(ids);
      user.setReadSystemMessageIds(set);
      userMapper.update(user);
    }
  }
}

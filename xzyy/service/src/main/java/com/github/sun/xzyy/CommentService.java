package com.github.sun.xzyy;

import com.github.sun.foundation.boot.exception.BadRequestException;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

@Service
public class CommentService {
  @Resource
  private CommentMapper mapper;
  @Resource
  private UserMapper userMapper;
  @Resource
  private GirlMapper girlMapper;
  @Resource(name = "mysql")
  private SqlBuilder.Factory factory;

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
    return id;
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

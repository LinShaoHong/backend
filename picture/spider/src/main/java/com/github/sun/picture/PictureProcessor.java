package com.github.sun.picture;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.spider.Spider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service("picture" + Spider.Processor.SUFFIX)
public class PictureProcessor implements Spider.Processor {
  @Override
  public void process(List<JsonNode> values) {
    System.out.println(JSON.serialize(values));
  }
}

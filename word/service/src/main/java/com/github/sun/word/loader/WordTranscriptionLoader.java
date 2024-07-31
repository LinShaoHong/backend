package com.github.sun.word.loader;

import com.github.sun.foundation.boot.utility.JSON;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;

@RefreshScope
@Service("transcription")
public class WordTranscriptionLoader extends WordBasicLoader {
  @Override
  public void load(String word, int userId) {
    retry(word, userId, dict -> {
      String q = loadQ("cues/音标.md");
      String resp = assistant.chat(apiKey, model, "直接给出单词" + word + "的英式音标和美式音标。" +
        "要求音标以/开头和结尾。");
      q = resp + "\n\n" + q;
      JSON.Valuer valuer = JSON.newValuer(parse(assistant.chat(apiKey, model, q)));
      valuer.get("uk_transcription").asArray().forEach(a -> dict.setUkTranscription(a.asText()));
      valuer.get("us_transcription").asArray().forEach(a -> dict.setUsTranscription(a.asText()));
    }, "ukTranscription", "usTranscription");
  }
}
package com.github.sun.word;

import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.word.loader.WordLoaderCheck;
import com.github.sun.word.loader.WordLoaderCheckMapper;
import com.github.sun.word.loader.WordLoaderCode;
import com.github.sun.word.loader.WordLoaderCodeMapper;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

@Service
@RefreshScope
public class WordCodeService {
    @Resource
    private WordLoaderCodeMapper mapper;
    @Resource
    private WordLoaderCheckMapper checkMapper;

    @Transactional
    public synchronized int genWordSort(int userId) {
        String date = Dates.format(new Date());
        long code = 1;
        String id = date + ":" + code;
        WordLoaderCode entity = mapper.queryForUpdate(id);
        if (entity != null) {
            code = entity.getCode() + 1;
            mapper.updateById(entity.getId(), code);
        } else {
            WordLoaderCode v = new WordLoaderCode();
            v.setId(id);
            v.setType(date);
            v.setCode(code);
            mapper.insert(v);

            WordLoaderCheck check = new WordLoaderCheck();
            check.setId(date + ":" + userId);
            check.setUserId(userId);
            check.setDate(date);
            check.setSort(1);
            checkMapper.insert(check);
        }
        return ((Long) code).intValue();
    }
}
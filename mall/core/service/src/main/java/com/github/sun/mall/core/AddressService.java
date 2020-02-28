package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.BasicService;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.mall.core.entity.Address;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService extends BasicService<String, Address, AddressMapper> {
  @Transactional
  public void insertOrUpdate(Address address) {
    if (address.isDefault()) {
      mapper.resetDefault(address.getUserId());
    }
    if (address.getId() == null) {
      address.setId(IdGenerator.next());
      mapper.insert(address);
    } else {
      mapper.update(address);
    }
  }
}

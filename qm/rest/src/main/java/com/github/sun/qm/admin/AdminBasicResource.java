package com.github.sun.qm.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.qm.Girl;
import com.github.sun.qm.GirlMapper;
import com.github.sun.qm.User;
import com.github.sun.qm.UserMapper;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AdminBasicResource extends AbstractResource {
    protected final UserMapper userMapper;
    protected final GirlMapper girlMapper;

    public AdminBasicResource(UserMapper userMapper, GirlMapper girlMapper) {
        this.userMapper = userMapper;
        this.girlMapper = girlMapper;
    }

    protected <T> List<ObjectNode> join(List<T> values, String... fields) {
        List<ObjectNode> list = new ArrayList<>();
        values.forEach(v -> list.add((ObjectNode) JSON.asJsonNode(v)));
        if (fields != null && fields.length > 0) {
            for (String field : fields) {
                if (field.endsWith("Id")) {
                    Set<String> ids = list.stream()
                            .map(v -> v.get(field))
                            .filter(Objects::nonNull)
                            .map(JsonNode::asText)
                            .filter(v -> v != null && !v.isEmpty())
                            .collect(Collectors.toSet());
                    Map<String, String> map = join(field, ids);
                    list.forEach(v -> {
                        JsonNode node = v.get(field);
                        if (node != null) {
                            String id = node.asText();
                            if (id != null && !id.isEmpty()) {
                                String name = field.substring(0, field.length() - 2) + "Name";
                                v.put(name, map.get(id));
                            }
                        }
                    });
                }
            }
        }
        return list;
    }

    private Map<String, String> join(String field, Set<String> ids) {
        if (!ids.isEmpty()) {
            if (field.equals("userId") || field.equals("commentatorId") || field.equals("replierId")) {
                return userMapper.findByIds(ids).stream()
                        .collect(Collectors.toMap(User::getId, u -> u.isVip() ? u.getUsername() + "(VIP)" : u.getUsername()));
            } else if (field.equals("girlId")) {
                return girlMapper.findByIds(ids).stream().collect(Collectors.toMap(Girl::getId, g -> {
                    String city = g.getCity();
                    if (city != null && !city.isEmpty()) {
                        int amount = g.getPrice() == null ? 0 : g.getPrice().intValue();
                        return city + " " + g.getName() + (amount > 0 ? ("(" + amount + ")") : "");
                    }
                    return g.getName();
                }));
            }
        }
        return new HashMap<>();
    }
}

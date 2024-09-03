package com.github.sun.card;

import com.github.sun.card.event.RoomEvent;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.sql.IdGenerator;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RefreshScope
@RequiredArgsConstructor
public class CardRoomService {
    private static final Map<String, Holder> holders = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(20);
    private final CardRoomMapper mapper;
    private final CardUserMapper userMapper;
    private final CardUserDefMapper defMapper;
    private final CardConfig config;

    //订阅
    public synchronized void sub(String mainUserId, String userId, boolean hks, SseEventSink sink, Sse sse) {
        if (sink.isClosed()) {
            return;
        }
        Holder holder = holders.get(mainUserId + ":" + hks);
        if (holder == null) {
            holder = new Holder(mainUserId);
            CardConfig.Card card;
            if (hks) {
                card = config.getHksCards().stream().filter(CardConfig.Card::isOpen).findFirst().orElse(null);
            } else {
                card = config.getLoverCards().stream().filter(CardConfig.Card::isOpen).findFirst().orElse(null);
            }
            if (card != null) {
                holder.cardType = card.getType();
            }
            holders.put(mainUserId + ":" + hks, holder);
        }
        holder.add(userId, hks, sink, sse);
    }

    public synchronized <T extends RoomEvent> void addEvent(T event) {
        Holder holder = holders.get(event.getMainUserId() + ":" + event.isHks());
        if (holder == null) {
            holder = new Holder(event.getMainUserId());
            holders.put(event.getMainUserId() + ":" + event.isHks(), holder);
        }
        holder.enqueue(event);
    }

    public class Holder {
        public final AtomicInteger running;
        public final String mainUserId;
        public final ConcurrentLinkedQueue<RoomEvent> queue;
        public final List<Client> clients;
        public Player player;
        private String cardType;
        public List<Chat> chats;

        public void setPlayer(Player player) {
            this.player = player;
        }

        public Holder(String mainUserId) {
            this.running = new AtomicInteger(0);
            this.mainUserId = mainUserId;
            this.queue = new ConcurrentLinkedQueue<>();
            this.clients = new ArrayList<>();
        }

        public <T extends RoomEvent> void enqueue(T event) {
            queue.offer(event);
            if (running.getAndIncrement() == 0) {
                executor.execute(this::process);
            }
        }

        public void process() {
            int c = 1;
            for (; ; ) {
                for (; ; ) {
                    RoomEvent event = queue.poll();
                    if (event == null) {
                        break;
                    } else {
                        new CopyOnWriteArrayList<>(this.clients).forEach(client -> {
                            try {
                                if (!client.getSink().isClosed()) {
                                    OutboundSseEvent e =
                                            client.getSse().newEventBuilder()
                                                    .name(event.getName())
                                                    .data(String.class, JSON.serialize(event))
                                                    .build();
                                    try {
                                        if ("NextEvent".equals(event.getName())) {
                                            Thread.sleep(150);
                                        } else if (!"OpenEvent".equals(event.getName())
                                                && !"ShuffleEvent".equals(event.getName())
                                                && !"CloseEvent".equals(event.getName())) {
                                            Thread.sleep(250);
                                        }
                                        if (!client.getSink().isClosed()) {
                                            client.getSink().send(e);
                                        }
                                    } catch (InterruptedException ex) {
                                        throw new RuntimeException(ex);
                                    }
                                }
                            } catch (Throwable ex) {
                                log.error("Event Error", ex);
                            }
                        });
                    }
                }
                if (running.addAndGet(-c) == 0) {
                    break;
                }
                c = running.get();
            }
        }

        public void add(String userId, boolean hks, SseEventSink sink, Sse sse) {
            Iterator<Client> it = this.clients.iterator();
            while (it.hasNext()) {
                Client client = it.next();
                if (Objects.equals(client.getUserId(), userId)) {
                    if (!client.getSink().isClosed()) {
                        client.getSink().close();
                    }
                    it.remove();
                    break;
                }
            }
            if (this.player == null) {
                CardUser player = userMapper.findById(userId);
                setPlayer(Player.from(mainUserId, player));
            }
            this.clients.add(Client.builder()
                    .userId(userId)
                    .sink(sink)
                    .sse(sse)
                    .time(new Date())
                    .build());
            CardRoomService.this.join(mainUserId, userId, hks, cardType);
        }

        public List<Chat> getChats() {
            if (chats == null) {
                chats = new ArrayList<>();
                return chats;
            }
            if (!chats.isEmpty()) {
                Set<String> ids = chats.stream()
                        .map(Chat::getUserId)
                        .collect(Collectors.toSet());
                Map<String, CardUser> map = userMapper.findByIds(ids).stream()
                        .collect(Collectors.toMap(CardUser::getId, v -> v));
                chats.forEach(chat -> {
                    CardUser user = map.get(chat.getUserId());
                    chat.setAvatar(user.getAvatar());
                    chat.setNickname(user.getNickname());
                    chat.setVip(user.getVip());
                });
            }
            return chats;
        }
    }

    @Data
    @Builder
    public static class Client {
        private String userId;
        private SseEventSink sink;
        private Sse sse;
        private Date time;
    }

    @Transactional
    public void join(String mainUserId, String userId, boolean hks, String cardType) {
        Set<String> ids = new HashSet<>();
        ids.add(mainUserId);
        ids.add(userId);
        Map<String, CardUser> users = userMapper.findByIds(ids).stream()
                .collect(Collectors.toMap(CardUser::getId, v -> v));
        if (ids.size() != users.size()) {
            throw new NotFoundException("找不到用户");
        }
        CardRoom room = mapper.byMainUserIdAndUserId(mainUserId, userId, hks);
        if (room == null) {
            room = new CardRoom();
            room.setId(IdGenerator.next());
            room.setMainUserId(mainUserId);
            room.setUserId(userId);
            room.setHks(hks);
            room.setEnterTime(new Date());
            mapper.insert(room);
        } else {
            room.setEnterTime(new Date());
            mapper.update(room);
        }
        CardUser user = users.get(userId);
        addEvent(RoomEvent.AddEvent.builder()
                .mainUserId(mainUserId)
                .userId(userId)
                .hks(hks)
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .vip(user.getVip())
                .cardType(cardType)
                .build());
    }

    public void shuffle(String mainUserId, String userId, boolean hks, String cardType) {
        addEvent(RoomEvent.ShuffleEvent.builder()
                .mainUserId(mainUserId)
                .userId(userId)
                .hks(hks)
                .total(total(mainUserId, cardType))
                .build());
    }

    public void open(String mainUserId, String userId, boolean hks, String cardType, int index, boolean music) {
        CardUserDef def = defMapper.byUserId(mainUserId);
        def.getDefs().stream().filter(v -> Objects.equals(v.getName(), cardType))
                .findAny().ifPresent(value -> {
                    List<CardUserDef.Item> items = value.getItems()
                            .stream()
                            .filter(CardUserDef.Item::isEnable)
                            .collect(Collectors.toList());
                    addEvent(RoomEvent.OpenEvent.builder()
                            .mainUserId(mainUserId)
                            .userId(userId)
                            .hks(hks)
                            .item(items.get(index - 1))
                            .index(index)
                            .music(music)
                            .build());
                });
    }

    public void close(String mainUserId, boolean hks) {
        addEvent(RoomEvent.CloseEvent.builder().mainUserId(mainUserId).hks(hks).build());
    }

    @SuppressWarnings("Duplicates")
    public void next(String mainUserId, boolean hks, String playerId) {
        Holder holder = holders.get(mainUserId + ":" + hks);
        if (holder != null) {
            List<Client> clients = new ArrayList<>(holder.clients);
            clients.removeIf(v -> v.getSink().isClosed());
            if (!clients.isEmpty()) {
                clients.sort(Comparator.comparing(Client::getTime));
                Client main = clients.stream().filter(v -> Objects.equals(v.getUserId(), mainUserId)).findFirst().orElse(null);
                if (main != null) {
                    clients.remove(main);
                    clients.add(0, main);
                }
                int j = -1;
                for (int i = 0; i < clients.size(); i++) {
                    Client client = clients.get(i);
                    if (Objects.equals(client.getUserId(), playerId)) {
                        j = i;
                        break;
                    }
                }
                j += 1;
                Client client = clients.get(j % clients.size());
                String userId = client.getUserId();
                CardUser player = userMapper.findById(userId);
                holder.setPlayer(Player.from(mainUserId, player));
                addEvent(RoomEvent.NextEvent.builder()
                        .mainUserId(mainUserId)
                        .userId(player.getId())
                        .hks(hks)
                        .nickname(player.getNickname())
                        .avatar(player.getAvatar())
                        .vip(player.getVip())
                        .build());
            }
        }
    }

    public void leave(String mainUserId, boolean hks, String userId) {
        Holder holder = holders.get(mainUserId + ":" + hks);
        if (holder != null) {
            if (holder.player != null && Objects.equals(holder.player.getUserId(), userId)) {
                next(mainUserId, hks, userId);
            }

            Iterator<Client> it = holder.clients.iterator();
            while (it.hasNext()) {
                Client client = it.next();
                if (Objects.equals(client.getUserId(), userId)) {
                    SseEventSink sink = client.getSink();
                    if (!sink.isClosed()) {
                        sink.close();
                    }
                    it.remove();
                    break;
                }
            }
            if (holder.clients.isEmpty()) {
                holders.remove(mainUserId + ":" + hks);
            } else {
                addEvent(RoomEvent.LeaveEvent.builder()
                        .mainUserId(mainUserId)
                        .userId(userId)
                        .hks(hks)
                        .build());
            }
        }
    }

    public void assign(String mainUserId, boolean hks, String userId) {
        Holder holder = holders.get(mainUserId + ":" + hks);
        if (holder != null) {
            holder.clients.forEach(client -> {
                if (!client.getSink().isClosed() && Objects.equals(client.getUserId(), userId)) {
                    CardUser player = userMapper.findById(userId);
                    if (player != null) {
                        holder.setPlayer(Player.from(mainUserId, player));
                        addEvent(RoomEvent.NextEvent.builder()
                                .mainUserId(mainUserId)
                                .userId(player.getId())
                                .hks(hks)
                                .nickname(player.getNickname())
                                .avatar(player.getAvatar())
                                .vip(player.getVip())
                                .build());
                    }
                }
            });
        }
    }

    public void changeCardType(String mainUserId, String cardType, boolean hks) {
        Holder holder = holders.get(mainUserId + ":" + hks);
        if (holder != null) {
            holder.cardType = cardType;
            holder.clients.forEach(client -> {
                if (!client.getSink().isClosed()) {
                    addEvent(RoomEvent.ChangeCardTypeEvent.builder()
                            .mainUserId(mainUserId)
                            .hks(hks)
                            .cardType(cardType)
                            .build());
                }
            });
        }
    }

    public String reply(String mainUserId, String userId, String message, boolean hks) {
        Holder holder = holders.get(mainUserId + ":" + hks);
        if (holder != null) {
            if (holder.chats == null) {
                holder.chats = new ArrayList<>();
            }
            String chatId = IdGenerator.next();
            holder.chats.add(Chat.builder()
                    .id(chatId)
                    .userId(userId)
                    .message(message)
                    .build());
            addEvent(RoomEvent.ReceiveReplyEvent.builder()
                    .mainUserId(mainUserId)
                    .userId(userId)
                    .chatId(chatId)
                    .hks(hks)
                    .build());
            return chatId;
        }
        return null;
    }

    public void withdrawReply(String mainUserId, String userId, String chatId, boolean hks) {
        Holder holder = holders.get(mainUserId + ":" + hks);
        if (holder != null) {
            List<Chat> chats = holder.getChats();
            chats.removeIf(c -> Objects.equals(c.getId(), chatId));
            addEvent(RoomEvent.ReceiveReplyEvent.builder()
                    .mainUserId(mainUserId)
                    .userId(userId)
                    .chatId(chatId)
                    .withdraw(true)
                    .hks(hks)
                    .build());
        }
    }

    public Chat byReplyId(String mainUserId, String chatId, boolean hks) {
        Holder holder = holders.get(mainUserId + ":" + hks);
        if (holder != null) {
            return holder.getChats().stream()
                    .filter(v -> Objects.equals(v.getId(), chatId))
                    .findFirst().orElse(null);
        }
        return null;
    }

    public List<Chat> replies(String mainUserId, boolean hks) {
        Holder holder = holders.get(mainUserId + ":" + hks);
        if (holder != null) {
            return holder.getChats();
        }
        return Collections.emptyList();
    }

    public List<Player> players(String mainUserId, boolean hks) {
        Holder holder = holders.get(mainUserId + ":" + hks);
        if (holder != null) {
            Set<String> userIds = holder.clients.stream().filter(v -> !v.getSink().isClosed())
                    .map(Client::getUserId).collect(Collectors.toSet());
            if (!userIds.isEmpty()) {
                List<CardUser> users = userMapper.findByIds(userIds);
                List<Player> players = users.stream()
                        .map(v -> Player.from(mainUserId, v))
                        .collect(Collectors.toList());
                Map<String, Client> map = holder.clients.stream().collect(Collectors.toMap(Client::getUserId, v -> v));
                players.sort((p1, p2) -> {
                    Client client1 = map.get(p1.getUserId());
                    Client client2 = map.get(p2.getUserId());
                    return (client1 == null ? new Date() : client1.getTime())
                            .compareTo((client2 == null ? new Date() : client2.getTime()));
                });
                Player main = players.stream().filter(v -> Objects.equals(v.getUserId(), mainUserId)).findFirst().orElse(null);
                if (main != null) {
                    players.remove(main);
                    players.add(0, main);
                }
                return players;
            }
        }
        return Collections.emptyList();
    }

    public int total(String mainUserId, String cardType) {
        CardUserDef def = defMapper.byUserId(mainUserId);
        CardUserDef.Def value = def.getDefs().stream().filter(v -> Objects.equals(v.getName(), cardType))
                .findAny()
                .orElse(null);
        long total = value == null ? 0L : value.getItems().stream().filter(CardUserDef.Item::isEnable).count();
        return ((Long) total).intValue();
    }

    @SuppressWarnings("Duplicates")
    public Player player(String mainUserId, boolean hks) {
        Holder holder = holders.get(mainUserId + ":" + hks);
        Player player;
        if (holder != null) {
            player = holder.player;
            List<Client> clients = new ArrayList<>(holder.clients);
            clients.removeIf(v -> v.getSink().isClosed());
            clients.sort(Comparator.comparing(Client::getTime));
            Client main = clients.stream().filter(v -> Objects.equals(v.getUserId(), mainUserId)).findFirst().orElse(null);
            if (main != null) {
                clients.remove(main);
                clients.add(0, main);
            }
            if (!clients.isEmpty()) {
                if (player == null) {
                    CardUser user = userMapper.findById(clients.get(0).getUserId());
                    player = Player.from(mainUserId, user);
                } else {
                    String playerId = player.getUserId();
                    if (clients.stream().noneMatch(v -> Objects.equals(v.getUserId(), playerId))) {
                        CardUser user = userMapper.findById(clients.get(0).getUserId());
                        player = Player.from(mainUserId, user);
                    }
                }
            } else {
                CardUser user = userMapper.findById(mainUserId);
                player = Player.from(mainUserId, user);
            }
            holder.setPlayer(player);
        } else {
            CardUser user = userMapper.findById(mainUserId);
            player = Player.from(mainUserId, user);
        }
        return player;
    }

    @Data
    @Builder
    public static class Player {
        private String mainUserId;
        private String userId;
        private String nickname;
        private int avatar;
        private int vip;

        public static Player from(String mainUserId, CardUser user) {
            return Player.builder()
                    .mainUserId(mainUserId)
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .avatar(user.getAvatar())
                    .vip(user.getVip())
                    .build();
        }
    }

    public List<JoinedRoom> joined(String userId, boolean hks) {
        List<CardRoom> joined = mapper.joined(userId, hks);
        Set<String> mainUserIds = joined.stream().map(CardRoom::getMainUserId).collect(Collectors.toSet());
        if (!mainUserIds.isEmpty()) {
            Map<String, CardUser> users = userMapper.findByIds(mainUserIds).stream()
                    .collect(Collectors.toMap(CardUser::getId, v -> v));
            return joined.stream()
                    .map(v -> {
                        CardUser user = users.get(v.getMainUserId());
                        return JoinedRoom.builder()
                                .id(v.getId())
                                .mainUserId(v.getMainUserId())
                                .nickname(user.getNickname())
                                .avatar(user.getAvatar())
                                .vip(user.getVip())
                                .time(Dates.simpleTime(v.getEnterTime()))
                                .build();
                    })
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Transactional
    public void remove(String id) {
        mapper.remove(id);
    }

    @Data
    @Builder
    public static class JoinedRoom {
        private String id;
        private String mainUserId;
        private String nickname;
        private int avatar;
        private int vip;
        private String time;
    }

    @Data
    @Builder
    public static class Chat {
        private String id;
        private String userId;
        private String nickname;
        private int avatar;
        private int vip;
        private String message;
    }
}
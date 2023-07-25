package cordori.dpstamina.listener;

import cordori.dpstamina.DPStamina;
import cordori.dpstamina.file.ConfigManager;
import cordori.dpstamina.hook.PAPIHook;
import cordori.dpstamina.utils.LogInfo;
import cordori.dpstamina.utils.PlayerData;
import cordori.dpstamina.utils.StaminaGroup;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.serverct.ersha.dungeon.common.api.event.DungeonEvent;
import org.serverct.ersha.dungeon.common.api.event.dungeon.DungeonStartEvent;
import org.serverct.ersha.dungeon.common.team.type.PlayerStateType;

import java.util.*;


public class DPSEvent implements Listener {

    private final HashMap<Player, ItemStack> consumeMap = new HashMap<>();
    private static final DPStamina dps = DPStamina.getInstance();

    /**
     *
     * @param player 玩家
     * @param itemName 要检查的门票名
     * @return 是否拥有该门票，有就返回false，没有返回true
     */
    public boolean noItem(Player player, String itemName) {
        Inventory inventory = player.getInventory();
        for (ItemStack item : inventory) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    && item.getItemMeta().getDisplayName().equals(itemName)) {
                consumeMap.put(player, item);
                return false;
            }
        }
        return true;
    }

    /**
     *
     * @param player 玩家
     * @param dungeonName 地牢名
     * @return 检查玩家体力是否足够
     */
    private boolean noEnoughStamina(Player player, String dungeonName) {
        // 检查体力是否足够
        double stamina = PlayerData.dataHashMap.get(player.getUniqueId()).getStamina();
        double cost = ConfigManager.defaultCost;
        if(ConfigManager.mapCost.containsKey(dungeonName)) {
            cost = Double.parseDouble(PAPIHook.onPAPIProcess(player, ConfigManager.mapCost.get(dungeonName)));
        }
        return stamina < cost;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerEnterDP(DungeonEvent event) {
        long currentTime = System.currentTimeMillis();
        String dungeonName = event.getDungeon().getDungeonName();
        List<Player> playerList = event.getDungeon().getTeam().getPlayers(PlayerStateType.ALL);

        String particularTicket = null;
        if(ConfigManager.ticketNamesMap.containsKey(dungeonName)) {
            particularTicket = ConfigManager.ticketNamesMap.get(dungeonName);
        }

        String defaultTicket = ConfigManager.defaultTicket;
        List<String> failJoinList = new ArrayList<>();

        // 加入地牢之前判断体力与门票
        if(event.getEvent() instanceof DungeonStartEvent.Before) {

            for(Player player : playerList) {

                String playerName = player.getName();

                // 先检查该副本是否有写门票配置
                if(particularTicket == null) {
                    // 如果没写特定门票，检查是否有通用门票或检查体力，体力不足就加入到failJoinList里
                    if(noItem(player, defaultTicket) && noEnoughStamina(player, dungeonName)) {
                        failJoinList.add(playerName);
                    }
                } else {
                    // 如果没有特定门票和通用门票并且体力不够
                    if (noItem(player, particularTicket) && noItem(player, defaultTicket) &&
                            noEnoughStamina(player, dungeonName)) {
                        failJoinList.add(playerName);
                    }
                }
            }

            if (!failJoinList.isEmpty()) {
                if(ConfigManager.messagesHashMap.containsKey("failEnter")) {
                    // 拼接体力不足的玩家名字
                    StringBuilder sb = new StringBuilder(ConfigManager.messagesHashMap.get("failEnter"));
                    for (String name : failJoinList) {
                        sb.append(name).append(", ");
                    }
                    sb.setLength(sb.length() - 2);
                    event.getDungeon().sendGroupMessage(ConfigManager.prefix + sb);
                }

                event.setCancelled(true);
            }
        }

        // 加入地牢之后扣除体力
        if(event.getEvent() instanceof DungeonStartEvent.After) {

            for(Player player : playerList) {
                ItemStack ticketItem = consumeMap.get(player);
                // 如果是有门票
                if(ticketItem != null && ticketItem.getType() != Material.AIR) {
                    String ticketName = ticketItem.getItemMeta().getDisplayName();
                    if(ticketItem.getAmount() >= 1) {
                        ticketItem.setAmount(ticketItem.getAmount() - 1);
                    }
                    if(ConfigManager.messagesHashMap.containsKey("consume")) {
                        if(ConfigManager.mapNamesMap.containsKey(dungeonName)) {
                            dungeonName = ConfigManager.mapNamesMap.get(dungeonName);
                        }
                        player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap
                                .get("consume")
                                .replaceFirst("%ticket%", ticketName)
                                .replaceFirst("%dungeon%", dungeonName)
                        );
                    }
                } else {
                    double stamina = PlayerData.dataHashMap.get(player.getUniqueId()).getStamina();
                    double cost = ConfigManager.defaultCost;
                    if(ConfigManager.mapCost.containsKey(dungeonName)) {
                        cost = Double.parseDouble(PAPIHook.onPAPIProcess(player, ConfigManager.mapCost.get(dungeonName)));
                    }

                    double newStamina = stamina - cost;

                    PlayerData.dataHashMap.get(player.getUniqueId()).setStamina(newStamina);
                    if(ConfigManager.messagesHashMap.containsKey("cost")) {
                        if(ConfigManager.mapNamesMap.containsKey(dungeonName)) {
                            dungeonName = ConfigManager.mapNamesMap.get(dungeonName);
                        }
                        player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("cost")
                                .replaceAll("%cost%", String.valueOf(cost))
                                .replaceAll("%dungeon%", dungeonName)
                                .replaceAll("%stamina%", String.valueOf(newStamina))
                        );
                    }
                }
            }
        }

        if(ConfigManager.debug) {
            long finishTime = System.currentTimeMillis();
            long useTime = finishTime - currentTime;
            LogInfo.debug("进入副本用时" + useTime/1000 + "ms");
        }

    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(dps, () -> {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            List<Object> objectsList = DPStamina.sql.getList(String.valueOf(uuid));

            // 如果数据不存在，插入默认数据
            if(objectsList == null) {
                double stamina = dps.getConfig().getDouble("group.default.limit");
                PlayerData playerData = new PlayerData("default", stamina);
                PlayerData.dataHashMap.put(uuid, playerData);
                DPStamina.sql.insert(String.valueOf(uuid));
                return;
            }

            String staminaGroup = (String) objectsList.get(0);
            double stamina = (double) objectsList.get(1);

            // 如果开启了离线回复，计算回复量后存入PlayerData
            if(ConfigManager.offline) {
                long lastTime = (long) objectsList.get(2);
                double limit = StaminaGroup.groupHashMap.get(staminaGroup).getLimit();
                double recover = Double.parseDouble(PAPIHook.onPAPIProcess(player, StaminaGroup.groupHashMap.get(staminaGroup).getRecover()));
                long currentTime = System.currentTimeMillis();
                long timeDiffMinutes = (currentTime - lastTime) / (1000L * 60L * ConfigManager.minutes);
                double timeRecover = timeDiffMinutes * recover;
                double recoveredStamina = stamina + timeRecover;
                if (recoveredStamina > limit) {
                    recoveredStamina = limit;
                    timeRecover = Math.max(limit - stamina, 0);
                }

                //  玩家进入时从数据库读取数据，用playerData存储
                PlayerData.dataHashMap.put(uuid, new PlayerData(staminaGroup, recoveredStamina));
                if(ConfigManager.messagesHashMap.containsKey("join")) {
                    player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("join")
                            .replaceAll("%min%", String.valueOf(timeDiffMinutes))
                            .replaceAll("%num%", String.valueOf(timeRecover))
                            .replaceAll("%stamina%", String.valueOf(recoveredStamina))
                    );
                }

            } else {
                // 如果没开离线回复，直接获取数据库数据并存入PlayerData
                PlayerData.dataHashMap.put(uuid, new PlayerData(staminaGroup, stamina));
            }

            // 判断是否需要进行每日刷新
            if(ConfigManager.refresh) {
                DPStamina.sql.insertDate(player);
            }

        }, 50L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(dps, () -> {

            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();

            //玩家退出时把 PlayerData 的玩家数据同步到数据库中
            PlayerData playerData = PlayerData.dataHashMap.get(uuid);
            double stamina = playerData.getStamina();
            String staminaGroup = playerData.getStaminaGroup();

            DPStamina.sql.updateAll(String.valueOf(uuid), stamina, staminaGroup);
            PlayerData.dataHashMap.remove(uuid);
        });
    }

}

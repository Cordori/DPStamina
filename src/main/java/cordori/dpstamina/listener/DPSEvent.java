package cordori.dpstamina.listener;

import cordori.dpstamina.DPStamina;
import cordori.dpstamina.file.ConfigManager;
import cordori.dpstamina.hook.PAPIHook;
import cordori.dpstamina.utils.PlayerData;
import cordori.dpstamina.utils.StaminaGroup;
import org.bukkit.Bukkit;
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
    private ItemStack ticketItem;

    private static final DPStamina dps = DPStamina.getInstance();

    public boolean hasItem(Player player, String itemName) {
        Inventory inventory = player.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    && item.getItemMeta().getDisplayName().equals(itemName)) {
                ticketItem = item;
                return false;
            }
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerEnterDP(DungeonEvent event) {
        String dungeonName = event.getDungeon().getDungeonName();
        List<Player> playerList = event.getDungeon().getTeam().getPlayers(PlayerStateType.ALL);
        String particularTicket = ConfigManager.particularTicket.replaceFirst("%dungeon%", dungeonName);
        if(ConfigManager.debug) System.out.println("特定门票名称为: " + particularTicket);
        String defaultTicket = ConfigManager.defaultTicket;

        // 加入地牢之前判断体力与门票
        if(event.getEvent() instanceof DungeonStartEvent.Before) {
            List<Player> failJoinList = new ArrayList<>();
            List<Player> noTicketList = new ArrayList<>();

            for(Player player : playerList) {
                double stamina = PlayerData.dataHashMap.get(player.getUniqueId()).getStamina();
                double cost = ConfigManager.defaultCost;
                if(ConfigManager.mapCost.containsKey(dungeonName)) {
                    cost = Double.parseDouble(PAPIHook.onPAPIProcess(player, ConfigManager.mapCost.get(dungeonName)));
                }
                if(stamina < cost) failJoinList.add(player);
                // 如果开启了门票功能
                if(ConfigManager.ticket) {
                    // 如果没有通用门票或特定门票
                    if(hasItem(player, particularTicket) && hasItem(player, defaultTicket)) {
                        noTicketList.add(player);
                    }
                }
            }

            if (!failJoinList.isEmpty() || !noTicketList.isEmpty()) {
                if(!failJoinList.isEmpty()) {
                    if(ConfigManager.messagesHashMap.containsKey("failEnter")) {
                        // 拼接体力不足的玩家名字
                        StringBuilder sb = new StringBuilder(ConfigManager.messagesHashMap.get("failEnter"));
                        for (Player player : failJoinList) {
                            sb.append(player.getName()).append(", ");
                        }
                        sb.setLength(sb.length() - 2);
                        event.getDungeon().sendGroupMessage(ConfigManager.prefix + sb);
                    }
                }

                if(!noTicketList.isEmpty()) {
                    if(ConfigManager.messagesHashMap.containsKey("noTicket")) {
                        // 拼接没有门票的玩家名字
                        StringBuilder sb = new StringBuilder(ConfigManager.messagesHashMap.get("noTicket")
                                .replaceFirst("%dungeon%", dungeonName));
                        for (Player player : noTicketList) {
                            sb.append(player.getName()).append(", ");
                        }
                        sb.setLength(sb.length() - 2);
                        event.getDungeon().sendGroupMessage(ConfigManager.prefix + sb);
                    }
                }
                event.setCancelled(true);
            }
        }

        // 加入地牢之后扣除体力
        if(event.getEvent() instanceof DungeonStartEvent.After) {

            for(Player player : playerList) {
                double stamina = PlayerData.dataHashMap.get(player.getUniqueId()).getStamina();
                double cost = ConfigManager.defaultCost;
                if(ConfigManager.mapCost.containsKey(dungeonName)) {
                    cost = Double.parseDouble(PAPIHook.onPAPIProcess(player, ConfigManager.mapCost.get(dungeonName)));
                }

                // 如果开启了门票功能
                if(ConfigManager.ticket) {

                    String ticketName = ticketItem.getItemMeta().getDisplayName();
                    if(ticketItem.getAmount() >= 1) {
                        ticketItem.setAmount(ticketItem.getAmount() - 1);
                    }
                    if(ConfigManager.messagesHashMap.containsKey("consume")) {
                        player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap
                                .get("consume")
                                .replaceFirst("%ticket%", ticketName)
                                .replaceFirst("%dungeon%", dungeonName)
                        );
                    }
                }
                double newStamina = stamina - cost;

                PlayerData.dataHashMap.get(player.getUniqueId()).setStamina(newStamina);
                if(ConfigManager.messagesHashMap.containsKey("cost")) {
                    player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("cost")
                            .replaceAll("%cost%", String.valueOf(cost))
                            .replaceAll("%dungeon%", dungeonName)
                            .replaceAll("%stamina%", String.valueOf(newStamina))
                    );
                }

            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(dps, () -> {
            Player player = event.getPlayer();
            UUID uuid = player.getUniqueId();
            List<Object> objectsList = DPStamina.sql.getList(String.valueOf(uuid));

            // 如果数据不存在，插入默认数据
            if(objectsList == null) {
                PlayerData playerData = new PlayerData("default", 100.0);
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
        });
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

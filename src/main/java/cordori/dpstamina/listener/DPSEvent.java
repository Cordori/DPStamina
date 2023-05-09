package cordori.dpstamina.listener;

import cordori.dpstamina.DPStamina;
import cordori.dpstamina.file.ConfigManager;
import cordori.dpstamina.utils.PlayerData;
import cordori.dpstamina.utils.StaminaGroup;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.serverct.ersha.dungeon.common.api.event.DungeonEvent;
import org.serverct.ersha.dungeon.common.api.event.dungeon.DungeonStartEvent;
import org.serverct.ersha.dungeon.common.team.type.PlayerStateType;

import java.util.*;


public class DPSEvent implements Listener {

    private static final DPStamina dps = DPStamina.getInstance();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerEnterDP(DungeonEvent event) {
        List<Player> playerList = event.getDungeon().getTeam().getPlayers(PlayerStateType.ALL);

        // 加入地牢之前判断体力
        if(event.getEvent() instanceof DungeonStartEvent.Before) {
            List<Player> failJoinList = new ArrayList<>();

            for(Player player : playerList) {
                double stamina = PlayerData.HashMap.get(player).getStamina();
                if(stamina < ConfigManager.cost) failJoinList.add(player);
            }

            if (!failJoinList.isEmpty()) {
                // 拼接体力不足的玩家名字
                StringBuilder sb = new StringBuilder(ConfigManager.messagesHashMap.get("failJoin"));
                for (Player player : failJoinList) {
                    sb.append(player.getName()).append(", ");
                }
                sb.setLength(sb.length() - 2);
                event.getDungeon().sendGroupMessage(ConfigManager.prefix + sb);
                event.setCancelled(true);
            }
        }

        // 加入地牢之后扣除体力
        if(event.getEvent() instanceof DungeonStartEvent.After) {
            for(Player player : playerList) {
                double stamina = PlayerData.HashMap.get(player).getStamina();
                double newStamina = stamina - ConfigManager.cost;
                String dungeonName = event.getDungeon().getDungeonName();

                PlayerData.HashMap.get(player).setStamina(newStamina);

                player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("cost")
                        .replaceAll("%cost%", String.valueOf(ConfigManager.cost))
                        .replaceAll("%dungeon%", dungeonName)
                        .replaceAll("%stamina%", String.valueOf(newStamina))
                );
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(dps, () -> {
            Player player = event.getPlayer();
            String uuid = player.getUniqueId().toString();
            List<Object> objectsList = DPStamina.sql.getList(uuid);

            // 如果数据不存在，插入默认数据
            if(objectsList == null) {
                PlayerData playerData = new PlayerData("default", 100.0);
                PlayerData.HashMap.put(player, playerData);
                DPStamina.sql.insert(uuid);
                return;
            }

            String staminaGroup = (String) objectsList.get(0);
            double stamina = (double) objectsList.get(1);

            // 如果开启了离线回复，计算回复量后存入PlayerData
            if(ConfigManager.offline) {
                long lastTime = (long) objectsList.get(2);
                double limit = StaminaGroup.HashMap.get(staminaGroup).getLimit();
                double recover = Double.parseDouble(PlaceholderAPI.setPlaceholders(player, StaminaGroup.HashMap.get(staminaGroup).getRecover()));
                long currentTime = System.currentTimeMillis();
                long timeDiffMinutes = (currentTime - lastTime) / (1000L * 60L * ConfigManager.minutes);
                double timeRecover = timeDiffMinutes * recover;
                double recoveredStamina = stamina + timeRecover;
                if (recoveredStamina > limit) {
                    recoveredStamina = limit;
                    timeRecover = Math.max(limit - stamina, 0);
                }

                //  玩家进入时数据库读取数据，用playerData存储
                PlayerData playerData = new PlayerData(staminaGroup, recoveredStamina);
                PlayerData.HashMap.put(player, playerData);

                player.sendMessage(ConfigManager.prefix + ConfigManager.messagesHashMap.get("join")
                        .replaceAll("%min%", String.valueOf(timeDiffMinutes))
                        .replaceAll("%num%", String.valueOf(timeRecover))
                        .replaceAll("%stamina%", String.valueOf(recoveredStamina))
                );
            } else {
                // 如果没开离线回复，直接获取数据库数据并存入PlayerData
                PlayerData playerData = new PlayerData(staminaGroup, stamina);
                PlayerData.HashMap.put(player, playerData);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTaskAsynchronously(dps, () -> {
            Player player = event.getPlayer();
            String uuid = player.getUniqueId().toString();
            //玩家退出时把 PlayerData 的玩家数据同步到数据库中
            double stamina = PlayerData.HashMap.get(player).getStamina();
            String staminaGroup = PlayerData.HashMap.get(player).getStaminaGroup();
            DPStamina.sql.updateAll(uuid, stamina, staminaGroup);
        });
    }

}

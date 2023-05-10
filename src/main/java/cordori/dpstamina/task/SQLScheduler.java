package cordori.dpstamina.task;

import cordori.dpstamina.DPStamina;
import cordori.dpstamina.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class SQLScheduler implements Runnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            PlayerData playerData = PlayerData.dataHashMap.get(uuid);
            double stamina = playerData.getStamina();
            String group = playerData.getStaminaGroup();

            DPStamina.sql.updateAll(String.valueOf(uuid), stamina, group);

        }
    }
}

package cordori.dpstamina.task;

import cordori.dpstamina.DPStamina;
import cordori.dpstamina.utils.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class SQLScheduler implements Runnable {
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            PlayerData playerData = PlayerData.HashMap.get(player);
            double stamina = playerData.getStamina();
            String group = playerData.getStaminaGroup();
            String uuid = player.getUniqueId().toString();

            DPStamina.sql.updateAll(uuid, stamina, group);

        }
    }
}

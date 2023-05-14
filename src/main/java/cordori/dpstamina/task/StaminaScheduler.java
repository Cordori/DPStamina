package cordori.dpstamina.task;

import cordori.dpstamina.file.ConfigManager;
import cordori.dpstamina.hook.PAPIHook;
import cordori.dpstamina.utils.PlayerData;
import cordori.dpstamina.utils.Region;
import cordori.dpstamina.utils.StaminaGroup;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class StaminaScheduler implements Runnable {

    @Override
    public void run() {

        if(ConfigManager.regionRecover && !Region.regionHashMap.isEmpty()) {

            for (Player player : Bukkit.getOnlinePlayers()) {
                if(!Region.isPlayerInRegion(player)) continue;
                staminaRecover(player);
            }

        } else {

            for (Player player : Bukkit.getOnlinePlayers()) {
                staminaRecover(player);
            }
        }
    }

    private void staminaRecover(Player player) {

        UUID uuid = player.getUniqueId();
        PlayerData playerData = PlayerData.dataHashMap.get(uuid);
        double stamina = playerData.getStamina();
        String group = playerData.getStaminaGroup();

        StaminaGroup staminaGroup = StaminaGroup.groupHashMap.get(group);
        double limit = staminaGroup.getLimit();
        double recover = Double.parseDouble(PAPIHook.onPAPIProcess(player, staminaGroup.getRecover()));

        // 如果体力已经满了则不需要恢复
        if (stamina >= limit) {
            return;
        }

        // 计算体力恢复量
        double recoveredStamina = stamina + recover;
        if (recoveredStamina > limit) {
            recoveredStamina = limit;
        }

        // 更新 PlayerData 中玩家的体力值
        playerData.setStamina(recoveredStamina);
    }
}


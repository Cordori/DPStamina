package cordori.dpstamina.task;

import cordori.dpstamina.utils.PlayerData;
import cordori.dpstamina.utils.StaminaGroup;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class StaminaScheduler implements Runnable {

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            PlayerData playerData = PlayerData.HashMap.get(player);
            double stamina = playerData.getStamina();
            String group = playerData.getStaminaGroup();

            StaminaGroup staminaGroup = StaminaGroup.HashMap.get(group);
            double limit = staminaGroup.getLimit();
            double recover = Double.parseDouble(PlaceholderAPI.setPlaceholders(player, staminaGroup.getRecover()));
            // 如果体力已经满了则不需要恢复
            if (stamina >= limit) {
                continue;
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
}


package cordori.dpstamina.utils;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.HashMap;

@Getter @Setter
public class PlayerData {

    private String staminaGroup;
    private Double stamina;

    public static HashMap<Player, PlayerData> HashMap = new HashMap<>();

    public PlayerData(String staminaGroup, double stamina) {
        this.staminaGroup = staminaGroup;
        this.stamina = stamina;
    }

}

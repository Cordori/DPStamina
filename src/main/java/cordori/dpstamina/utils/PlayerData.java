package cordori.dpstamina.utils;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.UUID;

@Getter @Setter
public class PlayerData {

    private String staminaGroup;
    private Double stamina;

    public static HashMap<UUID, PlayerData> dataHashMap = new HashMap<>();

    public PlayerData(String staminaGroup, double stamina) {
        this.staminaGroup = staminaGroup;
        this.stamina = stamina;
    }

}

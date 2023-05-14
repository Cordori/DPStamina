package cordori.dpstamina.hook;


import cordori.dpstamina.DPStamina;
import cordori.dpstamina.file.ConfigManager;
import cordori.dpstamina.utils.PlayerData;
import cordori.dpstamina.utils.StaminaGroup;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.UUID;


public class PAPIHook extends PlaceholderExpansion {
    private final DPStamina dps;

    public PAPIHook(DPStamina dps) {
        this.dps = dps;
    }

    @Override
    public boolean persist() {
        return true;
    }
    @Override
    public boolean canRegister() {
        return true;
    }
    @Override
    public String getAuthor() {
        return "Cordori";
    }
    @Override
    public String getVersion() {
        return dps.getDescription().getVersion();
    }
    @Override
    public String getIdentifier() {
        return "DPStamina";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {

        if (player == null || identifier == null || identifier.isEmpty()) return null;
        UUID uuid = player.getUniqueId();
        if (identifier.equalsIgnoreCase("stamina")) {
            if(PlayerData.dataHashMap.containsKey(uuid)) {
                return String.valueOf(PlayerData.dataHashMap.get(uuid).getStamina());
            } else return "0";
        }

        else if (identifier.equalsIgnoreCase("recover")) {
            if(PlayerData.dataHashMap.containsKey(uuid)) {
                String group = PlayerData.dataHashMap.get(uuid).getStaminaGroup();
                if(StaminaGroup.groupHashMap.containsKey(group)) {
                    return PlaceholderAPI.setPlaceholders(player, StaminaGroup.groupHashMap.get(group).getRecover());
                } else return "1";
            } else return "1";
        }

        else if (identifier.equalsIgnoreCase("group")) {
            if(PlayerData.dataHashMap.containsKey(uuid)) {
                return PlayerData.dataHashMap.get(uuid).getStaminaGroup();
            } else return "default";
        }

        else if (identifier.equalsIgnoreCase("groupLimit")) {
            if(PlayerData.dataHashMap.containsKey(uuid)) {
                String group = PlayerData.dataHashMap.get(uuid).getStaminaGroup();
                if(StaminaGroup.groupHashMap.containsKey(group)) {
                    return String.valueOf(StaminaGroup.groupHashMap.get(group).getLimit());
                } else return "100";

            } else return "100";
        }

        else if (identifier.startsWith("mapCost_")) {
            String mapName = identifier.substring(8);
            if(ConfigManager.mapCost.containsKey(mapName)) {
                String cost = ConfigManager.mapCost.get(mapName);
                return PlaceholderAPI.setPlaceholders(player, cost);
            } else return "10";
        }

        return null;
    }

    public static String onPAPIProcess(Player player, String str) {
        return PlaceholderAPI.setPlaceholders(player, str);
    }

}

package cordori.dpstamina.hook;


import cordori.dpstamina.DPStamina;
import cordori.dpstamina.utils.PlayerData;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;


public class PAPIHook extends PlaceholderExpansion {
    private final DPStamina dps;

    public PAPIHook(DPStamina dps) {
        this.dps = dps;
    }
    //持久化
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

        if (identifier.equals("stamina")) {
            return String.valueOf(PlayerData.HashMap.get(player).getStamina());
        } else if (identifier.equals("group")) {
            return PlayerData.HashMap.get(player).getStaminaGroup();
        }

        return null;
    }

}

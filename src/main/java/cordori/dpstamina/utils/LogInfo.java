package cordori.dpstamina.utils;

import cordori.dpstamina.DPStamina;

public class LogInfo {
    public static void debug(String str) {
        DPStamina.getInstance().getLogger().info(str);
    }
}

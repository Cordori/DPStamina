package cordori.dpstamina.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;

@Getter @RequiredArgsConstructor
public class StaminaGroup {
    private final double limit;
    private final String recover;

    public static HashMap<String, StaminaGroup> HashMap = new HashMap<>();
}

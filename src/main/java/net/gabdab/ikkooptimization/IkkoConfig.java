package net.gabdab.ikkooptimization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class IkkoConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("ikkooptimization.json");

    public static IkkoConfig I = new IkkoConfig();

    // ---- Основне ----
    public boolean enabled = true;
    public int mode = 0;                 // 0 = Феєрверк, 1 = Заряд вітру
    public int doubleTapWindow = 7;      // тіки між двома натисками пробілу
    public boolean requireAirborne = true;
    public boolean allowInWater = false;
    public boolean showMessages = false;
    public int maxSequenceTicks = 60;

    // ---- Елітри ----
    public int equipDelay = 0;
    public int takeoffDelay = 0;
    public int minDurability = 5;
    public boolean hopOnTakeoff = true;
    public double hopStrength = 0.42;
    public boolean restoreChestplate = true;
    public int restoreDelay = 2;

    // ---- Феєрверк ----
    public int fwSelectDelay = 0;
    public int fwUseDelay = 0;
    public int fwCount = 1;
    public int fwInterval = 5;
    public int fwPreferredSlot = 0;      // 0 = авто, 1..9 = конкретний слот хотбара
    public boolean swingHand = true;
    public boolean returnSlot = true;
    public int returnDelay = 1;

    // ---- Заряд вітру ----
    public int lookSpeed = 180;          // градусів за тік (180 = миттєво)
    public int lookPitch = 90;           // 90 = строго вниз
    public int windSelectDelay = 0;
    public int windThrowDelay = 0;
    public boolean hopOnThrow = true;
    public double windHopStrength = 0.42;
    public boolean glideAfterWind = true;
    public int glideDelay = 1;
    public boolean restoreLook = true;
    public int restoreLookDelay = 2;
    public boolean fireworkAfterWind = false;
    public int fireworkAfterWindDelay = 3;

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                IkkoConfig loaded = GSON.fromJson(Files.readString(PATH), IkkoConfig.class);
                if (loaded != null) I = loaded;
            } else {
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
            I = new IkkoConfig();
        }
    }

    public static void save() {
        try {
            Files.writeString(PATH, GSON.toJson(I));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void reset() {
        I = new IkkoConfig();
        save();
    }
}

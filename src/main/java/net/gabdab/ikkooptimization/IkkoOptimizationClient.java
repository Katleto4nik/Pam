package net.gabdab.ikkooptimization;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class IkkoOptimizationClient implements ClientModInitializer {
    public static KeyBinding OPEN;

    @Override
    public void onInitializeClient() {
        IkkoConfig.load();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("ikkooptimization", "main"));
        OPEN = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ikkooptimization.open_hud",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN.wasPressed()) {
                if (mc.currentScreen == null && mc.player != null) {
                    mc.setScreen(new IkkoScreen());
                }
            }
            AutoFlight.tick(mc);
        });
    }
}

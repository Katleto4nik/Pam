package net.gabdab.ikkooptimization;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

public class IkkoScreen extends Screen {
    private static final String[] TABS = {"Основне", "Елітри", "Феєрверк", "Заряд вітру"};
    private static final int W = 340, H = 222;
    private static int page = 0;

    private boolean listening = false;
    private ButtonWidget bindBtn;
    private int left, top, colW;

    public IkkoScreen() {
        super(Text.literal("IkkoOptimization"));
    }

    // ------------------------------------------------------------ layout

    private int cx(int col) {
        return left + 10 + col * (colW + 10);
    }

    private int ry(int row) {
        return top + 50 + row * 23;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        colW = (W - 30) / 2;
        IkkoConfig c = IkkoConfig.I;

        int tw = (W - 20 - 12) / 4;
        for (int i = 0; i < TABS.length; i++) {
            final int idx = i;
            Text t = Text.literal(TABS[i]).formatted(page == i ? Formatting.YELLOW : Formatting.WHITE);
            addDrawableChild(ButtonWidget.builder(t, b -> {
                page = idx;
                clearAndInit();
            }).dimensions(left + 10 + i * (tw + 4), top + 22, tw, 20).build());
        }

        switch (page) {
            case 0 -> {
                toggle(cx(0), ry(0), W - 20, "Мод", () -> c().enabled, v -> c().enabled = v);

                addDrawableChild(ButtonWidget.builder(Text.literal("Режим"), b -> {
                    page = c().mode == 0 ? 2 : 3;
                    clearAndInit();
                }).dimensions(cx(0), ry(1), colW, 20).build());
                addDrawableChild(ButtonWidget.builder(modeText(), b -> {
                    c().mode = (c().mode + 1) % 2; // ЛКМ перемикає режим
                    b.setMessage(modeText());
                    IkkoConfig.save();
                }).dimensions(cx(1), ry(1), colW, 20).build());

                bindBtn = ButtonWidget.builder(bindText(), b -> {
                    listening = true;
                    b.setMessage(Text.literal("> натисни клавішу <").formatted(Formatting.YELLOW));
                }).dimensions(cx(0), ry(2), W - 20, 20).build();
                addDrawableChild(bindBtn);

                slider(0, 3, "Вікно подвійного натиску (тіки)", 2, 20, 1, () -> c().doubleTapWindow, v -> c().doubleTapWindow = (int) Math.round(v));
                toggle(cx(1), ry(3), colW, "Тільки в повітрі", () -> c().requireAirborne, v -> c().requireAirborne = v);
                toggle(cx(0), ry(4), colW, "Працювати у воді", () -> c().allowInWater, v -> c().allowInWater = v);
                toggle(cx(1), ry(4), colW, "Повідомлення", () -> c().showMessages, v -> c().showMessages = v);
                slider(0, 5, "Макс. тривалість (тіки)", 10, 200, 5, () -> c().maxSequenceTicks, v -> c().maxSequenceTicks = (int) Math.round(v));
                addDrawableChild(ButtonWidget.builder(Text.literal("Скинути все").formatted(Formatting.RED), b -> {
                    IkkoConfig.reset();
                    clearAndInit();
                }).dimensions(cx(1), ry(5), colW, 20).build());
            }
            case 1 -> {
                slider(0, 0, "Затримка надягання", 0, 10, 1, () -> c().equipDelay, v -> c().equipDelay = (int) Math.round(v));
                slider(1, 0, "Затримка зльоту", 0, 10, 1, () -> c().takeoffDelay, v -> c().takeoffDelay = (int) Math.round(v));
                slider(0, 1, "Мін. міцність елітр", 1, 100, 1, () -> c().minDurability, v -> c().minDurability = (int) Math.round(v));
                toggle(cx(1), ry(1), colW, "Підстрибувати", () -> c().hopOnTakeoff, v -> c().hopOnTakeoff = v);
                slider(0, 2, "Сила підстрибу", 0.1, 1.0, 0.01, () -> c().hopStrength, v -> c().hopStrength = v);
                toggle(cx(1), ry(2), colW, "Повертати нагрудник", () -> c().restoreChestplate, v -> c().restoreChestplate = v);
                slider(0, 3, "Затримка повернення", 0, 20, 1, () -> c().restoreDelay, v -> c().restoreDelay = (int) Math.round(v));
            }
            case 2 -> {
                slider(0, 0, "Затримка вибору", 0, 10, 1, () -> c().fwSelectDelay, v -> c().fwSelectDelay = (int) Math.round(v));
                slider(1, 0, "Затримка використання", 0, 10, 1, () -> c().fwUseDelay, v -> c().fwUseDelay = (int) Math.round(v));
                slider(0, 1, "Кількість феєрверків", 1, 5, 1, () -> c().fwCount, v -> c().fwCount = (int) Math.round(v));
                slider(1, 1, "Інтервал (тіки)", 1, 40, 1, () -> c().fwInterval, v -> c().fwInterval = (int) Math.round(v));
                slider(0, 2, "Слот (0 = авто)", 0, 9, 1, () -> c().fwPreferredSlot, v -> c().fwPreferredSlot = (int) Math.round(v));
                toggle(cx(1), ry(2), colW, "Змах рукою", () -> c().swingHand, v -> c().swingHand = v);
                toggle(cx(0), ry(3), colW, "Повертати слот", () -> c().returnSlot, v -> c().returnSlot = v);
                slider(1, 3, "Затримка повернення", 0, 10, 1, () -> c().returnDelay, v -> c().returnDelay = (int) Math.round(v));
            }
            default -> {
                slider(0, 0, "Швидкість погляду °/тік", 5, 180, 5, () -> c().lookSpeed, v -> c().lookSpeed = (int) Math.round(v));
                slider(1, 0, "Кут вниз (°)", 45, 90, 1, () -> c().lookPitch, v -> c().lookPitch = (int) Math.round(v));
                slider(0, 1, "Затримка вибору", 0, 10, 1, () -> c().windSelectDelay, v -> c().windSelectDelay = (int) Math.round(v));
                slider(1, 1, "Затримка кидка", 0, 10, 1, () -> c().windThrowDelay, v -> c().windThrowDelay = (int) Math.round(v));
                toggle(cx(0), ry(2), colW, "Підстрибувати", () -> c().hopOnThrow, v -> c().hopOnThrow = v);
                slider(1, 2, "Сила підстрибу", 0.1, 1.0, 0.01, () -> c().windHopStrength, v -> c().windHopStrength = v);
                toggle(cx(0), ry(3), colW, "Зліт після кидка", () -> c().glideAfterWind, v -> c().glideAfterWind = v);
                slider(1, 3, "Затримка зльоту", 0, 10, 1, () -> c().glideDelay, v -> c().glideDelay = (int) Math.round(v));
                toggle(cx(0), ry(4), colW, "Повернути погляд", () -> c().restoreLook, v -> c().restoreLook = v);
                slider(1, 4, "Затримка погляду", 0, 10, 1, () -> c().restoreLookDelay, v -> c().restoreLookDelay = (int) Math.round(v));
                toggle(cx(0), ry(5), colW, "Феєрверк після", () -> c().fireworkAfterWind, v -> c().fireworkAfterWind = v);
                slider(1, 5, "Затримка феєрверка", 0, 20, 1, () -> c().fireworkAfterWindDelay, v -> c().fireworkAfterWindDelay = (int) Math.round(v));
            }
        }
    }

    private static IkkoConfig c() {
        return IkkoConfig.I;
    }

    // ----------------------------------------------------------- widgets

    private void slider(int col, int row, String label, double min, double max, double step, DoubleSupplier g, DoubleConsumer s) {
        addDrawableChild(new ConfigSlider(cx(col), ry(row), colW, 20, label, min, max, step, g, s));
    }

    private void toggle(int x, int y, int w, String label, BooleanSupplier g, Consumer<Boolean> s) {
        addDrawableChild(ButtonWidget.builder(toggleText(label, g.getAsBoolean()), b -> {
            s.accept(!g.getAsBoolean());
            b.setMessage(toggleText(label, g.getAsBoolean()));
            IkkoConfig.save();
        }).dimensions(x, y, w, 20).build());
    }

    private static Text toggleText(String label, boolean on) {
        return Text.literal(label + ": ").append(Text.literal(on ? "УВІМК" : "ВИМК").formatted(on ? Formatting.GREEN : Formatting.RED));
    }

    private static Text modeText() {
        return Text.literal(IkkoConfig.I.mode == 0 ? "Феєрверк" : "Заряд вітру").formatted(Formatting.AQUA);
    }

    private static Text bindText() {
        return Text.literal("Бінд HUD: ").append(IkkoOptimizationClient.OPEN.getBoundKeyLocalizedText());
    }

    // ------------------------------------------------------------- input

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (listening) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                IkkoOptimizationClient.OPEN.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(key));
                KeyBinding.updateKeysByCode();
                if (client != null) client.options.write();
            }
            listening = false;
            if (bindBtn != null) bindBtn.setMessage(bindText());
            return true;
        }
        if (key == KeyBindingHelper.getBoundKeyOf(IkkoOptimizationClient.OPEN).getCode()) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        IkkoConfig.save();
    }

    // -------------------------------------------------------------- draw

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(left, top, left + W, top + H, 0xD0101018);
        int border = 0xFF4C9BFF;
        ctx.fill(left, top, left + W, top + 1, border);
        ctx.fill(left, top + H - 1, left + W, top + H, border);
        ctx.fill(left, top, left + 1, top + H, border);
        ctx.fill(left + W - 1, top, left + W, top + H, border);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("IkkoOptimization"), width / 2, top + 8, 0xFF8AC8FF);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("By:GAB_DAB2025"), width / 2, top + H - 13, 0xFFAAAAAA);
    }
}

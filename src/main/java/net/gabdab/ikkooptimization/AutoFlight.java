package net.gabdab.ikkooptimization;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;

/**
 * Уся логіка моду: подвійний пробіл -> елітри -> зліт -> феєрверк / заряд вітру.
 * Кроки виконуються з чергою; затримка 0 = все в одному тіку (максимально швидко).
 */
public final class AutoFlight {
    private static final int DONE = 1, WAIT = 0, ABORT = -1;

    private interface Step {
        int run(MinecraftClient mc, ClientPlayerEntity p);
    }

    private static final class Queued {
        int delay;
        final Step step;

        Queued(int delay, Step step) {
            this.delay = delay;
            this.step = step;
        }
    }

    private static final ArrayDeque<Queued> queue = new ArrayDeque<>();

    private static int ticks = 0;
    private static int lastPress = -1000;
    private static int seqTicks = 0;

    private static int prevSlot = -1;
    private static float prevPitch = 0f;

    private static boolean swapped = false;
    private static boolean sawGlide = false;
    private static int swapAge = 0;
    private static int landTicks = 0;
    private static ItemStack prevChest = ItemStack.EMPTY;

    private AutoFlight() {
    }

    // ------------------------------------------------------------------ tick

    public static void tick(MinecraftClient mc) {
        ticks++;
        KeyBinding jump = mc.options.jumpKey;
        int presses = 0;
        while (jump.wasPressed()) presses++;

        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.interactionManager == null) {
            queue.clear();
            swapped = false;
            return;
        }

        boolean dbl = detectDoubleTap(presses);

        if (!queue.isEmpty()) {
            runQueue(mc, p);
            return;
        }

        if (dbl && canTrigger(mc, p)) {
            start(mc, p);
            runQueue(mc, p); // одразу в тому ж тіку
            return;
        }

        handleRestore(mc, p);
    }

    private static boolean detectDoubleTap(int presses) {
        int window = IkkoConfig.I.doubleTapWindow;
        if (presses >= 2) {
            lastPress = -1000;
            return true;
        }
        if (presses == 1) {
            if (ticks - lastPress <= window) {
                lastPress = -1000;
                return true;
            }
            lastPress = ticks;
        }
        return false;
    }

    private static boolean canTrigger(MinecraftClient mc, ClientPlayerEntity p) {
        IkkoConfig c = IkkoConfig.I;
        if (!c.enabled || mc.currentScreen != null) return false;
        if (p.isCreative() || p.isSpectator()) return false;
        if (p.isGliding() || p.hasVehicle() || p.isClimbing()) return false;
        if (p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) return false;
        if (c.requireAirborne && p.isOnGround()) return false;
        if (!c.allowInWater && (p.isTouchingWater() || p.isInLava())) return false;
        return true;
    }

    // ------------------------------------------------------------- sequence

    private static void add(int delay, Step step) {
        queue.add(new Queued(Math.max(0, delay), step));
    }

    private static void start(MinecraftClient mc, ClientPlayerEntity p) {
        IkkoConfig c = IkkoConfig.I;
        queue.clear();
        seqTicks = 0;
        prevPitch = p.getPitch();
        prevSlot = p.getInventory().getSelectedSlot();
        info(p, "IkkoOptimization: старт");

        add(c.equipDelay, AutoFlight::equip);

        if (c.mode == 0) {
            // ---- Режим 1: Феєрверк ----
            add(c.takeoffDelay, (m, pl) -> {
                if (c.hopOnTakeoff) hop(pl, c.hopStrength);
                return DONE;
            });
            add(0, AutoFlight::glide);
            add(c.fwSelectDelay, select(Items.FIREWORK_ROCKET, c.fwPreferredSlot));
            for (int i = 0; i < Math.max(1, c.fwCount); i++) {
                add(i == 0 ? c.fwUseDelay : c.fwInterval, use(Items.FIREWORK_ROCKET, c.fwPreferredSlot));
            }
            if (c.returnSlot) add(c.returnDelay, AutoFlight::returnSlotStep);
        } else {
            // ---- Режим 2: Заряд вітру ----
            add(0, (m, pl) -> lookDown(pl));
            add(c.windSelectDelay, select(Items.WIND_CHARGE, 0));
            add(c.windThrowDelay, (m, pl) -> {
                if (pl.getMainHandStack().isOf(Items.WIND_CHARGE)) {
                    m.interactionManager.interactItem(pl, Hand.MAIN_HAND);
                    if (c.swingHand) pl.swingHand(Hand.MAIN_HAND);
                }
                if (c.hopOnThrow) hop(pl, c.windHopStrength);
                return DONE;
            });
            if (c.glideAfterWind) add(c.glideDelay, AutoFlight::glide);
            if (c.fireworkAfterWind) {
                add(c.fireworkAfterWindDelay, select(Items.FIREWORK_ROCKET, c.fwPreferredSlot));
                add(0, use(Items.FIREWORK_ROCKET, c.fwPreferredSlot));
            }
            if (c.restoreLook) {
                add(c.restoreLookDelay, (m, pl) -> {
                    pl.setPitch(prevPitch);
                    return DONE;
                });
            }
            if (c.returnSlot) add(c.returnDelay, AutoFlight::returnSlotStep);
        }
    }

    private static void runQueue(MinecraftClient mc, ClientPlayerEntity p) {
        seqTicks++;
        if (seqTicks > IkkoConfig.I.maxSequenceTicks) {
            queue.clear();
            return;
        }
        while (!queue.isEmpty()) {
            Queued q = queue.peek();
            if (q.delay > 0) {
                q.delay--;
                return;
            }
            int r;
            try {
                r = q.step.run(mc, p);
            } catch (Exception e) {
                e.printStackTrace();
                queue.clear();
                return;
            }
            if (r == WAIT) return;
            if (r == ABORT) {
                queue.clear();
                return;
            }
            queue.poll();
        }
    }

    // ----------------------------------------------------------------- steps

    private static int equip(MinecraftClient mc, ClientPlayerEntity p) {
        IkkoConfig c = IkkoConfig.I;
        ItemStack chest = p.getEquippedStack(EquipmentSlot.CHEST);
        if (chest.isOf(Items.ELYTRA)) return DONE;

        PlayerInventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 36; i++) { // хотбар (0-8) перевіряється першим
            ItemStack s = inv.getStack(i);
            if (s.isOf(Items.ELYTRA) && s.getMaxDamage() - s.getDamage() > c.minDurability) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            p.sendMessage(Text.literal("§cIkkoOptimization: елітри не знайдено"), true);
            return ABORT;
        }

        prevChest = chest.copy();
        int sync = p.playerScreenHandler.syncId;
        ClientPlayerInteractionManager im = mc.interactionManager;
        if (slot < 9) {
            // 1 пакет: міняємо слот нагрудника (6) з слотом хотбара
            im.clickSlot(sync, 6, slot, SlotActionType.SWAP, p);
        } else {
            im.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);
            im.clickSlot(sync, 6, 0, SlotActionType.PICKUP, p);
            if (!chest.isEmpty()) im.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);
        }
        swapped = true;
        sawGlide = false;
        swapAge = 0;
        landTicks = 0;
        return DONE;
    }

    private static int glide(MinecraftClient mc, ClientPlayerEntity p) {
        if (p.isGliding()) {
            sawGlide = true;
            return DONE;
        }
        if (p.isOnGround()) return WAIT;
        if (!p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) return ABORT;
        p.startGliding();
        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(p, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        sawGlide = true;
        return DONE;
    }

    private static int lookDown(ClientPlayerEntity p) {
        float target = IkkoConfig.I.lookPitch;
        float speed = Math.max(1, IkkoConfig.I.lookSpeed);
        float cur = p.getPitch();
        float d = target - cur;
        if (Math.abs(d) <= speed) {
            p.setPitch(target);
            return DONE;
        }
        p.setPitch(cur + Math.signum(d) * speed);
        return WAIT;
    }

    private static Step select(Item item, int preferred) {
        return (mc, p) -> {
            int s = findHotbar(p, item, preferred);
            if (s < 0) {
                p.sendMessage(Text.literal("§cIkkoOptimization: немає предмета в хотбарі"), true);
                return DONE; // продовжуємо, use-крок сам пропуститься
            }
            p.getInventory().setSelectedSlot(s);
            return DONE;
        };
    }

    private static Step use(Item item, int preferred) {
        return (mc, p) -> {
            if (!p.getMainHandStack().isOf(item)) {
                int s = findHotbar(p, item, preferred);
                if (s < 0) return DONE;
                p.getInventory().setSelectedSlot(s);
            }
            mc.interactionManager.interactItem(p, Hand.MAIN_HAND);
            if (IkkoConfig.I.swingHand) p.swingHand(Hand.MAIN_HAND);
            return DONE;
        };
    }

    private static int returnSlotStep(MinecraftClient mc, ClientPlayerEntity p) {
        if (prevSlot >= 0 && prevSlot < 9) p.getInventory().setSelectedSlot(prevSlot);
        return DONE;
    }

    // --------------------------------------------------------------- helpers

    private static int findHotbar(ClientPlayerEntity p, Item item, int preferred) {
        PlayerInventory inv = p.getInventory();
        if (preferred >= 1 && preferred <= 9 && inv.getStack(preferred - 1).isOf(item)) return preferred - 1;
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(item)) return i;
        }
        return -1;
    }

    private static void hop(ClientPlayerEntity p, double strength) {
        Vec3d v = p.getVelocity();
        p.setVelocity(v.x, strength, v.z);
    }

    private static void info(ClientPlayerEntity p, String msg) {
        if (IkkoConfig.I.showMessages) p.sendMessage(Text.literal(msg), true);
    }

    // ------------------------------------------- повернення нагрудника

    private static void handleRestore(MinecraftClient mc, ClientPlayerEntity p) {
        if (!swapped) return;
        IkkoConfig c = IkkoConfig.I;
        swapAge++;

        if (!p.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA)) {
            swapped = false; // гравець сам щось змінив
            return;
        }
        if (!c.restoreChestplate) {
            swapped = false;
            return;
        }
        if (p.isGliding()) {
            sawGlide = true;
            landTicks = 0;
            return;
        }
        if (sawGlide && p.isOnGround() && mc.currentScreen == null) {
            landTicks++;
            if (landTicks >= c.restoreDelay) restoreChest(mc, p);
        } else if (!sawGlide && swapAge > 200) {
            swapped = false;
        }
    }

    private static void restoreChest(MinecraftClient mc, ClientPlayerEntity p) {
        swapped = false;
        int sync = p.playerScreenHandler.syncId;
        ClientPlayerInteractionManager im = mc.interactionManager;

        if (prevChest.isEmpty()) {
            im.clickSlot(sync, 6, 0, SlotActionType.QUICK_MOVE, p);
            return;
        }
        PlayerInventory inv = p.getInventory();
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            if (inv.getStack(i).isOf(prevChest.getItem())) {
                slot = i;
                break;
            }
        }
        if (slot < 0) return;
        if (slot < 9) {
            im.clickSlot(sync, 6, slot, SlotActionType.SWAP, p);
        } else {
            im.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);
            im.clickSlot(sync, 6, 0, SlotActionType.PICKUP, p);
            im.clickSlot(sync, slot, 0, SlotActionType.PICKUP, p);
        }
    }
}

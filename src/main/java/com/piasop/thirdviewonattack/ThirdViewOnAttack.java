package com.piasop.thirdviewonattack;

import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod("thirdviewonattack")
public class ThirdViewOnAttack {

    private static final Minecraft mc = Minecraft.getInstance();

    /* ===== STATE ===== */
    private boolean modEnabled = true;
    private boolean inCombat = false;
    private boolean cameraForcedByMod = false;
    private long lastAttackTime = 0L;

    /* ===== UI MESSAGE ===== */
    private String centerMessage = null;
    private long messageEndTime = 0L;

    /* ===== KEYBIND ===== */
    private static KeyMapping TOGGLE_KEY;

    public ThirdViewOnAttack() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterKeybinds);
    }

    /* ================= KEYBIND REG ================= */

    private void onRegisterKeybinds(RegisterKeyMappingsEvent event) {
        TOGGLE_KEY = new KeyMapping(
                "thirdviewonattack",
                GLFW.GLFW_KEY_G,
                "thirdviewonattack"
        );
        event.register(TOGGLE_KEY);
    }

    /* ================= KEY INPUT ================= */

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (TOGGLE_KEY != null && TOGGLE_KEY.consumeClick()) {
            modEnabled = !modEnabled;
            inCombat = false;

            if (cameraForcedByMod && mc.player != null) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }

            cameraForcedByMod = false;

            showCenterMessage(
                    modEnabled
                            ? "Thirdview on attack: ON"
                            : "Thirdview on attack: OFF",
                    2000
            );
        }
    }

    /* ================= ATTACK ================= */

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (!modEnabled) return;

        Entity target = event.getTarget();
        if (!(target instanceof Mob)) return;
        if (target instanceof Villager || target instanceof WanderingTrader) return;

        if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
            inCombat = true;
            lastAttackTime = System.currentTimeMillis();
            return;
        }

        inCombat = true;
        lastAttackTime = System.currentTimeMillis();
        cameraForcedByMod = true;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    /* ================= TICK ================= */

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!modEnabled) return;

        LocalPlayer player = mc.player;
        if (player == null) return;

        long now = System.currentTimeMillis();

        // Respeta F5
        if (cameraForcedByMod && mc.options.getCameraType() != CameraType.THIRD_PERSON_BACK) {
            cameraForcedByMod = false;
            inCombat = false;
            return;
        }

        BlockPos basePos = player.blockPosition();
        for (int i = 1; i <= 6; i++) {
            BlockPos checkPos = basePos.above(i);
            if (!player.level().getBlockState(checkPos).isAir()) {
                if (cameraForcedByMod) {
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                }
                cameraForcedByMod = false;
                inCombat = false;
                return;
            }
        }

        if (inCombat && cameraForcedByMod && now - lastAttackTime > 5000) {
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            cameraForcedByMod = false;
            inCombat = false;
        }
    }

    /* ================= CAMERA ================= */

    @SubscribeEvent
    public void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!modEnabled || !inCombat || !cameraForcedByMod) return;
        event.setYaw(event.getYaw() + 15f);
    }

    /* ================= UI MESSAGE ================= */

    private void showCenterMessage(String text, long durationMs) {
        centerMessage = text;
        messageEndTime = System.currentTimeMillis() + durationMs;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (centerMessage == null) return;
        if (System.currentTimeMillis() > messageEndTime) {
            centerMessage = null;
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        Font font = mc.font;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int x = screenWidth / 2 - font.width(centerMessage) / 2;
        int y = (int) (screenHeight * 0.8f);

        gui.drawString(font, centerMessage, x, y, 0xFFFFFF, true);
    }
}

package com.piasop.thirdviewonattack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod("thirdviewonattack")
public class ThirdViewOnAttack {

    private static final Minecraft mc = Minecraft.getInstance();
    private boolean inCombat = false;
    private long lastAttackTime = 0L;

    public ThirdViewOnAttack() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        Entity target = event.getTarget();
        if (!(target instanceof Mob) || target instanceof Villager || target instanceof WanderingTrader) return;

        inCombat = true;
        lastAttackTime = System.currentTimeMillis();
        mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        LocalPlayer player = mc.player;
        if (player == null) return;

        // Revisar bloque encima de la cabeza (de 1 a 6 bloques arriba)
        BlockPos playerPos = player.blockPosition();
        boolean blockOverhead = false;
        for (int i = 1; i <= 6; i++) {
            BlockPos posCheck = playerPos.above(i);
            if (!player.level().getBlockState(posCheck).isAir()) {
                blockOverhead = true;
                break;
            }
        }

        long now = System.currentTimeMillis();

        if (blockOverhead) {
            // Bloque obstruyendo: volver a primera persona inmediato
            inCombat = false;
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            player.setInvisible(false);
            return;
        }

        if (inCombat) {
            // Revisar tiempo sin atacar
            if (now - lastAttackTime > 5000) {
                // Pasaron 5 segundos sin atacar, volver a primera persona
                inCombat = false;
                mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                player.setInvisible(false);
            }
        }
    }

    @SubscribeEvent
    public void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!inCombat) return;

        // Mantener yaw fijo -15 y ajustar pitch para cámara estática
        event.setYaw(event.getYaw() - -15f);
        // Puedes ajustar pitch si quieres, aquí lo dejamos igual para cámara más estable
    }
}

package me.spunkyinsaan.heartindicator;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;

import java.util.Map;
import java.util.WeakHashMap;

/** Mapuje stan renderowania gracza na encję gracza (render state nie przechowuje encji). */
public final class RenderStateEntityLookup {
    private static final Map<PlayerEntityRenderState, AbstractClientPlayerEntity> PLAYERS = new WeakHashMap<>();

    private RenderStateEntityLookup() {}

    public static void put(PlayerEntityRenderState state, AbstractClientPlayerEntity player) {
        if (state == null || player == null) return;
        PLAYERS.put(state, player);
    }

    public static AbstractClientPlayerEntity get(PlayerEntityRenderState state) {
        return state == null ? null : PLAYERS.get(state);
    }
}

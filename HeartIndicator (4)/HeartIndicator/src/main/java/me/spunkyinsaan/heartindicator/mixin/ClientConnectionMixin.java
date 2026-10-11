package me.spunkyinsaan.heartindicator.mixin;

import me.spunkyinsaan.heartindicator.TpsTracker;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {
    @Inject(method = "handlePacket", at = @At("HEAD"))
    private static <T extends PacketListener> void heartindicator$packet(Packet<T> packet, PacketListener listener, CallbackInfo ci) {
        if (packet instanceof WorldTimeUpdateS2CPacket) TpsTracker.onTimeUpdate();
    }
}

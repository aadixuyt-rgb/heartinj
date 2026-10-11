package me.spunkyinsaan.heartindicator.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import me.spunkyinsaan.heartindicator.Config;
import me.spunkyinsaan.heartindicator.RenderStateEntityLookup;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.texture.GuiAtlasManager;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Duże serca nad głowami graczy (jak w oryginalnym Heart Indicator). Włączane w GUI (J). */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Unique private static final Identifier H_EMPTY = Identifier.ofVanilla("hud/heart/container");
    @Unique private static final Identifier H_RED = Identifier.ofVanilla("hud/heart/full");
    @Unique private static final Identifier H_RED_HALF = Identifier.ofVanilla("hud/heart/half");
    @Unique private static final Identifier H_GOLD = Identifier.ofVanilla("hud/heart/absorbing_full");
    @Unique private static final Identifier H_GOLD_HALF = Identifier.ofVanilla("hud/heart/absorbing_half");

    @Shadow
    protected abstract boolean hasLabel(LivingEntity entity, double squaredDistanceToCamera);

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("TAIL"))
    public void heartindicator$renderHearts(LivingEntityRenderState state, MatrixStack matrices,
                                            VertexConsumerProvider provider, int light, CallbackInfo ci) {
        Config c = Config.INSTANCE;
        if (!c.renderingEnabled || !c.showOverhead) return;
        if (!((Object) this instanceof PlayerEntityRenderer)) return;
        if (!(state instanceof PlayerEntityRenderState ps)) return;
        AbstractClientPlayerEntity player = RenderStateEntityLookup.get(ps);
        if (!heartindicator$should(player)) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        matrices.push();
        matrices.translate(0f, player.getHeight() + 0.5f, 0f);
        var dispatcher = mc.getEntityRenderDispatcher();
        double dist = dispatcher.getSquaredDistanceToCamera(player);
        if (this.hasLabel(player, dist)) matrices.translate(0.0, 0.30875, 0.0);
        matrices.multiply(dispatcher.getRotation());
        matrices.scale(-1f, 1f, 1f);
        float s = 0.025f * Math.max(0.3f, c.overheadScale);
        matrices.scale(s, s, s);

        GuiAtlasManager atlas = mc.getGuiAtlasManager();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX);
        RenderSystem.setShaderTexture(0, atlas.getSprite(H_EMPTY).getAtlasId());
        RenderSystem.enableDepthTest();

        BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        Matrix4f model = matrices.peek().getPositionMatrix();

        int health = MathHelper.ceil(player.getHealth());
        int max = MathHelper.ceil(player.getMaxHealth());
        int abs = c.showAbsorption ? MathHelper.ceil(player.getAbsorptionAmount()) : 0;
        int redHearts = MathHelper.ceil(health / 2f);
        boolean redHalf = (health & 1) == 1;
        int maxHearts = MathHelper.ceil(max / 2f);
        int absHearts = MathHelper.ceil(abs / 2f);
        boolean absHalf = (abs & 1) == 1;
        int total = maxHearts + absHearts;

        for (int i = 0; i < total; i++) {
            int row = i / 10, col = i % 10;
            int inRow = Math.min(10, total - row * 10);
            float half = (inRow * 8 + 1) / 2f;
            float x = half - col * 8;
            float y = row * 10;
            heartindicator$quad(model, buf, x, y, atlas.getSprite(H_EMPTY));

            Identifier type;
            if (i < redHearts) {
                type = (i == redHearts - 1 && redHalf) ? H_RED_HALF : H_RED;
            } else if (i < maxHearts) {
                type = null;
            } else {
                type = (i == total - 1 && absHalf) ? H_GOLD_HALF : H_GOLD;
            }
            if (type != null) heartindicator$quad(model, buf, x, y, atlas.getSprite(type));
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());
        matrices.pop();
    }

    @Unique
    private static boolean heartindicator$should(AbstractClientPlayerEntity p) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (p == null || mc == null || mc.player == null) return false;
        return !p.isSpectator() && !p.isInvisibleTo(mc.player);
    }

    @Unique
    private static void heartindicator$quad(Matrix4f m, VertexConsumer vc, float x, float y, Sprite sp) {
        float u0 = sp.getMinU(), u1 = sp.getMaxU(), v0 = sp.getMinV(), v1 = sp.getMaxV();
        float w = 9f;
        vc.vertex(m, x, y - w, 0f).texture(u0, v1);
        vc.vertex(m, x - w, y - w, 0f).texture(u1, v1);
        vc.vertex(m, x - w, y, 0f).texture(u1, v0);
        vc.vertex(m, x, y, 0f).texture(u0, v0);
    }
}

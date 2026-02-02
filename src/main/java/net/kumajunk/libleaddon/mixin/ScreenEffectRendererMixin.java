package net.kumajunk.libleaddon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kumajunk.libleaddon.features.impl.render.NoFire;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public class ScreenEffectRendererMixin {
    @Inject(
            method = "renderFire",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void disableFireOverlay(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            TextureAtlasSprite sprite,
            CallbackInfo ci
    ) {
        if (NoFire.INSTANCE.getEnabled()) ci.cancel();
    }
}

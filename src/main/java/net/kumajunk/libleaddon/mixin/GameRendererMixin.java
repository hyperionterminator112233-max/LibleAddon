package net.kumajunk.libleaddon.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.kumajunk.libleaddon.features.impl.render.NoHurtCam;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(
            method = "bobHurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void nohurtcam$cancelBobHurt(PoseStack poseStack, float f, CallbackInfo ci) {
        if (NoHurtCam.INSTANCE.getEnabled()) ci.cancel();
    }
}

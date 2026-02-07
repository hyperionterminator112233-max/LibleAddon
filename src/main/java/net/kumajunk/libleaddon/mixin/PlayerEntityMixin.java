package net.kumajunk.libleaddon.mixin;

import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils;
import net.kumajunk.libleaddon.features.impl.render.RemoveGlow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class PlayerEntityMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    public void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (RemoveGlow.INSTANCE.getEnabled() && DungeonUtils.INSTANCE.getInDungeons()) {
            if ((Object) this instanceof Player) {
                cir.setReturnValue(false);
            }
        }
    }
}

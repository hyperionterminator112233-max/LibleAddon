package net.kumajunk.libleaddon.mixin;

import net.kumajunk.libleaddon.state.HideArmorState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(HumanoidRenderState.class)
public class HumanoidRenderStateMixin implements HideArmorState {

    @Unique
    private boolean hideArmor;

    @Unique
    @Override
    public boolean libleaddon$isHideArmor() {
        return hideArmor;
    }

    @Unique
    @Override
    public void libleaddon$setHideArmor(boolean value) {
        this.hideArmor = value;
    }
}

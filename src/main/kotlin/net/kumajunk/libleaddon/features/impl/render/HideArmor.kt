package net.kumajunk.libleaddon.features.impl.render

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.features.Module
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

object HideArmor : Module(
    name = "Hide Armor(LA)",
    description = "Prevents rendering of armor pieces for players."
) {
    private val enableHide by BooleanSetting(
        "Enable Hide Armor",
        false,
        desc = "Enable hiding of armor pieces."
    )

    private val hideSelf by BooleanSetting(
        "Hide Self",
        false,
        desc = "Hide your own armor."
    )

    private val hideOthers by BooleanSetting(
        "Hide Others",
        false,
        desc = "Hide other players' armor."
    )

    @JvmStatic
    fun shouldHide(entity: LivingEntity?): Boolean {
        if (!enabled) return false
        if (!enableHide) return false
        if (entity !is Player) return false

        val self = mc.player ?: return false
        val isSelf = entity === self

        if (isSelf && hideSelf) return true
        if (!isSelf && hideOthers) return true

        return false
    }
}
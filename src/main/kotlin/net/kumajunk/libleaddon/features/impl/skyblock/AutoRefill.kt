package net.kumajunk.libleaddon.features.impl.skyblock

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.skyblock.Island
import com.odtheking.odin.utils.skyblock.LocationUtils
import net.kumajunk.libleaddon.utils.addonMessage

object AutoRefill : Module(
    name = "Auto Refill(LA)",
    description = "Automatically refill consumable items",
) {
    private var isZeroRefill = +BooleanSetting(
        name = "Allow Zero Refill",
        default = false,
        desc = "Whether to replenish items when they reach 0"
    )

    private var isPearlRefill = +BooleanSetting(
        name = "Pearl Refill",
        default = true,
        desc = "Automatically refills ender pearls"
    )
    private var pearlThreshold = +NumberSetting(
        name = "Pearl Threshold",
        default = 8,
        min = 1,
        max = 16,
        desc = "The amount of pearls you need to have to trigger a refill"
    ).withDependency { isPearlRefill.value }

    private var isSuperboomRefill = +BooleanSetting(
        name = "Superboom Refill",
        default = false,
        desc = "Automatically refills superboom TNT"
    )
    private var superboomThreshold = +NumberSetting(
        name = "Superboom Threshold",
        default = 32,
        min = 1,
        max = 64,
        desc = "The amount of superboom TNT you need to have to trigger a refill"
    ).withDependency { isSuperboomRefill.value }

    private var tick = 0

    init {
        on<TickEvent.End> {
            tick++
            if (tick % 10 != 0) return@on
            if (!LocationUtils.isInSkyblock || LocationUtils.currentArea == Island.Rift) return@on
            if (mc.screen != null) return@on

            if (isPearlRefill.value) {
                val pearlCount = mc.player?.inventory?.find { it?.itemId == "ENDER_PEARL" }?.count ?: 0
                if (pearlCount == 0 && !isZeroRefill.value) return@on
                if (pearlCount < pearlThreshold.value) {
                    sendCommand("gfs ender_pearl ${16 - pearlCount}")
                    addonMessage("§aAuto Refilled Ender Pearls!")
                    return@on
                }
            }

            if (isSuperboomRefill.value) {
                val superboomCount = mc.player?.inventory?.find { it?.itemId == "SUPERBOOM_TNT" }?.count ?: 0
                if (superboomCount == 0 && !isZeroRefill.value) return@on
                if (superboomCount < superboomThreshold.value) {
                    sendCommand("gfs superboom_tnt ${64 - superboomCount}")
                    addonMessage("§aAuto Refilled Superboom TNT!")
                    return@on
                }
            }
        }
    }
}
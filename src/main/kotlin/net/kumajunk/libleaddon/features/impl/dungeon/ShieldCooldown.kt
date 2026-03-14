package net.kumajunk.libleaddon.features.impl.dungeon

import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.render.textDim
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.sounds.SoundEvents

object ShieldCooldown : Module(
    name = "Shield Cooldown(LA)",
    description = "Displays the cooldown of the Wither Shield ability."
){
    private val color by ColorSetting("Color", Color(85, 255, 255), false, desc = "Color of the timer text on HUD.")

    private val hud by HUD("Shield Cooldown", desc = "Displays the cooldown of the Wither Shield ability.", toggleable = false) { example ->
        if (example) {
            textDim("5.00", 0, 0, color)
        } else if (cooldown == 0) {
            0 to 0
        } else {
            val seconds = cooldown / 20.0
            textDim(String.format("%.2f", seconds), 0, 0, color)
        }
    }

    var cooldown = 0

    init {
        onReceive<ClientboundSoundPacket> { event ->
            val packet = event.packet as? ClientboundSoundPacket ?: return@onReceive
            val player = mc.player ?: return@onReceive
            val isHyperion = player.mainHandItem.itemId == "HYPERION"
            if (packet.sound.value() == SoundEvents.ZOMBIE_VILLAGER_CURE && isHyperion) {
                cooldown = 100
            }
        }

        on<TickEvent.Server> {
            if (cooldown == 0) return@on
            cooldown--
            if (cooldown < 0) cooldown = 0
        }
    }
}
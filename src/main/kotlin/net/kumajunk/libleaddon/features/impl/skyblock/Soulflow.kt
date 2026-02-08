package net.kumajunk.libleaddon.features.impl.skyblock

import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.textDim
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

object Soulflow : Module(
    name = "Soulflow(LA)",
    description = "Display Soulflow counts"
) {
    private val hud by HUD("Soulflow", desc = "Display Soulflow counts.", toggleable = false) { example ->
        if (example) {
            textDim("§3⸎ 160", 0, 0, Colors.WHITE)
        } else if (soulflowCounts > 0L) {
            textDim("§3⸎ $soulflowCounts", 0, 0, Colors.WHITE)
        } else
            0 to 0
    }

    var soulflowCounts: Long = 0L
    var lastEtherWarp: Long = 0L
    var lastGyro: Long = 0L
    var lastOverflowMana = 0
    private val overflowManaRegex = Regex("([0-9,]+)ʬ")

    init {
        on<ChatPacketEvent> {
            val msg = value.noControlCodes
            val regex = Regex("have a total of ([0-9,]+)⸎")
            val match = regex.find(msg)

            if (match != null) {
                val soulflowValue = match.groupValues[1].replace(",", "")
                soulflowCounts = soulflowValue.toLong()
            }
        }

        onReceive<ClientboundSystemChatPacket> { event ->
            val packet = event.packet as? ClientboundSystemChatPacket ?: return@onReceive
            if (!packet.overlay) return@onReceive

            val content = packet.content().string.noControlCodes

            if (content.contains("Ether Transmission")) {
                if (lastEtherWarp + 2500L > System.currentTimeMillis()) return@onReceive
                soulflowCounts--
                lastEtherWarp = System.currentTimeMillis()
            }
            if (content.contains("Gravity Storm")) {
                if (lastGyro + 4000L > System.currentTimeMillis()) return@onReceive
                soulflowCounts -= 10
                lastGyro = System.currentTimeMillis()
            }
            val match = overflowManaRegex.find(content)
            if (match != null) {
                val value = match.groupValues[1].replace(",", "").toInt()
                if (lastOverflowMana < value) {
                    soulflowCounts -= 15L
                }
                lastOverflowMana = value
            }
        }
    }
}
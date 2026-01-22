package net.kumajunk.libleaddon.features.impl.floor7

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.createSoundSettings
import com.odtheking.odin.utils.handlers.schedule
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.playSoundSettings
import com.odtheking.odin.utils.render.textDim
import net.kumajunk.libleaddon.LibleAddon.playerName

object PreEnterNotifier : Module(
    name = "Pre Enter Notifier(LA)",
    description = "Notifies you when a player is at a device or melody terminal in Floor 7 Phase 2.",
) {
    // --- Phase 2 Detection Settings ---
    private val forceP2True by BooleanSetting("Force P2 True", false, desc = "Force Phase 2 detection to true for debugging. \nDon't Touch This Unless Testing!")

    // --- EE Notifier Settings ---
    private val eeNotifyDisplayTime by NumberSetting("EE Display Time", 3.0, 1.0, 10.0, 0.5, desc = "Duration to display the EE notification.")
    private val eeNotifierColor by ColorSetting("EE Notifier Color", Colors.MINECRAFT_RED, desc = "Color of the EE notification text.")
    private val eeSoundDropdown by DropdownSetting("EE Sound")
    private val eeSoundSettings = createSoundSettings("EE Notification Sound", "random.orb") { eeSoundDropdown }
    
    private val eeHud by HUD("EE Notifier HUD", desc = "Displays EE device notifications on HUD.", toggleable = false) {
        if (it) {
            return@HUD textDim("$playerName is At SS!", 0, 0, eeNotifierColor)
        }
        else if (!drawEEHud) return@HUD 0 to 0
        textDim(eeMessage, 0, 0, eeNotifierColor)
    }

    // --- Melody Notifier Settings ---
    private val isNotifyMelody by BooleanSetting("Notify Melody", true, desc = "Enable Melody terminal notifications.")
    private val melodyNotifyDisplayTime by NumberSetting("Melody Display Time", 3.0, 1.0, 10.0, 0.5, desc = "Duration to display the Melody notification.")
    private val melodyNotifyColor by ColorSetting("Melody Notifier Color", Colors.MINECRAFT_GREEN, desc = "Color of the Melody notification text.")
    private val melodySoundDropdown by DropdownSetting("Melody Sound")
    private val melodySoundSettings = createSoundSettings("Melody Notification Sound", "random.levelup") { melodySoundDropdown }

    private val melodyHud by HUD("Melody Notifier HUD", desc = "Displays Melody terminal notifications on HUD.", toggleable = false) {
        if (it) {
            return@HUD textDim("$playerName Has Melody! (25%)", 0, 0, melodyNotifyColor)
        }
        else if (!drawMelodyHud) return@HUD 0 to 0
        textDim(melodyMessage, 0, 0, melodyNotifyColor)
    }

    // --- State ---
    private var isP2 = false
    private var drawEEHud = false
    private var eeMessage = ""
    private var drawMelodyHud = false
    private var melodyMessage = ""

    init {
        // Boss Dialogue / Phase Detection
        on<ChatPacketEvent> {
            val msg = value.noControlCodes
            
            // Phase 2 Detection
            if (msg == "[BOSS] Storm: Pathetic Maxor, just like expected.") {
                isP2 = true
            }
            if (msg == "[BOSS] Necron: All this, for nothing..." || msg.startsWith("Sending to server")) {
                isP2 = false
            }
        }

        // Player Chat Detection
        on<ChatPacketEvent> {
            val msg = value.noControlCodes
            
            // Regex for Party chat: Party > [Rank] User: Message or Party > User: Message
            val partyRegex = Regex("^Party > (?:.+ )?(\\w+): (.+)")
            val match = partyRegex.find(msg) ?: return@on
            
            val (player, message) = match.destructured
            val lowerMessage = message.lowercase()

            if (player == mc.player?.name.toString()) return@on

            // EE Notification (At/Inside/Outside)
            if ((isP2 || forceP2True) && 
                Regex("\\b(at|inside|outside)\\b", RegexOption.IGNORE_CASE).containsMatchIn(message)) {


                eeMessage = "$player is $message!"
                drawEEHud = true
                playSoundSettings(eeSoundSettings())
                schedule((eeNotifyDisplayTime * 20).toInt()) { drawEEHud = false }
            }

            // Melody Notification
            if (isP2 && isNotifyMelody && (lowerMessage.contains("melody"))) {
                 if (lowerMessage.contains("%") || lowerMessage.contains("/")) {

                    // Extract progress
                    var progress = "25%" // Default/Fallback
                    val percentMatch = Regex("(\\d{1,3})%").find(message)
                    if (percentMatch != null) {
                        progress = percentMatch.value
                    } else {
                        val fractionMatch = Regex("(\\d+)\\s*/\\s*(\\d+)").find(message)
                        if (fractionMatch != null) {
                            val (num, den) = fractionMatch.destructured
                            val d = den.toIntOrNull()
                            if (d != null && d != 0) {
                                val p = (num.toInt() * 100) / d
                                progress = "$p%"
                            }
                        }
                    }

                    melodyMessage = "$player Has Melody! ($progress)"
                    drawMelodyHud = true
                    playSoundSettings(melodySoundSettings())
                    schedule((melodyNotifyDisplayTime * 20).toInt()) { drawMelodyHud = false }
                 }
            }
        }

        // World Unload Reset
        on<WorldEvent.Unload> {
            isP2 = false
            drawEEHud = false
            drawMelodyHud = false
        }
    }
}

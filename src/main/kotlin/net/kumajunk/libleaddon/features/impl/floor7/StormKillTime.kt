package net.kumajunk.libleaddon.features.impl.floor7

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.sendCommand
import com.odtheking.odin.utils.toFixed
import net.kumajunk.libleaddon.utils.addonMessage

object StormKillTime : Module(
    name = "Storm Kill Time(LA)",
    description = "test"
) {
    private val announceInParty by BooleanSetting(
        name = "Announce in Party",
        desc = "Whether Storm kill times are announced in the party."
    )
    // Stormのセリフパターン (Crush攻撃検出)
    private val crushPattern = Regex("""^\[BOSS] Storm: (Ouch, that hurt!|Oof)$""")

    private val crushEndTime: MutableList<Long> = mutableListOf()
    private val totalTimes: MutableList<Long> = mutableListOf()

    init {
        // Crushパターン検出でタイマー開始
        on<ChatPacketEvent> {
            val msg = value.noControlCodes
            if (crushPattern.matches(msg)) {
                crushEndTime.add(System.currentTimeMillis() + 1000L)
            }
            if (msg.contains("Storm is enraged")) {
                val enragedTime = System.currentTimeMillis()
                val crushTime = crushEndTime.last()
                val totalTime = enragedTime - crushTime
                totalTimes.add(totalTime)
                addonMessage("§fStorm enraged in §b${(totalTime / 1000.0).toFixed(3)}s§f!")
                if (announceInParty) {
                    sendCommand("pc Storm enraged in ${(totalTime / 1000.0).toFixed(3)}s!")
                }
            }
            if (msg.contains("I should have known that I stood no chance")) {
                val killTime = System.currentTimeMillis()
                val crushTime = crushEndTime.last()
                val totalTime = killTime - crushTime
                totalTimes.add(totalTime)
                addonMessage("§fStorm killed in §b${(totalTime / 1000.0).toFixed(3)}s§f! Took §b${((totalTimes[0] + totalTimes[1]) / 1000.0).toFixed(3)}s §ftotal.")
                if (announceInParty) {
                    sendCommand("pc Storm killed in ${(totalTime / 1000.0).toFixed(3)}s! Took ${((totalTimes[0] + totalTimes[1]) / 1000.0).toFixed(3)}s total.")
                }
            }
        }

        // ワールドアンロード時にリセット
        on<WorldEvent.Unload> {
            crushEndTime.clear()
            totalTimes.clear()
        }
    }
}
package net.kumajunk.libleaddon.features.impl.floor7

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.ChatPacketEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.utils.PersonalBest
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.textDim
import com.odtheking.odin.utils.skyblock.dungeon.DungeonListener
import com.odtheking.odin.utils.toFixed
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

object TrueSplit : Module(
    name = "True Splits(LA)",
    description = "Advanced split timers for Floor 7 and Master Mode 7."
) {
    private val pb = PersonalBest(this, "SplitsPB")

    private val isF7OrM7: Boolean
        get() = DungeonListener.floor?.floorNumber == 7

    private val mainHud by HUD("Main Splits", "Displays the main split timers.") { example ->
        if (!example && !isF7OrM7) return@HUD 0 to 0
        val lines = if (example) {
            listOf(
                createHudText(0, "Blood Open", 1500L, 20L),
                createHudText(1, "Watcher", 5000L, 100L),
                createHudText(1, "Portal", 5000L, 100L),
                createHudText(1, "Watcher", 5000L, 100L),
            )
        } else {
            buildList {
                val runSnapshot = currentRun.toList()

                runSnapshot.forEachIndexed { index, split ->
                    add(createHudText(index, split.name, split.time, split.tick))
                }

                if (currentStage > 0 && currentStage <= STAGE_LABELS.size) {
                    val idx = currentStage - 1
                    val label = STAGE_LABELS[idx].first
                    add(
                        createHudText(
                            idx,
                            label,
                            System.currentTimeMillis() - startTimestamp,
                            tickCounter - startTick
                        )
                    )
                }
            }
        }

        if (lines.isEmpty()) return@HUD 0 to 0

        var yOffset = 0
        var maxWidth = 0

        lines.forEach { line ->
            val dim = textDim(line, 0, yOffset)
            maxWidth = maxOf(maxWidth, dim.first)
            yOffset += dim.second
        }

        maxWidth to yOffset
    }

    private val breakdownHud by HUD("Breakdown Splits", "Displays the detailed breakdown splits.") { example ->
        if (!breakdownEnabled && !example) return@HUD 0 to 0
        if (!example && !isF7OrM7) return@HUD 0 to 0

        val lines = if (example) {
            listOf(
                "§7§n§lMAXOR BREAKDOWN",
                "§bCrystals > §a1.50s",
                "§bMaxor Kill > §a5.00s"
            )
        } else {
            val snapshot = currentBreakdownLines.toList()

            buildList {
                var idx = currentBreakdownScrollIndex
                repeat(scrollLength) {
                    if (idx < snapshot.size) {
                        add(snapshot[idx])
                        idx++
                    }
                }

                if ((currentStage in 4..8 || (currentStage == 9 && DungeonListener.floor?.isMM == true)) && lastBreakdownTime > 0) {
                    val diff = System.currentTimeMillis() - lastBreakdownTime
                    val label = getCurrentBreakdownLabel()
                    add("§b$label §6> §a${(diff / 1000f).toFixed(2)}s")
                }
            }
        }

        if (lines.isEmpty()) return@HUD 0 to 0

        var yOffset = 0
        var maxWidth = 0

        lines.forEach { line ->
            val dim = textDim(line, 0, yOffset)
            maxWidth = maxOf(maxWidth, dim.first)
            yOffset += dim.second
        }

        maxWidth to yOffset
    }

    private val breakdownEnabled: Boolean by BooleanSetting(
        "Breakdown",
        true,
        desc = "Toggle the breakdown HUD."
    )
    private val scrollLength: Int by NumberSetting(
        "Breakdown Scroll Length",
        7,
        1,
        20,
        1,
        desc = "Number of lines to show in the breakdown HUD."
    )
    private val scrollDelay: Int by NumberSetting(
        "Breakdown Scroll Delay",
        5,
        0,
        20,
        1,
        unit = "seconds",
        desc = "Delay in seconds before scrolling to the next breakdown section."
    )

    data class SplitEntry(val name: String, val time: Long, val tick: Long)

    private var currentRun = mutableListOf<SplitEntry>()
    private var currentBreakdownLines = mutableListOf<String>()

    // Internal state
    private var currentStage = 0
    private var startTimestamp = 0L
    private var startTick = 0L
    private var tickCounter = 0L
    private var currentBreakdownScrollIndex = 0
    private var lastScrollTime = 0L

    // Breakdown State
    private var lastBreakdownTime = 0L
    private var lastBreakdownTick = 0L
    private var passedMaxorCrystals = 0
    private var lastGoldorTerm = 0
    private var gateDestroyed = false
    private var termStage = 0
    private var relicCount = 0
    private var dragonCount = 0
    private var stormEnraged = false

    init {
        on<WorldEvent.Load> {
            reset()
        }

        on<TickEvent.Server> {
            if (!isF7OrM7) return@on
            tickCounter++

            // Scroll logic
            if (currentBreakdownLines.isNotEmpty() && currentBreakdownLines.size > scrollLength) {
                val now = System.currentTimeMillis()
                if (now - lastScrollTime > scrollDelay * 1000L) {
                    currentBreakdownScrollIndex = (currentBreakdownLines.size - scrollLength).coerceAtLeast(0)
                    lastScrollTime = now
                }
            }
        }

        on<ChatPacketEvent> {
            if (!isF7OrM7) return@on
            val msg = value.noControlCodes
            handleChat(msg)
        }

        onReceive<ClientboundSoundPacket> {
            if (!isF7OrM7 || currentStage != 9 || !breakdownEnabled) return@onReceive
            if (sound.value().location.path == "mob.wither.death" || sound.value().location.path == "entity.wither.death") {
                relicCount++
                if (relicCount == 5) {
                    addBreakdownEntry("Relics")
                }
            }
        }
    }

    private fun reset() {
        currentRun.clear()
        currentBreakdownLines.clear()
        currentStage = 0
        tickCounter = 0
        currentBreakdownScrollIndex = 0
        resetBreakdownState()
    }

    private fun resetBreakdownState() {
        lastBreakdownTime = System.currentTimeMillis()
        lastBreakdownTick = tickCounter
        passedMaxorCrystals = 0
        lastGoldorTerm = 0
        gateDestroyed = false
        termStage = 0
        relicCount = 0
        dragonCount = 0
        stormEnraged = false
    }


    private fun handleChat(msg: String) {
        // Start check
        if (msg == "[NPC] Mort: Here, I found this map when I first entered the dungeon.") {
            reset()
            startTimestamp = System.currentTimeMillis()
            startTick = tickCounter
            currentStage = 1 // Start waiting for Blood Open (Stage 1)
            return
        }

        // Breakdown Logic (Must run before Main Splits to capture end-of-stage events like Necron Kill)
        handleBreakdown(msg)

        // Main Splits Logic
        val currentCriteria = MAINSPLITSTRINGS.getOrNull(currentStage)

        // Special case for Blood Start which has multiple messages
        if (currentStage == 1) {
            if (BLOODSTARTMESSAGES.any { msg == it }) {
                advanceStage("Blood Open")
            }
        } else if (currentStage == 6 && msg == "The Core entrance is opening!") { // Goldor special case
            if (termStage == 3) addBreakdownEntry("Section 4") // Final section done
            advanceStage("Terminals")
        } else if (currentCriteria != null && msg == currentCriteria) {
            // currentStage is index in MAINSPLITSTRINGS we matched.
            // We want to label the STAGE that JUST finished.
            // If we just matched Watcher (Stage 2), we finished "Watcher" segment.
            // STAGE_LABELS[1] is "Watcher".
            // So label is STAGE_LABELS[currentStage - 1].
            val stageName = STAGE_LABELS.getOrNull(currentStage - 1)?.first ?: "Unknown"
            advanceStage(stageName)
        }
    }

    private fun advanceStage(stageName: String) {
        val now = System.currentTimeMillis()
        val currentTick = tickCounter

        val timeDiff = now - startTimestamp
        val tickDiff = currentTick - startTick

        // Check PB using Odin's PersonalBest
        // It handles internal logic for "Is it a PB?" and "Send message"
        // Key is stageName, Time is seconds (Float)
        if (stageName != "Dragons" || (DungeonListener.floor?.floorNumber == 7 && DungeonListener.floor?.isMM == true)) {
            pb.time(stageName, timeDiff / 1000f, "s", "§b$stageName §6> §a", true)
        }

        currentRun.add(SplitEntry(stageName, timeDiff, tickDiff))

        // Prepare for next stage
        startTimestamp = now
        startTick = currentTick

        // If finishing Necron (Stage 8), we are entering Dragons (Stage 9). Print breakdown.
        if (currentStage == 8) {
            printBreakdown()
        }

        currentStage++

        // Trigger breakdown header for new stage if applicable
        addBreakdownHeader(currentStage)
    }

    private fun addBreakdownHeader(stage: Int) {
        when (stage) {
            4 -> currentBreakdownLines.add("§7§n§lMAXOR BREAKDOWN") // Entering Maxor
            5 -> currentBreakdownLines.add("§7§n§lSTORM BREAKDOWN") // Entering Storm
            6 -> currentBreakdownLines.add("§7§n§lGOLDOR BREAKDOWN") // Entering Goldor (Terminals)
            // 7 -> Entering Goldor Tunnel. Still Goldor phase. Do not add Necron header yet.
            8 -> currentBreakdownLines.add("§7§n§lNECRON BREAKDOWN") // Entering Necron (Necron Start)
            9 -> if (DungeonListener.floor?.floorNumber == 7 && DungeonListener.floor?.isMM == true)
                currentBreakdownLines.add("§7§n§lDRAGONS BREAKDOWN") // Entering Dragons (M7 only)
        }
        lastBreakdownTime = System.currentTimeMillis()
        lastBreakdownTick = tickCounter
        // Auto scroll
        currentBreakdownScrollIndex = (currentBreakdownLines.size - scrollLength).coerceAtLeast(0)
    }

    private fun handleBreakdown(msg: String) {
        // Maxor
        if (msg == "The Energy Laser is charging up!") {
            passedMaxorCrystals++
            if (passedMaxorCrystals == 2) {
                addBreakdownEntry("Crystals")
            }
        } else if (msg == MAINSPLITSTRINGS[4]) { // Storm entry message
            if (passedMaxorCrystals >= 2) {
                addBreakdownEntry("Maxor Kill")
            }
        }

        // Storm
        if (msg == "⚠ Storm is enraged! ⚠") {
            stormEnraged = true
            addBreakdownEntry("First Pillar Kill") // Assuming straight to enraged means p1 done? Or just simplify
        } else if (msg == MAINSPLITSTRINGS[5]) { // Goldor entry
            addBreakdownEntry("Storm Kill")
        }

        // Goldor
        if (msg == "The gate has been destroyed!") {
            gateDestroyed = true
            checkForwardTermStage()
        }
        val termMatch =
            Regex(".+ (?:activated|completed) a .+! \\((\\d+)/(\\d+)\\)").find(msg)

        if (termMatch != null) {
            val (currentStr, totalStr) = termMatch.destructured
            val current = currentStr.toInt()
            val total = totalStr.toInt()

            if (current > lastGoldorTerm && current == total) {
                lastGoldorTerm = current
                checkForwardTermStage()
            }
        }

        // Necron
        if (msg == MAINSPLITSTRINGS[7]) { // Necron Start (Goldor End)
            addBreakdownEntry("Goldor Kill")
        } else if (msg == MAINSPLITSTRINGS[8]) { // "All this, for nothing..."
            addBreakdownEntry("Necron Kill")
        }

        // Dragons
        if (DRAGONKILLSTRINGS.any { msg == it }) {
            dragonCount++
            if (dragonCount > 5) return
            if (dragonCount > 2) { // 3rd, 4th, 5th dragons
                addBreakdownEntry(if (dragonCount == 3) "Third Dragon Kill" else if (dragonCount == 4) "Fourth Dragon Kill" else "Fifth Dragon Kill")
            }
        }
    }

    private fun checkForwardTermStage() {
        if (gateDestroyed && (lastGoldorTerm == 7 || lastGoldorTerm == 8)) {
            gateDestroyed = false
            lastGoldorTerm = 0
            termStage++

            val label = when (termStage) {
                1 -> "Section 1"
                2 -> "Section 2"
                3 -> "Section 3" // Wait, logic might be slightly off compared to JS, JS does section 1,2,3,4 then goldor kill
                else -> "Unknown Section"
            }
            addBreakdownEntry(label)
        }
    }

    private fun printBreakdown() {
        if (currentBreakdownLines.isEmpty()) return

        val message = Component.empty()
        message.append(Component.literal("§b-----------------------------------------------------§r\n"))
        message.append(Component.literal("§6§lTrue Splits Breakdown§r\n"))

        val sections = mutableMapOf<String, MutableList<String>>()
        var currentSection = "Global"

        currentBreakdownLines.forEach { line ->
            if (line.startsWith("§7§n§l")) {
                currentSection = line
                sections[currentSection] = mutableListOf()
            } else {
                sections.getOrPut(currentSection) { mutableListOf() }.add(line)
            }
        }

        sections.forEach { (header, lines) ->
            if (header == "Global" && lines.isEmpty()) return@forEach // Skip empty start

            val cleanHeader = header.replace("§7§n§l", "").replace(" BREAKDOWN", "")
            val hoverContent = Component.empty()
            hoverContent.append(Component.literal("$header\n"))
            lines.forEach { l -> hoverContent.append(Component.literal("$l\n")) }

            val button = Component.literal(" §b[View §r$cleanHeader§b]§r ")
            button.style = Style.EMPTY.withHoverEvent(HoverEvent.ShowText(hoverContent))

            message.append(button)
        }
        message.append(Component.literal("\n§b-----------------------------------------------------§r"))

        net.kumajunk.libleaddon.utils.sendMessage(message)
    }

    private fun addBreakdownEntry(label: String) {
        val now = System.currentTimeMillis()
        val diff = now - lastBreakdownTime
        currentBreakdownLines.add("§b$label > §a${(diff / 1000f).toFixed(2)}s")
        lastBreakdownTime = now
        lastBreakdownTick = tickCounter
        // Auto scroll logic
        currentBreakdownScrollIndex = (currentBreakdownLines.size - scrollLength).coerceAtLeast(0)
    }

    private fun getCurrentBreakdownLabel(): String {
        return when (currentStage) {
            4 -> if (passedMaxorCrystals < 2) "Crystals" else "Maxor Kill"
            5 -> if (!stormEnraged) "First Pillar" else "Storm Kill"
            6 -> when (termStage) {
                0 -> "Section 1"
                1 -> "Section 2"
                2 -> "Section 3"
                3 -> "Section 4"
                else -> "Terminals"
            }

            7 -> "Goldor Kill"
            8 -> "Necron Kill"
            9 -> "Dragons"
            else -> "Split"
        }
    }

    private fun createHudText(stage: Int, label: String, realTime: Long, tickTime: Long): String {
        // Hide Dragons split if not in Master Mode
        if (label == "Dragons") {
            val floor = DungeonListener.floor
            if (floor == null || floor.floorNumber != 7 || !floor.isMM) return ""
        }

        val colorConstraint = STAGE_LABELS.getOrNull(stage)
        val baseColor = colorConstraint?.second ?: "§f"
        val activeMarker = if (stage == currentStage - 1) "§n" else ""

        val timeColor = "§a"

        return "§b$activeMarker$baseColor$label§b §6> $timeColor${(realTime / 1000f).toFixed(2)}s $timeColor(${
            (tickTime / 20f).toFixed(
                2
            )
        }s)"
    }

    // Constants
    private val BLOODSTARTMESSAGES = listOf(
        "[BOSS] The Watcher: Congratulations, you made it through the Entrance.",
        "[BOSS] The Watcher: Ah, you've finally arrived.",
        "[BOSS] The Watcher: Ah, we meet again...",
        "[BOSS] The Watcher: So you made it this far... interesting.",
        "[BOSS] The Watcher: You've managed to scratch and claw your way here, eh?",
        "[BOSS] The Watcher: I'm starting to get tired of seeing you around here...",
        "[BOSS] The Watcher: Oh.. hello?",
        "[BOSS] The Watcher: Things feel a little more roomy now, eh?"
    )

    private val MAINSPLITSTRINGS = listOf(
        "[NPC] Mort: Here, I found this map when I first entered the dungeon.", // 0 (Start)
        "BLOOD_START_PLACEHOLDER", // 1 (Blood Open) - Handled separately
        "[BOSS] The Watcher: You have proven yourself. You may pass.", // 2 (Watcher)
        "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!", // 3 (Portal Entry / Maxor Start)
        "[BOSS] Storm: Pathetic Maxor, just like expected.", // 4 (Storm Start)
        "[BOSS] Goldor: Who dares trespass into my domain?", // 5 (Goldor Start)
        "The Core entrance is opening!", // 6 (Terminals Done / Goldor Phase 2?) 
        "[BOSS] Necron: You went further than any human before, congratulations.", // 7 (Necron Start)
        "[BOSS] Necron: All this, for nothing...", // 8 (Necron Kill / End F7)
        "                             > EXTRA STATS <",
    )

    val DRAGONKILLSTRINGS = listOf(
        "[BOSS] Wither King: Oh, this one hurts!",
        "[BOSS] Wither King: I have more of those.",
        "[BOSS] Wither King: My soul is disposable."
    )

    private val STAGE_LABELS = listOf(
        "Blood Open" to "§2",
        "Watcher" to "§b",
        "Portal" to "§d",
        "Maxor" to "§5",
        "Storm" to "§3",
        "Terminals" to "§6",
        "Goldor" to "§6",
        "Necron" to "§c",
        "Dragons" to "§4"
    )
}

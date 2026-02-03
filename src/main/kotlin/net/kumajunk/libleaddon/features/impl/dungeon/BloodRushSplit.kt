package net.kumajunk.libleaddon.features.impl.dungeon

import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.dungeon.map.SpecialColumn
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.skyblock.dungeon.tiles.RoomType
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.kumajunk.libleaddon.features.impl.dungeon.map.DungMap
import net.kumajunk.libleaddon.features.impl.dungeon.map.MapScanner
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

/**
 * Blood Rush中のスプリットタイムを計測・表示するモジュール
 */
object BloodRushSplit : Module(
    name = "Blood Rush Split(LA)",
    description = "Tracks and displays split times during Blood Rush."
) {
    private val showTotalTime by BooleanSetting(
        name = "Show Total Time",
        default = false,
        desc = "Displays the total time taken for the Blood Rush at the end."
    )

    private val rooms = mutableListOf<String>()
    private val clearTimes = mutableListOf<Long>()
    private var brStart = 0L
    private val omitRooms = listOf("Entrance", "Fairy", "Blood")

    init {
        // ワールドロード時にリセット
        on<WorldEvent.Load> { reset() }

        on<TickEvent.End> {
            MapScanner.scan(world)
        }

        ClientChunkEvents.CHUNK_LOAD.register { _, _ ->
            DungMap.onChunkLoad()
        }

        onReceive<ClientboundMapItemDataPacket> {
            DungMap.rescanMapItem(this)
        }

        // チャットメッセージ検知
        onReceive<ClientboundSystemChatPacket> {
            val msg = content.string?.noControlCodes ?: return@onReceive

            // Blood Rush開始（Mortのメッセージ）
            if (msg.contains("Mort:") && msg.contains("I found this map")) {
                brStart = System.currentTimeMillis()
                clearTimes.add(0L)
            }

            // WITHERドア開放 -> スプリット記録
            if (brStart > 0 && msg.contains("opened a WITHER door")) {
                clearTimes.add(System.currentTimeMillis() - brStart)
            }

            // BLOODドア開放 -> ルート計算・結果表示
            if (brStart > 0 && msg.contains("BLOOD DOOR") && msg.contains("opened")) {
                clearTimes.add(System.currentTimeMillis() - brStart)

                // ルート計算
                val route = MapScanner.getRouteBetween(RoomType.ENTRANCE, RoomType.BLOOD)
                    .filter { it !in omitRooms }
                rooms.addAll(route)

                displaySplits()
                reset()
            }
        }
    }

    /**
     * スプリットタイムをチャットに表示
     */
    private fun displaySplits() {
        val message = buildString {
            append("\n§f§m------------------------------§r\n")
            append("§c§lBlood Rush Splits:\n")
            for (i in rooms.indices) {
                if (i + 1 < clearTimes.size) {
                    val time = (clearTimes[i + 1] - clearTimes[i]) / 1000.0
                    append("§f${rooms[i]}: §b${String.format("%.2f", time)}s\n")
                }
            }
            if (showTotalTime) append("\n§b§lTotal Time§f: ${String.format("%.2f", (clearTimes.last() / 1000.0))}s")
            append("\n§f§m------------------------------§r\n")
        }
        mc.execute { mc.gui?.chat?.addMessage(Component.literal(message)) }
    }

    /**
     * 状態をリセット
     */
    private fun reset() {
        SpecialColumn.unload()
        MapScanner.unload()
        DungMap.unload()
        rooms.clear()
        clearTimes.clear()
        brStart = 0L
    }
}

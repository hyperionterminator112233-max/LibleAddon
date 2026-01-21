package net.kumajunk.libleaddon.features.impl.dungeon

import com.odtheking.odin.clickgui.settings.Setting.Companion.withDependency
import com.odtheking.odin.clickgui.settings.impl.BooleanSetting
import com.odtheking.odin.clickgui.settings.impl.ColorSetting
import com.odtheking.odin.clickgui.settings.impl.DropdownSetting
import com.odtheking.odin.clickgui.settings.impl.NumberSetting
import com.odtheking.odin.events.RenderEvent
import com.odtheking.odin.events.TickEvent
import com.odtheking.odin.events.WorldEvent
import com.odtheking.odin.events.core.on
import com.odtheking.odin.events.core.onReceive
import com.odtheking.odin.features.Module
import com.odtheking.odin.features.impl.dungeon.map.*
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.withAlpha
import com.odtheking.odin.utils.Colors
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.itemId
import com.odtheking.odin.utils.noControlCodes
import com.odtheking.odin.utils.render.drawFilledBox
import com.odtheking.odin.utils.render.drawWireFrameBox
import com.odtheking.odin.utils.render.hollowFill
import com.odtheking.odin.utils.skyblock.dungeon.DungeonUtils
import com.odtheking.odin.utils.skyblock.dungeon.tiles.RoomState
import com.odtheking.odin.utils.skyblock.dungeon.tiles.RoomType
import net.kumajunk.libleaddon.features.impl.dungeon.map.Door
import net.kumajunk.libleaddon.features.impl.dungeon.map.MapScanner
import net.kumajunk.libleaddon.features.impl.dungeon.map.DungMap
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.world.phys.AABB
import kotlin.collections.iterator
import java.awt.Color as AwtColor

object IllegalMap : Module(
    name = "Illegal Map(LA)",
    description = "Customizable dungeon map with room colors, door colors, and player names."
) {
    var backgroundColor by ColorSetting("Background Color", Color(0, 0, 0, 0.7f), true, desc = "The background color of the map.")
    var backgroundSize by NumberSetting("Background Size", 5f, 0f, 20f, 1f, desc = "The size of the background border.")

    var textScaling by NumberSetting("Text Scaling", 0.45f, 0.1f, 1f, 0.05f, desc = "Scale of room names.")

    private val playerDropdown by DropdownSetting("Player Settings")
    var playerHeadBackgroundSize by NumberSetting("Player Head BG Size", 1, 0, 10, 1, desc = "Size of player head background.").withDependency { playerDropdown }
    var playerNamesScaling by NumberSetting("Player Names Scaling", 0.75f, 0.1f, 2f, 0.05f, desc = "Scale of player names.").withDependency { playerDropdown }
    var playerNameColor by ColorSetting("Player Name Color", Color(70, 70, 70), false, desc = "Color of player names.").withDependency { playerDropdown }

    private val doorDropdown by DropdownSetting("Door Settings")
    var doorThickness by NumberSetting("Door Thickness", 9, 1, 20, 1, desc = "Thickness of doors on map.").withDependency { doorDropdown }
    var unopenedDoorColor by ColorSetting("Unopened Door", Color(30, 30, 30), false, desc = "Color of unopened doors.").withDependency { doorDropdown }
    var bloodDoorColor by ColorSetting("Blood Door", Colors.MINECRAFT_RED, false, desc = "Color of blood room doors.").withDependency { doorDropdown }
    var witherDoorColor by ColorSetting("Wither Door", Colors.BLACK, false, desc = "Color of wither doors.").withDependency { doorDropdown }
    var normalDoorColor by ColorSetting("Normal Door", Color(107, 58, 17), false, desc = "Color of normal doors.").withDependency { doorDropdown }
    var puzzleDoorColor by ColorSetting("Puzzle Door", Color(117, 0, 133), false, desc = "Color of puzzle doors.").withDependency { doorDropdown }
    var championDoorColor by ColorSetting("Champion Door", Color(254, 223, 0), false, desc = "Color of champion doors.").withDependency { doorDropdown }
    var trapDoorColor by ColorSetting("Trap Door", Color(216, 127, 51), false, desc = "Color of trap doors.").withDependency { doorDropdown }
    var entranceDoorColor by ColorSetting("Entrance Door", Color(20, 133, 0), false, desc = "Color of entrance doors.").withDependency { doorDropdown }
    var fairyDoorColor by ColorSetting("Fairy Door", Color(244, 19, 139), false, desc = "Color of fairy room doors.").withDependency { doorDropdown }
    var rareDoorColor by ColorSetting("Rare Door", Color(255, 203, 89), false, desc = "Color of rare doors.").withDependency { doorDropdown }

    // Room Settings
    private val roomDropdown by DropdownSetting("Room Settings")
    var darkenMultiplier by NumberSetting("Darken Multiplier", 0.4f, 0f, 1f, 0.05f, desc = "Multiplier for darkening rooms.").withDependency { roomDropdown }
    var unopenedRoomColor by ColorSetting("Unopened Room", Color(30, 30, 30), false, desc = "Color of unopened rooms.").withDependency { roomDropdown }
    var bloodRoomColor by ColorSetting("Blood Room", Color(255, 0, 0), false, desc = "Color of blood rooms.").withDependency { roomDropdown }
    var normalRoomColor by ColorSetting("Normal Room", Color(107, 58, 17), false, desc = "Color of normal rooms.").withDependency { roomDropdown }
    var puzzleRoomColor by ColorSetting("Puzzle Room", Color(117, 0, 133), false, desc = "Color of puzzle rooms.").withDependency { roomDropdown }
    var championRoomColor by ColorSetting("Champion Room", Color(254, 223, 0), false, desc = "Color of champion rooms.").withDependency { roomDropdown }
    var trapRoomColor by ColorSetting("Trap Room", Color(216, 127, 51), false, desc = "Color of trap rooms.").withDependency { roomDropdown }
    var entranceRoomColor by ColorSetting("Entrance Room", Color(20, 133, 0), false, desc = "Color of entrance rooms.").withDependency { roomDropdown }
    var fairyRoomColor by ColorSetting("Fairy Room", Color(244, 19, 139), false, desc = "Color of fairy rooms.").withDependency { roomDropdown }
    var rareRoomColor by ColorSetting("Rare Room", Color(255, 203, 89), false, desc = "Color of rare rooms.").withDependency { roomDropdown }

    // Door ESP Settings
    private val doorEspDropdown by DropdownSetting("Door ESP")
    var doorEspEnabled by BooleanSetting("Enable Door ESP", false, desc = "Show Wither/Blood doors in 3D when in adjacent room.").withDependency { doorEspDropdown }
    var doorEspFilled by BooleanSetting("Filled ESP", false, desc = "Draw ESP with filled box.").withDependency { doorEspDropdown }

    // Key tracking
    var hasWitherKey: Boolean = false
        private set
    var hasBloodKey: Boolean = false
        private set
    var brComplete: Boolean = false
        private set

    private val mapHud by HUD("Dungeon Map", "Displays the dungeon map with customizable colors.") { example ->
        when {
            !DungeonUtils.inClear && !example -> 0 to 0
            example -> renderExampleMap()
            else -> renderDungeonMap()
        }
    }

    private fun GuiGraphics.renderExampleMap(): Pair<Int, Int> {
        val roomsX = 116
        val roomsZ = 116
        val offset = backgroundSize.toInt()

        fill(0, 0, roomsX + offset * 2, roomsZ + offset * 2, backgroundColor.rgba)
        drawCenteredString(mc.font, "MAP", roomsX / 2 + offset, roomsZ / 2 + offset - mc.font.lineHeight, AwtColor.WHITE.rgb)

        return (roomsX + offset * 2) to (roomsZ + offset * 2)
    }

    private fun GuiGraphics.renderDungeonMap(): Pair<Int, Int> {
        val matrices = pose()
        val mapSize = DungMap.calculateMapSize()
        val roomsX = mapSize.x * 16 + (mapSize.x - 1) * 4
        val roomsZ = mapSize.z * 16 + (mapSize.z - 1) * 4
        val offset = backgroundSize.toInt() * 2

        matrices.pushMatrix()

        fill(0, 0, roomsX + offset, roomsZ + offset, backgroundColor.rgba)
        hollowFill(0, 0, roomsX + offset, roomsZ + offset, 1, Colors.gray26)
        matrices.translate(backgroundSize, backgroundSize)

        for (room in MapScanner.allRooms.values) {
            for (tile in room.tiles) renderTile(tile)
        }

        for (door in MapScanner.doors) {
            // if (!door.seen) continue
            renderTile(door)
            for (roomTile in door.rooms) {
                if (roomTile.owner.state == RoomState.UNOPENED) renderTile(roomTile)
            }
        }

        val fontHeight = mc.font.lineHeight
        val textFactor = 1 / textScaling

        for ((name, room) in MapScanner.allRooms) {
            if (room.data.type.equalsOneOf(RoomType.FAIRY, RoomType.ENTRANCE, RoomType.BLOOD)) continue

            val splitName = name.split(" ")
            val defaultHeight = 8 - fontHeight / (2 * textFactor) - ((splitName.size - 1) / 2f * (fontHeight / textFactor)).toInt()
            val placement = room.textPlacement()
            val color = when (room.state) {
                RoomState.GREEN -> Colors.MINECRAFT_GREEN
                RoomState.CLEARED -> Colors.WHITE
                RoomState.DISCOVERED -> Color(100, 100, 100)
                RoomState.FAILED -> Colors.MINECRAFT_RED
                else -> Colors.WHITE
            }.rgba

            for ((index, text) in splitName.withIndex()) {
                matrices.pushMatrix()
                matrices.translate(
                    placement.x + 8f,
                    placement.z + index * (fontHeight / textFactor) + defaultHeight
                )
                matrices.scale(textScaling)
                drawCenteredString(mc.font, text, 0, 0, color)
                matrices.popMatrix()
            }
        }

        if (!DungeonUtils.inBoss) {
            val renderNames = mc.player?.mainHandItem?.itemId?.equalsOneOf("INFINITE_SPIRIT_LEAP", "SPIRIT_LEAP") == true

            for (player in DungeonUtils.dungeonTeammates) {
                if (player.isDead) continue

                val (posX, posZ) = player.mapRenderPosition()

                matrices.pushMatrix()
                matrices.translate(posX, posZ)

                if (renderNames) {
                    matrices.pushMatrix()
                    matrices.scale(playerNamesScaling)
                    drawCenteredString(mc.font, player.name, 0, 8, playerNameColor.rgba)
                    matrices.popMatrix()
                }

                player.locationSkin?.let { skin ->
                    matrices.rotate(Math.toRadians(180.0 + player.mapRenderYaw()).toFloat())

                    if (playerHeadBackgroundSize != 0) {
                        val size = 5 + playerHeadBackgroundSize
                        fill(-size, -size, size, size, player.clazz.color.rgba)
                    }

                    PlayerFaceRenderer.draw(this, skin, -5, -5, 10, false, false, -1)
                }

                matrices.popMatrix()
            }
        }

        matrices.popMatrix()

        return (roomsX + offset) to (roomsZ + offset)
    }

    private fun GuiGraphics.renderTile(tile: Tile) {
        val size = tile.size()
        if (size == Vec2i(0, 0)) return

        val placement = tile.placement()
        val colors = tile.color()

        pose().pushMatrix()
        pose().translate(placement.x.toFloat(), placement.z.toFloat())

        when (colors.size) {
            1 -> fill(0, 0, size.x, size.z, colors[0].rgba)
            2 -> {
                fill(0, 0, 16, 8, colors[0].rgba)
                fill(0, 8, 16, 16, colors[1].rgba)
            }
            3 -> {
                fill(0, 0, size.x, 5, colors[0].rgba)
                fill(0, 0, 5, 10, colors[0].rgba)
                fill(10, 5, 16, 16, colors[1].rgba)
                fill(0, 10, 16, 16, colors[1].rgba)
                fill(5, 5, 11, 11, colors[2].rgba)
            }
        }

        pose().popMatrix()
    }

    /**
     * Renders Door ESP for Wither/Blood doors adjacent to current room.
     * Can be called externally with RenderEvent.Extract context.
     */
    private fun RenderEvent.Extract.renderDoorEsp() {
        if (!DungeonUtils.inClear) return
        if (brComplete) return

        val currentRoomName = DungeonUtils.currentRoomName

        for (door in MapScanner.doors) {
            // Only Wither or Blood doors
            if (door.type !in listOf(Door.Type.WITHER, Door.Type.BLOOD)) continue

            // Skip if door is not locked
            if (!door.locked) continue

            // Check if door is adjacent to current room
            val isAdjacentToCurrentRoom = door.rooms.any { roomTile ->
                roomTile.owner.data.name == currentRoomName
            }
            if (!isAdjacentToCurrentRoom) continue

            // Color based on key possession
            val hasKey = when (door.type) {
                Door.Type.WITHER -> hasWitherKey
                Door.Type.BLOOD -> hasBloodKey
                else -> false
            }
            val color = if (hasKey) Colors.MINECRAFT_GREEN else Colors.MINECRAFT_RED

            // Calculate AABB (3x3x4) from door position
            val x = door.pos.x.toDouble() + 0.5
            val z = door.pos.z.toDouble() + 0.5
            val box = AABB(x - 1.5, 69.0, z - 1.5, x + 1.5, 73.0, z + 1.5)

            drawWireFrameBox(box, color, 2f, false)
            if (doorEspFilled) {
                drawFilledBox(box, color.withAlpha(0.3f, false), false)
            }
        }
    }

    init {
        on<WorldEvent.Load> {
            SpecialColumn.unload()
            MapScanner.unload()
            DungMap.unload()

            // Reset key counts
            hasWitherKey = false
            hasBloodKey = false
            brComplete = false
        }

        on<TickEvent.End> {
            MapScanner.scan(world)
        }

        ClientChunkEvents.CHUNK_LOAD.register { _, _ ->
            DungMap.onChunkLoad()
        }

        onReceive<ClientboundMapItemDataPacket> {
            DungMap.rescanMapItem(this)
        }

        // Chat detection for key pickup/consumption
        onReceive<ClientboundSystemChatPacket> {
            val msg = content.string?.noControlCodes ?: return@onReceive

            // Key pickup detection
            when {
                msg.contains("Wither Key") && msg.contains("obtained") -> hasWitherKey = true
                msg.contains("Blood Key") && msg.contains("obtained") -> hasBloodKey = true
            }

            // Door open detection (key consumption)
            when {
                msg.contains("WITHER DOOR") && msg.contains("has been opened") -> hasWitherKey = false
                msg.contains("BLOOD DOOR") && msg.contains("has been opened") -> {
                    hasBloodKey = false
                    brComplete = true
                }
            }
        }

        // Door ESP rendering
        on<RenderEvent.Extract> {
            if (!doorEspEnabled) return@on
            renderDoorEsp()
        }
    }
}

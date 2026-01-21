package net.kumajunk.libleaddon.features.impl.dungeon.map

import net.kumajunk.libleaddon.features.impl.dungeon.IllegalMap
import com.odtheking.odin.features.impl.dungeon.map.Tile
import com.odtheking.odin.features.impl.dungeon.map.Vec2i
import com.odtheking.odin.utils.Color
import com.odtheking.odin.utils.Color.Companion.darker
import com.odtheking.odin.utils.equalsOneOf
import com.odtheking.odin.utils.skyblock.dungeon.tiles.RoomState
import com.odtheking.odin.utils.skyblock.dungeon.tiles.RoomType

class Door(pos: Vec2i, var type: Type, val rooms: List<MapRoom.RoomTile>): Tile(pos) {
    enum class Type { BLOOD, NORMAL, WITHER }

    var locked = type.equalsOneOf(Type.WITHER, Type.BLOOD)
    val seen get() = rooms.any { it.owner.state !in setOf(RoomState.UNDISCOVERED, RoomState.UNOPENED) }

    override fun size(): Vec2i {
        val xOffset = ((pos.x + 185) shr 4) % 2
        val zOffset = ((pos.z + 185) shr 4) % 2
        return Vec2i(
            (xOffset xor 1) * IllegalMap.doorThickness + xOffset * 4,
            (zOffset xor 1) * IllegalMap.doorThickness + zOffset * 4
        )
    }

    override fun placement(): Vec2i {
        val x = (pos.x + 185) shr 4
        val z = (pos.z + 185) shr 4
        val xEven = x % 2
        val zEven = z % 2
        val thicknessBasedOffset = (16 - IllegalMap.doorThickness) / 2
        val xOffset = (x shr 1) * 20 + xEven * 16 + (xEven xor 1) * thicknessBasedOffset
        val yOffset = (z shr 1) * 20 + zEven * 16 + (zEven xor 1) * thicknessBasedOffset
        return Vec2i(xOffset, yOffset)
    }

    override fun color(): Array<Color> {
        val hasUnopenedRoom = rooms.any { it.owner.state in setOf(RoomState.UNDISCOVERED, RoomState.UNOPENED) }

        return when {
            hasUnopenedRoom && type != Type.NORMAL -> getLockedDoorColor()
            hasUnopenedRoom -> arrayOf(IllegalMap.unopenedDoorColor)
            else -> arrayOf(getOpenDoorColor())
        }
    }

    private fun getLockedDoorColor(): Array<Color> = when (type) {
        Type.BLOOD -> arrayOf(if (locked) IllegalMap.bloodDoorColor.darker(IllegalMap.darkenMultiplier) else IllegalMap.bloodDoorColor)
        Type.WITHER -> arrayOf(if (locked) IllegalMap.witherDoorColor.darker(IllegalMap.darkenMultiplier) else IllegalMap.witherDoorColor)
        Type.NORMAL -> arrayOf(IllegalMap.unopenedDoorColor)
    }

    private fun getOpenDoorColor(): Color = when (type) {
        Type.BLOOD -> IllegalMap.bloodDoorColor
        Type.WITHER -> if (locked) IllegalMap.witherDoorColor
                      else rooms.firstOrNull { it.owner.data.type == RoomType.FAIRY }?.let { IllegalMap.fairyDoorColor }
                           ?: IllegalMap.normalDoorColor
        Type.NORMAL -> getDoorColorByRoomType()
    }

    private fun getDoorColorByRoomType(): Color {
        val specialRoom = rooms.firstOrNull { it.owner.data.type !in setOf(RoomType.NORMAL, RoomType.FAIRY) }
        return when (specialRoom?.owner?.data?.type) {
            RoomType.ENTRANCE -> IllegalMap.entranceDoorColor
            RoomType.BLOOD -> IllegalMap.bloodDoorColor
            RoomType.CHAMPION -> IllegalMap.championDoorColor
            RoomType.PUZZLE -> IllegalMap.puzzleDoorColor
            RoomType.RARE -> IllegalMap.rareDoorColor
            RoomType.TRAP -> IllegalMap.trapDoorColor
            else -> IllegalMap.normalDoorColor
        }
    }
}


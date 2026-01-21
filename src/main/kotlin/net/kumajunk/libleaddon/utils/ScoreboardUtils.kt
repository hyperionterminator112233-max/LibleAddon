package net.kumajunk.libleaddon.utils

import com.odtheking.odin.OdinMod.mc
import net.minecraft.network.chat.Component
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.PlayerTeam

object ScoreboardUtils {

    /**
     * 現在のスコアボード（サイドバー）のタイトルを取得
     */
    fun getScoreboardTitle(): String {
        val scoreboard = mc.level?.scoreboard ?: return ""
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return ""
        return objective.displayName.string
    }

    /**
     * スコアボード（サイドバー）の行を取得（フォーマットコード付き）
     */
    fun getSidebarLines(): List<String> {
        val scoreboard = mc.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        
        return scoreboard.listPlayerScores(objective)
            .sortedByDescending { it.value }
            .take(15) // スコアボードの最大行数は15
            .map { entry ->
                val team = scoreboard.getPlayersTeam(entry.owner)
                // チームのプレフィックス/サフィックスを適用して完全なテキストを生成
                PlayerTeam.formatNameForTeam(team, Component.literal(entry.owner)).string
            }
    }
    
    /**
     * 色コードを除去したクリーンなスコアボードの行を取得
     */
    fun getCleanSidebarLines(): List<String> {
        return getSidebarLines().map { stripColor(it) }
    }

    data class DungeonPlayerInfo(
        val className: String,
        val level: Int?
    )

    private val classMap = mapOf(
        "A" to "Archer",
        "B" to "Berserk",
        "H" to "Healer",
        "T" to "Tank",
        "M" to "Mage"
    )

    fun getDungeonPlayerClasses(): Map<String, DungeonPlayerInfo> {
        val lines = getCleanSidebarLines()
        val playerClasses = mutableMapOf<String, DungeonPlayerInfo>()

        // Remove unicode and match
        // Regex: [ClassChar] PlayerName [Lv(Level)]?
        val regex = Regex("^\\[(\\w+)]\\s+(\\w+)(?:\\s+\\[Lv(\\d+)])?")

        lines.forEach { line ->
            // removeUnicode: ASCII以外を除去
            val cleaned = line.replace(Regex("[^\\x00-\\x7F]"), "").trim()
            val match = regex.find(cleaned)

            if (match != null) {
                // destructured ではグループ数が固定されるため、optional group がマッチしなかった場合も
                // 空文字列として取得される (Kotlin仕様)
                val (classChar, name, levelStr) = match.destructured
                val level = levelStr.toIntOrNull()
                val className = classMap[classChar] ?: classChar

                playerClasses[name] = DungeonPlayerInfo(className, level)
            }
        }
        return playerClasses
    }

    /**
     * 特定のプレイヤーのダンジョン情報を取得
     */
    fun getDungeonPlayer(playerName: String?): DungeonPlayerInfo? {
        if (playerName == null) return null
        return getDungeonPlayerClasses()[playerName]
    }

    private fun stripColor(input: String): String {
        return input.replace(Regex("(?i)§[0-9A-Z]"), "")
    }
}

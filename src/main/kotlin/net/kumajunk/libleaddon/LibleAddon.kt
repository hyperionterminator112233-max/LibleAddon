package net.kumajunk.libleaddon

import com.odtheking.odin.OdinMod.logger
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.OdinMod.version
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.network.WebUtils
import com.odtheking.odin.utils.network.WebUtils.fetchJson
import com.odtheking.odin.utils.network.WebUtils.gson
import com.odtheking.odin.utils.network.WebUtils.postData
import com.odtheking.odin.utils.network.hypixelapi.RequestUtils.getUuid
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kumajunk.libleaddon.commands.profileViewerCommand
import net.kumajunk.libleaddon.features.impl.dungeon.AutoPotionBag
import net.kumajunk.libleaddon.features.impl.dungeon.BloodRushSplit
import net.kumajunk.libleaddon.features.impl.dungeon.IllegalMap
import net.kumajunk.libleaddon.features.impl.dungeon.StarMobHighlight
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.kumajunk.libleaddon.features.impl.dungeon.CalcLagLoss
import net.kumajunk.libleaddon.features.impl.dungeon.ClassDupeNotifier
import net.kumajunk.libleaddon.features.impl.dungeon.LeapAnnounce
import net.kumajunk.libleaddon.features.impl.dungeon.MaskTimer
import net.kumajunk.libleaddon.features.impl.dungeon.ScoreMilestone
import net.kumajunk.libleaddon.features.impl.skyblock.AutoRefill
import net.minecraft.world.scores.Score
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.UUID

object LibleAddon : ClientModInitializer {

    var playerUUID: UUID? = null
    var playerName: String? = null

    override fun onInitializeClient() {
        scope.launch {
            if (isJapanIp()) {
                // Register commands by adding to the array
                ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
                    arrayOf(profileViewerCommand).forEach { commodore -> commodore.register(dispatcher) }
                }

                // Register objects to event bus by adding to the list
                listOf(this).forEach { EventBus.subscribe(it) }

                // Register modules by adding to the list
                ModuleManager.registerModules(ModuleConfig("LibleAddon.json"),
                    // dungeon
                    AutoPotionBag,
                    BloodRushSplit,
                    CalcLagLoss,
                    ClassDupeNotifier,
                    IllegalMap,
                    LeapAnnounce,
                    MaskTimer,
                    ScoreMilestone,
                    StarMobHighlight,

                    // skyblock
                    AutoRefill
                )

                val name = mc.user?.name?.takeIf { !it.matches(Regex("Player\\d{2,3}")) } ?: return@launch
                scope.launch {
                    getUuid(name)
                        .onSuccess { uuid ->
                            playerUUID = UUID.fromString(uuid.id)
                            playerName = name
                            val data = mapOf(
                                "mcid" to name,
                                "uuid" to uuid.id
                            )
                            val json = gson.toJson(data)
                            postData("https://api.kumajunk.net/hypixel/log", json)
                        }
                        .onFailure {
                            println("Failed to get UUID for $name: ${it.message}")
                        }
                }
                println("LibleAddon initialized!")
            }
        }
    }

    data class IpInfo(
        val country: String
    )

    suspend fun isJapanIp(): Boolean {
        val url = "https://ipapi.co/json/"
        logger.info("Making request to $url")

        val req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("User-Agent", "Mozilla/5.0")
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build()

        return runCatching {
            val httpClient = WebUtils.createClient()
            val res = httpClient
                .sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .await()

            val data = gson.fromJson(res.body(), IpInfo::class.java)
            data.country == "JP"
        }.onFailure {
            logger.warn("Failed to fetch IP country from $url: ${it.message}")
        }.getOrDefault(false)
    }
}

package net.kumajunk.libleaddon

import com.google.gson.JsonObject
import com.odtheking.odin.OdinMod.logger
import com.odtheking.odin.OdinMod.mc
import com.odtheking.odin.OdinMod.scope
import com.odtheking.odin.config.ModuleConfig
import com.odtheking.odin.events.core.EventBus
import com.odtheking.odin.features.ModuleManager
import com.odtheking.odin.utils.network.WebUtils
import com.odtheking.odin.utils.network.WebUtils.gson
import com.odtheking.odin.utils.network.hypixelapi.RequestUtils.getUuid
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.kumajunk.libleaddon.commands.profileViewerCommand
import net.kumajunk.libleaddon.features.impl.dungeon.*
import net.kumajunk.libleaddon.features.impl.floor7.*
import net.kumajunk.libleaddon.features.impl.render.HideArmor
import net.kumajunk.libleaddon.features.impl.render.NoFire
import net.kumajunk.libleaddon.features.impl.render.NoHurtCam
import net.kumajunk.libleaddon.features.impl.render.RemoveGlow
import net.kumajunk.libleaddon.features.impl.skyblock.AutoRefill
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.*

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
                    CryptReminder,
                    DungeonMap,
                    LeapAnnounce,
                    MaskTimer,
                    ScoreMilestone,
                    // ShieldCooldown,
                    StarMobHighlight,
                    StatsViewer,
                    WarpCooldown,
                    WarpSuccessNotifier,

                    // floor7
                    CoreTime,
                    CrushTimer,
                    CrystalNotifier,
                    DiamanteNotifier,
                    DragPrio,
                    HidePlayerOnLeap,
                    I4Helper,
                    I4Timer,
                    PositionNotifier,
                    Predev,
                    PreEnterNotifier,
                    PurplePad,
                    SimonSaysTimer,
                    StormKillTime,
                    TrueSplit,

                    // render
                    HideArmor,
                    NoFire,
                    NoHurtCam,
                    RemoveGlow,

                    // skyblock
                    AutoRefill
                )

                val name = mc.user?.name?.takeIf { !it.matches(Regex("Player\\d{2,3}")) } ?: return@launch
                scope.launch {
                    getUuid(name)
                        .onSuccess { uuid ->
                            playerUUID = uuid.id.toUUID()
                            playerName = name
                        }
                        .onFailure {
                            println("Failed to get UUID for $name: ${it.message}")
                        }
                }
                println("LibleAddon initialized!")
            }
        }
    }

    suspend fun isJapanIp(): Boolean {

        val urls = listOf(
            "http://ip-api.com/json",
            "https://ipapi.co/json/",
            "https://ipinfo.io/json",
            "https://ipwho.is/"
        )

        for (url in urls) {
            val result = checkJapan(url)
            if (result != null) return result
        }

        return false
    }

    private suspend fun checkJapan(url: String): Boolean? {

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

            val body = res.body()

            if (!body.trim().startsWith("{")) {
                throw IllegalStateException("Invalid response: $body")
            }

            when {
                url.contains("ip-api") -> {
                    val json = gson.fromJson(body, JsonObject::class.java)
                    json["countryCode"]?.asString == "JP"
                }

                url.contains("ipinfo") -> {
                    val json = gson.fromJson(body, JsonObject::class.java)
                    json["country"]?.asString == "JP"
                }

                url.contains("ipwho") -> {
                    val json = gson.fromJson(body, JsonObject::class.java)
                    json["country_code"]?.asString == "JP"
                }

                else -> false
            }

        }.onFailure {
            logger.warn("Failed to fetch IP country from $url: ${it.message}")
        }.getOrNull()
    }

    fun String.toUUID(): UUID {
        val dashed = if (this.contains("-")) {
            this
        } else {
            // 8-4-4-4-12 の位置にハイフンを挿入
            this.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})".toRegex(),
                "$1-$2-$3-$4-$5"
            )
        }
        return UUID.fromString(dashed)
    }
}

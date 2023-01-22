package me.dolphin2410.playerfarm

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.tap.fake.FakeEntity
import io.github.monun.tap.protocol.AnimationType
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component.text
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.ExperienceOrb
import org.bukkit.entity.LivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack

class NMSListener: Listener {
    private val gompowder = ItemStack(Material.GUNPOWDER).apply {
        editMeta {
            it.displayName(text("Gompowder", NamedTextColor.AQUA))
        }
    }

    private fun killTarget(target: FakeEntity<out Entity>) = HeartbeatScope().launch {
        if ((target.bukkitEntity as LivingEntity).health == 0.0) return@launch

        target.updateMetadata {
            try {
                (target.bukkitEntity as LivingEntity).health = 0.0
            } catch (_: Exception) {

            }
        }
        target.location.world!!.spawn(target.location, ExperienceOrb::class.java).experience = 9
        target.location.world!!.dropItemNaturally(target.location, gompowder)
        delay(50 * 13)
        target.remove()
    }

    fun handleFakeAttack(target: FakeEntity<out Entity>) {
        if (target.bukkitEntity.type != EntityType.PLAYER) return
        target.playAnimation(AnimationType.TAKE_DAMAGE)
//            target.updateMetadata {
//                HeartbeatScope().launch {
//                    (this@updateMetadata as CraftLivingEntity).health = 0.0
//                }
//            }
        killTarget(target)
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        val fakeServer = GomtoryPlugin.instance.fakeServer
        fakeServer.addPlayer(e.player)
        val player = (e.player as CraftPlayer).handle
        val handler = object: ChannelDuplexHandler() {
            override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
                super.channelRead(ctx, msg)
                if (msg is ServerboundInteractPacket) {
                    if (msg.actionType == ServerboundInteractPacket.ActionType.ATTACK) {
                        val fakeEntity = fakeServer.entities.find { it.bukkitEntity.entityId == msg.entityId }
                        if (fakeEntity != null) {
                            handleFakeAttack(fakeEntity)
                        }
                    }
                }
            }
        }

        val pipeline = player.connection.connection.channel.pipeline()
        pipeline.addBefore("packet_handler", e.player.name, handler)
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        val fakeServer = GomtoryPlugin.instance.fakeServer
        fakeServer.removePlayer(e.player)
        val channel = (e.player as CraftPlayer).handle.connection.connection.channel
        channel.eventLoop().submit {
            channel.pipeline().remove(e.player.name)
        }
    }
}
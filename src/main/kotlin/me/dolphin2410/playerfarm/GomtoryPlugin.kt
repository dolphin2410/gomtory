package me.dolphin2410.playerfarm

import io.github.monun.heartbeat.coroutines.HeartbeatScope
import io.github.monun.kommand.getValue
import io.github.monun.kommand.kommand
import io.github.monun.kommand.wrapper.BlockPosition3D
import io.github.monun.tap.fake.FakeEntityServer
import io.github.monun.tap.mojangapi.MojangAPI
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.dolphin2410.mcphysics.PhysicsVector
import me.dolphin2410.mcphysics.tap.TapPhysicsRuntime
import net.minecraft.server.level.ServerPlayer
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.data.type.Piston
import org.bukkit.block.data.type.PistonHead
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftLivingEntity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.util.Vector


class GomtoryPlugin: JavaPlugin(), Listener {
    companion object {
        lateinit var instance: GomtoryPlugin
    }

    private val coroutines = ArrayList<Job>()
    lateinit var physicsRuntime: TapPhysicsRuntime
    lateinit var fakeServer: FakeEntityServer

    override fun onEnable() {
        instance = this
        fakeServer = FakeEntityServer.create(this)
        physicsRuntime = TapPhysicsRuntime()

        server.scheduler.runTaskTimer(this, fakeServer::update, 0, 1)
        server.scheduler.runTaskTimer(this, physicsRuntime::update, 0, 1)
        server.pluginManager.registerEvents(this, this)

        server.pluginManager.registerEvents(NMSListener(), this)


        val pp = MojangAPI.fetchSkinProfile(MojangAPI.fetchProfile("Gompowder")!!.uuid())!!.profileProperties().toSet()

        kommand {
            register("gomtory") {
                then("stop") {
                    executes {
                        for (coroutine in coroutines) {
                            coroutine.cancel()
                        }

                        for (entity in fakeServer.entities) {
                            entity.remove()
                        }
                    }
                }
                then("start") {
                    then("bp" to blockPosition()) {
                        executes {
                            val bp: BlockPosition3D by it
                            val bottomPiston = bp.toBlock(world)
                            val topPiston = bp.asVector.add(Vector(0, 3, 0)).toLocation(world).block

                            val bottomPistonData = Material.PISTON.createBlockData() as Piston
                            bottomPistonData.facing = BlockFace.UP
                            val bottomPistonHeadData = Material.PISTON_HEAD.createBlockData() as PistonHead
                            bottomPistonHeadData.facing = bottomPistonData.facing

                            val topPistonData = Material.PISTON.createBlockData() as Piston
                            topPistonData.facing = BlockFace.DOWN
                            val topPistonHeadData = Material.PISTON_HEAD.createBlockData() as PistonHead
                            topPistonHeadData.facing = topPistonData.facing

                            coroutines.add(HeartbeatScope().launch {
                                while (true) {
                                    if (fakeServer.entities.size > 30) {
                                        delay(1000)
                                        continue
                                    }

                                    bottomPistonData.isExtended = !bottomPistonData.isExtended
                                    topPistonData.isExtended = !topPistonData.isExtended

                                    bottomPiston.blockData = bottomPistonData
                                    topPiston.blockData = topPistonData

                                    if (!bottomPistonData.isExtended) {
                                        delay(1000)
                                        continue
                                    }

                                    val topPistonHead = topPiston.getRelative(topPistonData.facing)
                                    val bottomPistonHead = bottomPiston.getRelative(bottomPistonData.facing)

                                    topPistonHead.setBlockData(topPistonHeadData, false)
                                    bottomPistonHead.setBlockData(bottomPistonHeadData, false)

                                    val spawnLoc = bottomPiston.location.clone().add(0.5, 1.0, 0.5)
                                    val entity = fakeServer.spawnPlayer(spawnLoc, "Gompowder", pp)
                                    val obj = physicsRuntime.addObject(entity)
                                    obj.registerAction { ah ->
                                        if (obj.position.z > spawnLoc.z + 20) {
                                            obj.applyVelocity(PhysicsVector(0.0, 0.0, -0.8))
                                            ah.cancel()
//                                            HeartbeatScope().launch {
//                                                entity.updateMetadata {
//                                                    health = 0.0
//                                                }
//                                                delay(1000)
//                                                entity.remove()
//                                            }
                                        }
                                    }
                                    obj.applyVelocity(PhysicsVector(0.0, 0.0, 0.8))

                                    delay(200)
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {
        fakeServer.addPlayer(e.player)
    }

    @EventHandler
    fun onQuit(e: PlayerQuitEvent) {
        fakeServer.removePlayer(e.player)
    }

}

package your.game

import com.curiouscreature.kotlin.math.Quaternion
import com.curiouscreature.kotlin.math.Quaternion.Companion.fromEulers
import com.curiouscreature.kotlin.math.clamp
import com.curiouscreature.kotlin.math.pow
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.StateMachineComponent
import com.github.dwursteisen.minigdx.ecs.components.particles.ParticleConfiguration.Companion.spark
import com.github.dwursteisen.minigdx.ecs.components.particles.ParticleEmitterComponent
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.ecs.events.Event
import com.github.dwursteisen.minigdx.ecs.physics.AABBCollisionResolver
import com.github.dwursteisen.minigdx.ecs.states.State
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.StateMachineSystem
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.game.Storyboard
import com.github.dwursteisen.minigdx.game.StoryboardAction
import com.github.dwursteisen.minigdx.game.StoryboardEvent
import com.github.dwursteisen.minigdx.graph.GraphScene
import com.github.dwursteisen.minigdx.input.Key
import com.github.dwursteisen.minigdx.math.Vector2
import com.github.dwursteisen.minigdx.math.Vector3
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Canon : StateMachineComponent()

class Bomb(var direction: Vector3 = Vector3(18f, 0f, 0f), var emitter: Entity? = null) : Component
class Arrow(var t: Seconds = 0f) : Component
class Target : Component
class DestroyBox : Component

class SelectOtherCanon(val target: Entity, val emitter: Entity) : Event

class LevelStoryboadEvent : StoryboardEvent

class BombSystem : System(EntityQuery.of(Bomb::class)) {

    val collider = AABBCollisionResolver()

    val canons by interested(EntityQuery.of(Canon::class))

    val target by interested(EntityQuery.of(Target::class))

    val destroyBox by interested(EntityQuery.of(DestroyBox::class))

    override fun onEntityAdded(entity: Entity) {
        entity.chidren.first()
            .get(ParticleEmitterComponent::class)
            .emit()
    }

    override fun update(delta: Seconds, entity: Entity) {
        val direction = entity.get(Bomb::class).direction
        entity.position.addLocalTranslation(direction, delta = delta)


        for (it in canons) {
            if (collider.collide(entity, it)) {
                if (it.get(Canon::class).hasCurrentState(CanonSystem.Wait::class)) {
                    val emitter = entity.get(Bomb::class).emitter!!
                    entity.destroy()
                    emit(SelectOtherCanon(it, emitter), EntityQuery.of(Canon::class))
                    return
                }
            }
        }

        for (it in target) {
            if (collider.collide(entity, it)) {
                emit(LevelStoryboadEvent())
                return
            }
        }

        for (it in destroyBox) {
            if (collider.collide(entity, it)) {
                emit(LevelStoryboadEvent())
                return
            }
        }
    }
}

class ArrowSystem : System(EntityQuery.of(Arrow::class)) {

    val interpo = Pow(2)

    override fun update(delta: Seconds, entity: Entity) {
        val arrow = entity.get(Arrow::class)
        arrow.t += delta

        entity.position.setLocalTranslation(y = cos(arrow.t * 5f) * 0.5f)
        entity.position.setLocalRotation(y = interpo.apply(clamp(cos(arrow.t * 5f), 0f, 1f)) * 360)
    }
}

class CanonSystem : StateMachineSystem(Canon::class) {

    val arrows by interested(EntityQuery.of(Arrow::class))

    inner class Wait : State() {

        override fun configure(entity: Entity) {
            onEvent(SelectOtherCanon::class) { event ->
                if (event.target == entity) {
                    Selected()
                } else {
                    null
                }
            }
        }
    }

    inner class Selected : State() {

        override fun onEnter(entity: Entity) {
            arrows.forEach {
                it.position.setLocalTranslation(entity.position.localTranslation)
            }
        }

        override fun update(delta: Seconds, entity: Entity): State? {
            fun colineaire(entity: Entity): Vector2 {
                val direction = Vector3(0f, 0f, 1f).rotate(entity.position.localQuaternion)

                val down = Vector3(0f, 0f, 1f).dot(direction)
                val right = Vector3(1f, 0f, 0f).dot(direction)

                return Vector2(down, right)
            }

            if (input.isKeyJustPressed(Key.SPACE)) {
                return Fire()
            } else if (input.isKeyJustPressed(Key.ARROW_LEFT)) {
                val (down, right) = colineaire(entity)

                val angle = if (down > 0.5f) {
                    // turn left
                    -90f
                } else if (down < -0.5f) {
                    // turn right
                    90f
                } else if (right > 0.5) {
                    // full turn
                    180f
                } else {
                    return null
                }

                return Turning(entity.position.localQuaternion, fromEulers(0f, 1f, 0f, -90f), angle)
            } else if (input.isKeyJustPressed(Key.ARROW_RIGHT)) {
                val (down, right) = colineaire(entity)

                val angle = if (down > 0.5f) {
                    // turn left
                    90f
                } else if (down < -0.5f) {
                    // turn right
                    -90f
                } else if (right > 0.5) {
                    // full turn
                    return null
                } else {
                    180f
                }
                return Turning(entity.position.localQuaternion, fromEulers(0f, 1f, 0f, 90f), angle)
            } else if (input.isKeyJustPressed(Key.ARROW_UP)) {
                val (down, right) = colineaire(entity)

                val angle = if (down > 0.5f) {
                    // turn left
                    180f
                } else if (down < -0.5f) {
                    // turn right
                    return null
                } else if (right > 0.5) {
                    // full turn
                    90f
                } else {
                    -90f
                }
                return Turning(entity.position.localQuaternion, fromEulers(0f, 1f, 0f, 180f), angle)
            } else if (input.isKeyJustPressed(Key.ARROW_DOWN)) {
                val (down, right) = colineaire(entity)

                val angle = if (down > 0.5f) {
                    // turn left
                    return null
                } else if (down < -0.5f) {
                    // turn right
                    180f
                } else if (right > 0.5) {
                    // full turn
                    -90f
                } else {
                    90f
                }
                return Turning(entity.position.localQuaternion, fromEulers(0f, 1f, 0f, 0f), angle)
            }
            return null
        }

        override fun configure(entity: Entity) {
            onEvent(SelectOtherCanon::class) { event ->
                if (event.emitter == entity) {
                    Wait()
                } else {
                    null
                }
            }
        }
    }

    inner class Turning(val startQuaternion: Quaternion, val target: Quaternion, var angle: Float = 90f) : State() {

        var current = 0f

        val duration = 0.5f

        var turned = 0f

        override fun update(delta: Seconds, entity: Entity): State? {
            current += delta / duration

            val percent = interpo.apply(current)
            val toTurn = percent * angle

            val remaining = toTurn - turned

            turned = toTurn
            entity.position
                .addLocalRotation(y = remaining)

            if (current >= 1.0) {
                return Selected()
            } else {
                return null
            }
        }

        override fun onExit(entity: Entity) {
            entity.position.setLocalRotation(target)
        }
    }

    inner class Fire : State() {

        private var t = 0.5f

        override fun onEnter(entity: Entity) {
            val bomb = entityFactory.createFromTemplate("bomb")
            bomb.position.setLocalTranslation(entity.position.localTranslation)
            bomb.get(Bomb::class).direction.rotate(entity.position.localQuaternion)
            bomb.get(Bomb::class).emitter = entity
            bomb.position.setLocalRotation(entity.position.localQuaternion)
        }

        override fun update(delta: Seconds, entity: Entity): State? {
            if (t < 0f) {
                return Selected()
            }
            t -= delta
            return null
        }

        override fun configure(entity: Entity) {
            onEvent(SelectOtherCanon::class) { event ->
                if (event.emitter == entity) {
                    Wait()
                } else {
                    null
                }
            }
        }
    }

    override fun initialState(entity: Entity): State {
        if (entity.name == "canon") {
            return Selected()
        } else {
            return Wait()
        }
    }

    companion object {

        // val interpo = Elastic(2f, 10f, 7, 1f)
        val interpo = Pow(2)
    }
}

class TargetSystem: System(EntityQuery.of(Target::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        entity.position.addLocalRotation(y = 180f, delta = delta)
    }
}

@OptIn(ExperimentalStdlibApi::class)
class MyGame(override val gameContext: GameContext) : Game {

    private val scene by gameContext.fileHandler.get<GraphScene>("assets.protobuf")

    override fun createStoryBoard(event: StoryboardEvent): StoryboardAction {
        return if (event is LevelStoryboadEvent) {
            Storyboard.replaceWith { MyGame(gameContext) }
        } else {
            Storyboard.stayHere()
        }
    }

    override fun createEntities(entityFactory: EntityFactory) {
        scene.nodes.forEach { node ->
            if (node.name.startsWith("canon")) {
                val entity = entityFactory.createFromNode(node)
                entity.add(Canon())
            } else if (node.name.startsWith("bomb")) {
                entityFactory.registerTemplate("bomb") {
                    val bombParticles = entityFactory.createParticles(
                        spark(
                            factory = { entityFactory.createFromNode(node) },
                            velocity = 2f,
                            ttl = 0.6f,
                            numberOfParticles = 7,
                            time = -1,
                            duration = 0.1f
                        )
                    )
                    entityFactory.createFromNode(node)
                        .add(Bomb()).apply {
                            bombParticles.attachTo(this)
                        }
                }
            } else if (node.name == "arrow") {
                entityFactory.createFromNode(node)
                    .add(Arrow())
            } else if (node.name == "target") {
                val target = entityFactory.createFromNode(node)
                    .add(Target())

                val bomb = scene.nodes.first { it.name.startsWith("bomb") }
                val targetParticles = entityFactory.createParticles(
                    spark(
                        factory = { entityFactory.createFromNode(bomb) },
                        velocity = 3f,
                        ttl = 1f,
                        numberOfParticles = 6,
                        time = -1,
                        duration = 5f,
                        emitOnStartup = true
                    )
                )
                targetParticles.attachTo(target)
                targetParticles.position.addLocalRotation(y = 90f)
            } else if (node.name.startsWith("Empty")) {
                entityFactory.createFromNode(node)
                    .add(DestroyBox())
            } else {
                entityFactory.createFromNode(node)
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(
            BombSystem(),
            CanonSystem(),
            ArrowSystem(),
            TargetSystem()
        )
    }
}

class Elastic(val value: Float, val power: Float, bounces: Int, val scale: Float) {

    val bounces: Float = bounces * PI.toFloat() * if (bounces % 2 == 0) 1f else -1f

    fun apply(alpha: Float): Float {
        var a = alpha
        if (a <= 0.5f) {
            a *= 2f
            return pow(
                value,
                (power * (a - 1))
            ) * sin(a * bounces) * scale / 2
        }
        a = 1 - a
        a *= 2f
        return 1 - pow(
            value,
            (power * (a - 1))
        ) * sin(a * bounces) * scale / 2
    }

    /** @param blend Alpha value between 0 and 1.
     */
    fun apply(start: Float, end: Float, blend: Float): Float {
        return start + (end - start) * apply(blend)
    }
}

class Pow(val power: Int) {

    /** @param blend Alpha value between 0 and 1.
     */
    fun apply(start: Float, end: Float, blend: Float): Float {
        return start + (end - start) * apply(blend)
    }

    fun apply(a: Float): Float {
        return if (a <= 0.5f) pow(
            (a * 2f),
            power.toFloat()
        ) / 2 else pow(
            ((a - 1) * 2f),
            power.toFloat()
        ) / (if (power % 2 == 0) -2 else 2) + 1
    }
}


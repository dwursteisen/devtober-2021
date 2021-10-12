package your.game

import com.curiouscreature.kotlin.math.Quaternion
import com.curiouscreature.kotlin.math.clamp
import com.curiouscreature.kotlin.math.pow
import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.Component
import com.github.dwursteisen.minigdx.ecs.components.StateMachineComponent
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.entities.position
import com.github.dwursteisen.minigdx.ecs.states.State
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.StateMachineSystem
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.graph.GraphScene
import com.github.dwursteisen.minigdx.input.Key
import com.github.dwursteisen.minigdx.math.Vector3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class Canon : StateMachineComponent()

class Bomb(var direction: Vector3 = Vector3(9f, 0f, 0f)) : Component
class Arrow(var t: Seconds = 0f) : Component

class BombSystem : System(EntityQuery.of(Bomb::class)) {

    override fun update(delta: Seconds, entity: Entity) {
        val direction = entity.get(Bomb::class).direction
        entity.position.addLocalTranslation(direction, delta = delta)
    }
}

class ArrowSystem : System(EntityQuery.of(Arrow::class)) {

    val interpo = Pow(2)

    override fun update(delta: Seconds, entity: Entity) {
        val arrow = entity.get(Arrow::class)
        arrow.t += delta

        // FIXME: le 3f ne devrait pas être nécessaire. Vu que l'origin du model :thinking:
        entity.position.setLocalTranslation(y =cos(arrow.t * 5f) * 0.5f)
        entity.position.setLocalRotation(y = interpo.apply(clamp(cos(arrow.t * 5f), 0f, 1f)) * 360)
    }
}

class CanonSystem : StateMachineSystem(Canon::class) {

    val arrows by interested(EntityQuery.of(Arrow::class))

    inner class Wait : State() {

    }

    inner class Selected : State() {

        override fun onEnter(entity: Entity) {
            arrows.forEach {
                it.position.setLocalTranslation(entity.position.localTranslation)
            }
        }

        override fun update(delta: Seconds, entity: Entity): State? {
            if (input.isKeyJustPressed(Key.SPACE)) {
                return Fire()
            } else if (input.isKeyJustPressed(Key.ARROW_LEFT)) {
                return Turning(entity.position.localQuaternion, -90f)
            } else if (input.isKeyJustPressed(Key.ARROW_RIGHT)) {
                return Turning(entity.position.localQuaternion, 90f)
            }
            return null
        }
    }

    inner class Turning(val startQuaternion: Quaternion, val angle: Float) : State() {

        var current = 0f

        val duration = 0.5f

        override fun update(delta: Seconds, entity: Entity): State? {
            current += delta / duration

            val a = interpo.apply(0f, angle, current)
            entity.position.setLocalRotation(startQuaternion).addLocalRotation(y = a)
            if (current >= 1.0) {
                return Selected()
            } else {
                return null
            }
        }

        override fun onExit(entity: Entity) {
            entity.position.setLocalRotation(startQuaternion).addLocalRotation(y = angle)
        }
    }

    inner class Fire : State() {

        private var t = 0.5f

        override fun onEnter(entity: Entity) {
            val bomb = entityFactory.createFromTemplate("bomb")
            bomb.position.setLocalTranslation(entity.position.localTranslation)
            bomb.get(Bomb::class).direction.rotate(entity.position.localQuaternion)
        }

        override fun update(delta: Seconds, entity: Entity): State? {
            if (t < 0f) {
                return Selected()
            }
            t -= delta
            return null
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

@OptIn(ExperimentalStdlibApi::class)
class MyGame(override val gameContext: GameContext) : Game {

    private val scene by gameContext.fileHandler.get<GraphScene>("assets.protobuf")

    override fun createEntities(entityFactory: EntityFactory) {
        scene.nodes.forEach { node ->
            if (node.name.startsWith("canon")) {
                val entity = entityFactory.createFromNode(node)
                entity.add(Canon())
            } else if (node.name.startsWith("bomb")) {
                entityFactory.registerTemplate("bomb") {
                    entityFactory.createFromNode(node)
                        .add(Bomb())
                }
            } else if (node.name == "arrow") {
                entityFactory.createFromNode(node)
                    .add(Arrow())
            } else {
                entityFactory.createFromNode(node)
            }
        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(
            BombSystem(),
            CanonSystem(),
            ArrowSystem()
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


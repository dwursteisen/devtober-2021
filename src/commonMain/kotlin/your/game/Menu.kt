package your.game

import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.components.HorizontalAlignment
import com.github.dwursteisen.minigdx.ecs.components.TextComponent
import com.github.dwursteisen.minigdx.ecs.components.text.WaveEffect
import com.github.dwursteisen.minigdx.ecs.components.text.WriteText
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.file.Font
import com.github.dwursteisen.minigdx.file.get
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.game.Storyboard
import com.github.dwursteisen.minigdx.game.StoryboardAction
import com.github.dwursteisen.minigdx.game.StoryboardEvent
import com.github.dwursteisen.minigdx.graph.GraphScene
import com.github.dwursteisen.minigdx.input.Key

class StartLevel : StoryboardEvent

class MenuSystem : System(EntityQuery.none()) {

    override fun update(delta: Seconds, entity: Entity) = Unit

    override fun update(delta: Seconds) {
        if (input.isKeyJustPressed(Key.SPACE)) {
            emit(StartLevel())
        }
    }
}

@ExperimentalStdlibApi
class Menu(override val gameContext: GameContext) : Game {

    private val scene by gameContext.fileHandler.get<GraphScene>("title.protobuf")
    private val font by gameContext.fileHandler.get<Font>("title")

    override fun createStoryBoard(event: StoryboardEvent): StoryboardAction {
        if (event is StartLevel) {
            return Storyboard.replaceWith { MyGame(gameContext) }
        } else {
            return Storyboard.stayHere()
        }
    }

    override fun createEntities(entityFactory: EntityFactory) {
        scene.nodes.forEach { node ->
            if (node.name == "title") {
                val content = "CANON BALL\npress SPACE to start"
                val e = entityFactory.createText(WaveEffect(WriteText(content)), font, node)
                e.get(TextComponent::class).horizontalAlign = HorizontalAlignment.Center
            } else {
                entityFactory.createFromNode(node)
            }

        }
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(MenuSystem())
    }
}

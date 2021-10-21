package your.game

import com.github.dwursteisen.minigdx.GameContext
import com.github.dwursteisen.minigdx.Seconds
import com.github.dwursteisen.minigdx.ecs.Engine
import com.github.dwursteisen.minigdx.ecs.entities.Entity
import com.github.dwursteisen.minigdx.ecs.entities.EntityFactory
import com.github.dwursteisen.minigdx.ecs.systems.EntityQuery
import com.github.dwursteisen.minigdx.ecs.systems.System
import com.github.dwursteisen.minigdx.game.Game
import com.github.dwursteisen.minigdx.game.Storyboard
import com.github.dwursteisen.minigdx.game.StoryboardAction
import com.github.dwursteisen.minigdx.game.StoryboardEvent
import com.github.dwursteisen.minigdx.input.Key

class StartLevel : StoryboardEvent

class MenuSystem : System(EntityQuery.none()) {

    override fun update(delta: Seconds, entity: Entity) = Unit

    override fun update(delta: Seconds) {
        if(input.isKeyJustPressed(Key.SPACE)) {
            emit(StartLevel())
        }
    }
}
class Menu(override val gameContext: GameContext) : Game {

    override fun createStoryBoard(event: StoryboardEvent): StoryboardAction {
        if(event is StartLevel) {
            return Storyboard.replaceWith { MyGame(gameContext) }
        } else {
            return Storyboard.stayHere()
        }
    }

    override fun createEntities(entityFactory: EntityFactory) {
    }

    override fun createSystems(engine: Engine): List<System> {
        return listOf(MenuSystem())
    }
}

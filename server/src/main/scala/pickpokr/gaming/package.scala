package pickpokr

import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.ws.Message

package object gaming {
  type Client = ActorRef[Client.Message]
  type WSClient = ActorRef[Message]
  type Player = ActorRef[Player.Command]
  type Game = ActorRef[Game.Command]
  type Train = ActorRef[Train.Command]
  type Lobby = ActorRef[Lobby.Command]
  val minGameSize = 2
}
package pickpokr

import akka.actor.typed.ActorRef

package object gaming {
  type Client = ActorRef[Client.Message]
  type Player = ActorRef[Player.Command]
  type Game = ActorRef[Game.Command]
  type Train = ActorRef[Train.Command]
  type Lobby = ActorRef[Lobby.Command]
}
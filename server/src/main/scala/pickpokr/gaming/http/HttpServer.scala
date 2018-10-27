package pickpokr.gaming.http

import akka.NotUsed
import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import pickpokr.gaming.{Lobby, Nick, Pin}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}
import ujson.Js

object HttpServer extends App with JsonSupport with Directives {
  implicit val system: ActorSystem = ActorSystem("pickpokr")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher
  val lobby: ActorRef[Lobby.Command] = system.spawn(Lobby.behavior(), "lobby")
  val bufferSize = 100

  lazy val routes: Route = {
    pathPrefix("ws") {
      pathPrefix(IntNumber) { trainId =>
        path(Segment) { nick =>
          handleWebSocketMessages(webSocketFlow(trainId, Nick(nick)))
        }
      }
    } ~
      getFromDirectory("../www")
  }

  def lobbySink(trainId: Int, nick: Nick) =
    ActorSink.actorRef[Lobby.Command](lobby, Lobby.Cancel(trainId, nick), _ => Lobby.Cancel(trainId, nick))

  def webSocketFlow(trainId: Int, nick: Nick): Flow[Message, Message, NotUsed] = {
    Flow[Message].
      collect {
        case TextMessage.Strict(msg) => msg
      }.
      via {
        val in = Flow[String].
          map { data =>
            val js = ujson.read(data)
            val kind = js.obj("type").str
            (kind, js.obj.get("payload"))
          }.
          collect {
            case ("guess", Some(Js.Str(guess))) =>
              Lobby.Guess(trainId, nick, guess)
            case ("requestExchange", _) ⇒
              Lobby.RequestExchange(trainId, nick)
            case ("exchangeCommit", Some(Js.Num(n))) ⇒
              Lobby ExchangeCommit (trainId, nick, Pin(n.toInt))
            case ("startGame", _) ⇒
              Lobby.StartGame(trainId)
          }.
          to(lobbySink(trainId, nick))

        val out = ActorSource.actorRef[Message](
          { case msg if false => println(msg.toString) }, // Todo complete
          { case msg if false => new RuntimeException(s"bad:$msg") },
          bufferSize,
          OverflowStrategy.fail).
          mapMaterializedValue { clientRef =>
            lobby ! Lobby.ClientConnected(trainId, nick, clientRef)
            NotUsed
          }

        Flow.fromSinkAndSource(in, out)
      }
  }

  val serverBinding: Future[Http.ServerBinding] =
    Http().bindAndHandle(routes, "0.0.0.0", 8080)

  serverBinding.onComplete {
    case Success(bound) =>
      println(s"pickpokr http server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
    case Failure(e) =>
      Console.err.println(s"Server could not start!")
      e.printStackTrace()
      system.terminate()
  }

  Await.result(system.whenTerminated, Duration.Inf)
}
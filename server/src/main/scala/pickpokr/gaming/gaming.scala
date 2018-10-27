package pickpokr
package gaming

import java.security.SecureRandom

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors._
import akka.http.scaladsl.model.ws.TextMessage
import pickpokr.gaming.http.JsonSupport
import spray.json.JsValue

import scala.annotation.tailrec

case class Nick(literal: String)
case class Pin(value: Int)

object Player {
  sealed trait Event
  case class RoasterUpdated(roaster: Train.Roaster) extends Event
  sealed trait Command
  case class UpdateRoaster(roaster: Train.Roaster) extends Command
  case class JoinGame(index: Int, question: List[Challenge], game: Game) extends Command
  case class Guess(literal: String) extends Command
  case class Winner(nick: Nick, keyword: String) extends Command
  case class Pin(value: Int) extends Command
  case class ExchangeCommit(player: Player) extends Command
  case class ExchangeChallenge(index: Int, challenge: Challenge) extends Command
  case class Challenge(query: Option[String], length: Int)

  def waiting(client: WSClient, nick: Nick): Behavior[Command] = {
    receiveMessagePartial {
      case JoinGame(index, challenges, game) =>
        client ! Client.Challenge(challenges.map(c ⇒ (c.query, c.length))).toTextMessage
        playing(client, nick, game, index, challenges)
    }
  }
  def playing(client: WSClient, nick: Nick, game: Game, index: Int, challenges: List[Challenge]): Behavior[Command] = {
    receiveMessage {
      case UpdateRoaster(roaster) =>
        client ! Client.Roaster(roaster.nicks).toTextMessage
        same
      case Pin(pin) ⇒
        client ! Client.Pin(pin).toTextMessage
        same
      case ExchangeCommit(player) ⇒
        player ! ExchangeChallenge(index, challenges(index))
        same
      case ExchangeChallenge(i, challenge) ⇒
        val uc = challenges.updated(i, challenge)
        client ! clientChallengeMessage(challenges)
        playing(client, nick, game, index, uc)
      case Guess(guess) =>
        game ! Game.Guess(nick, guess)
        same
      case Winner(winner, keyword) ⇒
        client ! Client.Winner(winner, keyword).toTextMessage
        same // Todo end game
    }
  }
  private def clientChallengeMessage(challenges: List[Challenge]) = {
    Client.Challenge(challenges.map(c ⇒ (c.query, c.length))).toTextMessage
  }
}

object Game {
  private val random = new SecureRandom()

  case class Question(query: String, answer: String) {
    val length: Int = query.length
  }

  private val questions = List[List[Question]](
    List(
      Question("Är en resenär", "Turist"),
      Question("Blixt och dunder", "Åska"),
      Question("Finns i Falun", "Gruva")),
    List(
      Question("Kan bära skägg", "Haka"),
      Question("Blixt och dunder", "A"),
      Question("Finns i Falun", "V")),

  )

  private def keyword(index:Int) = questions(index).map(_.answer.head).mkString

  sealed trait Event
  sealed trait Command
  case class Guess(nick: Nick, guess: String) extends Command
  case class Winner(nick: Nick) extends Command

  def behavior(players: List[Player]): Behavior[Command] = playing(players)

  def playing(players: List[Player]): Behavior[Command] = {
    setup { ctx =>
      val game = random.nextInt(2)
      players.zipWithIndex.foreach {
        case (player, plyerIndex) =>
          val challenges = questions(game).zipWithIndex.map {
            case (q, questionIndex) ⇒
              val question = if (plyerIndex == questionIndex) Some(q.query) else None
              Player.Challenge(question, q.length)
          }
          player ! Player.JoinGame(plyerIndex, challenges, ctx.self)
      }
      receiveMessage {
        case Guess(nick, guess) if guess == keyword(game) =>
          players foreach (_ ! Player.Winner(nick, guess))
          finished(players, nick, guess)
      }
    }
  }
  def finished(players: List[Player], winner: Nick, keyword: String): Behavior[Command] = {
    receiveMessage {
      case Guess(_, _) =>
        players foreach (_ ! Player.Winner(winner, keyword))
        same
    }
  }
}

case class ClientMessage(`type`: String, payload: JsValue)

object Client {

  sealed trait Message {
    private def format(tpe: String): String = s"""{"type":"$tpe"}"""
    private def format(tpe: String, payload: String): String = s"""{"type":"$tpe", "payload":"$payload"}"""
    private def format(tpe: String, payload: Int): String = s"""{"type":"$tpe", "payload":$payload}"""
    def toTextMessage: TextMessage = {
      val json = this match {
        case Denied(reason) ⇒ format("denied", reason)
        case Roaster(nicks) ⇒ s"""{"type": "roaster", "payload":${nicks.map("\"" + _.literal + "\"").mkString("[", ", ", "]")}}"""
        case Pin(value) ⇒ format("pin", value)
        case Challenge(qas) ⇒
          val qal = qas.map {
            case (Some(q), l) ⇒ s"""{"question": "$q", "answerLength": $l}"""
            case (None, l) ⇒ s"""{"answerLength": $l}"""
          }.mkString("[", ", ", "]")
          s"""{"type":"challenge", "payload":$qal}"""
        case Winner(nick, answer) ⇒
          s"""{"type":"outcome", "payload":{"nick": $nick, "answer": "$answer"}"""
      }
      println(s"to client -> $json")
      TextMessage(json)
    }
  }
  case class Denied(reason: String) extends Message
  case class Roaster(nicks: List[Nick]) extends Message
  case class Pin(value: Int) extends Message
  case class Challenge(qas: List[(Option[String], Int)]) extends Message
  case class Winner(nick: Nick, keyword: String) extends Message
}

object Train extends JsonSupport {
  type Id = Long

  case class Roaster(nicks: List[Nick] = Nil)

  sealed trait Event
  final case object Enrolled extends Event
  final case class Denied(reason: String) extends Event

  sealed trait Command
  case class ClientConnected(nick: Nick, client: WSClient) extends Command
  private case object CheckRoaster extends Command
  case class Guess(nick: Nick, value: String) extends Command
  case class RequestExchange(nick: Nick) extends Command
  case class ExchangeCommit(nick: Nick, pin: Pin) extends Command

  private val random = new SecureRandom()

  def behavior(players: Map[Nick, Player] = Map.empty, roaster: Roaster = Roaster(), games: List[Game] = Nil, exchanges: Map[Int, Nick] = Map.empty): Behavior[Command] = {
    setup { ctx ⇒
      receiveMessagePartial {
        case ClientConnected(nick, client) =>
          if (players.keySet.contains(nick)) {
            client ! Client.Denied("Nick already taken").toTextMessage
            same
          } else {
            val player = ctx.spawn(Player.waiting(client, nick), s"player-${nick.literal}")
            val up = players.updated(nick, player)
            val updatedRoaster = roaster.copy(nicks = nick :: roaster.nicks)
            client ! Client.Roaster(updatedRoaster.nicks).toTextMessage
            up.values.foreach(_ ! Player.UpdateRoaster(updatedRoaster))
            ctx.self ! CheckRoaster
            behavior(up, updatedRoaster, games)
          }
        case CheckRoaster if roaster.nicks.size > 2 =>
          val gamePlayers = roaster.nicks.flatMap(players.get)
          val game = ctx.spawn(Game.behavior(gamePlayers), s"game-${games.size + 1}")
          behavior(players, Roaster(), game :: games)
        case Guess(nick, guess) ⇒
          players(nick) ! Player.Guess(guess)
          same
        case RequestExchange(nick) ⇒
          @tailrec
          def generatePin(): Int = {
            val pin = random.nextInt(10000)
            if (!exchanges.keySet(pin)) pin
            else generatePin()
          }
          val pin = generatePin()
          players(nick) ! Player.Pin(pin)
          behavior(players, roaster, games, exchanges + (pin → nick))
        case ExchangeCommit(nick, pin) ⇒
          exchanges.get(pin.value).foreach { otherNick ⇒
            val player = players(nick)
            val otherPlayer = players(otherNick)
            player ! Player.ExchangeCommit(otherPlayer)
            otherPlayer ! Player.ExchangeCommit(player)
          }
          same
      }
    }
  }
}

object Lobby {
  type Roster = List[String]

  sealed trait Command
  case class Cancel(train: Train.Id, nick: Nick) extends Command
  case class ClientConnected(train: Train.Id, nick: Nick, client: WSClient) extends Command
  case class Guess(train: Train.Id, nick: Nick, guess: String) extends Command
  case class RequestExchange(trainId: Int, nick: Nick) extends Command
  case class ExchangeCommit(trainId: Int, nick: Nick, pin: Pin) extends Command

  def behavior(trains: Map[Train.Id, Train] = Map.empty, clients: List[WSClient] = Nil): Behavior[Command] = {
    setup { ctx ⇒
      receiveMessagePartial {
        case ClientConnected(trainId, nick, client) =>
          val train = trains.getOrElse(trainId, ctx.spawn(Train.behavior(), s"train-$trainId"))
          train ! Train.ClientConnected(nick, client)
          behavior(trains.updated(trainId, train), client :: clients)
        case Guess(trainId, nick, guess) ⇒
          trains(trainId) ! Train.Guess(nick, guess)
          same
        case RequestExchange(trainId, nick) ⇒
          trains(trainId) ! Train.RequestExchange(nick)
          same
        case ExchangeCommit(trainId, nick, pin) ⇒
          trains(trainId) ! Train.ExchangeCommit(nick, pin)
          same
      }
    }
  }
}

object Words {
  val all: String =
    """Abroad, Access, Accommodations, Activities, Addition, Adventure, Affordable, Agency, Airfare, Allure, Ambiance, Amenities, Amount, Ample, Amusement, Appetite, Aquatic, Arrangements, Array, Assortment, Atmosphere, Attraction, Availability
      |Backyard, Barbecue, Beach, Bellhop, Beverage, Biking, Boathouse, Boating, Boutique, Break, Budget, Business
      |Camper, Campground, Camping, Cancellation, Canoeing, Capacity, Captain, Caravan, Cash, Certification, Challenge, Charter, Chef, Choice, Clientele, Climate, Coach, Comfort, Comfortable, Contract, Convenience, Costly, Crafts, Credit, Cruise
      |Decadent, Delight, Deluxe, Deposit, Destination, Discounts, Dismay, Dispatch, Distinguish, Diversion, Diversity, Downtime
      |Earnest, Easy, Energetic, Enjoyable, Enjoyment, Entertainment, Environment, Envision, Equipment, Escape, Event, Exclusive, Excursion, Expedition, Expensive, Experience, Exploration, Extras, Extravagant, Exude
      |Facilities, Fancy, Fanfare, Fare, Fees, Fitness, Food, Foreign, Free, Freedom, Friendliness, Function, Furlough, Futon
      |Gastronomy, Gathering, Gear, Getaway, Gifts, Global, Globetrotter, Golf, Guest, Guide, Gusty
      |Harbor, Hiatus, Hike, Holiday, Honorarium, Hooky, Hospitality, Host, Hostel, Hostess, Hotel
      |Ideal, Idyll, Impressive, Inn, Instruction, Insurance, Intensive, Interim, International, Island, Itinerary
      |Jaunt, Journey, Journey, Joy, Joyride, Junket
      |Kayaking, Keen, Kid-friendly, Kindly, Kindness, Kinship, Knitting
      |Lake-view, Landmass, Language, Launch, Lazy, Leave, Leisure, Lessons, Liberty, Lifestyle, Limit, Locale, Location, Lodging, Lounge, Luggage, Lull, Luxurious
      |Mandatory, Marina, Massive, Maximum, Meals, Meetings, Memento, Memorable, Minimum, Moderation, Monitor, Mood, Motion, Movement, Music
      |Nice, Nominal, Noteworthy, Noticeable
      |Occasion, Odyssey, Option, Organization, Original, Outdoors, Outing, Outstanding, Overbooking
      |Paddle, Parade, Park, Participation, Partying, Pause, Payment, Payoff, Peaceful, Pension, Perambulate, Perks, Picnic, Picturesque, Pizazz, Playground, Playtime, Pleasure, Porter, Promenade, Property, Protection, Public, Purser
      |Quaint, Quality, Quantity, Query, Quest, Quiet, Quirky
      |Racing, Rate, Reasonable, Recess, Recreation, Recuperation, Refreshment, Refund, Regard, Regatta, Relaxation, Renown, Rental, Reputation, Requisite, Reservation, Reserve, Resort, Restaurant, Retreat, Riparian, Romantic, Round-the-world, Round-trip, Route, Routine, Rowing
      |Sabbatical, Safari, Safety, Sailing, Sanctuary, Sand, Satisfying, Scenic, Secluded, Selection, Setting, Ship, Side-trip, Soothing, Souvenir, Spa, Space, Spacious, Steerage, Steward, Stewardess, Sublime, Successful, Suitcase, Sumptuous, Sunscreen, Sunshine, Swimming pool
      |Tan, Tennis, Tent, Tour, Tourism, Tourist, Tournament, Trail, Trailer, Train, Transfer, Transportation, Travel, Trek, Trip, Tropical, Truancy, Trunk
      |Ubiquitous, Unique, Universal, Updated, Upgrade
      |Vacation, Valuable, Variety, View, Visit, Vista, Volleyball, Voyage
      |Walk, Wander, Waterfront, Wayfarer, Weary, Weather, Weekend, Whim, Whirlpool, Wide-ranging, Windsurf, Woo, Workshop, World, World-class, Worldwide
      |Xanadu
      |Yacht, Yoga, Youth
      |Zeal, Zoological
    """.stripMargin
}
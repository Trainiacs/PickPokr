package pickpokr
package game

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors._

case class Nick(literal: String) extends AnyVal

object Player {
  sealed trait Event
  case class RoasterUpdated(roaster: Train.Roaster) extends Event
  sealed trait Command
  case class UpdateRoaster(roaster: Train.Roaster) extends Command
  case class Challenge(index: Int, question: String, answerLength: Int) extends Command
  case class Answer(answer: String) extends Command

  def behavior(client: Client): Behavior[Command] = {
    receiveMessage {
      case UpdateRoaster(roaster) =>
        client ! Client.UpdateRoaster(roaster.nicks)
        same
      case Challenge(index, question, answerLength) =>
        client ! Client.Challenge(index, question, answerLength)
        same
      case Answer(answer) =>
        client ! Client.A
        same
    }
  }
}

object Game {
  val questions = List(
    "Är en resenär" -> "Turist",
    "Blixt och dunder!" -> "Åska",
    "Finns i Falun" -> "Gruva")

  sealed trait Event
  sealed trait Command

  def behavior(players: List[Player]): Behavior[Command] = {
    setup { ctx =>
      players.zip(questions).zipWithIndex.foreach {
        case ((player, (question, answer)), i) =>
          player ! Player.Challenge(i, question, answer.length)
      }
      receiveMessage {
        case Answer => println(x)
      }
    }
  }
}

object Client {
  sealed trait Message
  case object Enrolled extends Message
  case class Denied(reason: String) extends Message
  case class UpdateRoaster(nicks: List[Nick]) extends Message
  case class Pin(value: Int) extends Message
  case class Challenge(index: Int, question: String, answerLength: Int) extends Message
  case class Winner(nick: Nick) extends Message
  case object Completed extends Message
  case object Failed extends Message
}

object Train {
  type Id = Long

  case class Roaster(nicks: List[Nick] = Nil)

  sealed trait Event
  final case object Enrolled extends Event
  final case class Denied(reason: String) extends Event

  sealed trait Command
  case class ClientConnected(nick: Nick, client: Client) extends Command
  private case object CheckRoaster extends Command

  def behavior(players: Map[Nick, Player] = Map.empty, roaster: Roaster = Roaster(), games: List[Game] = Nil): Behavior[Command] = {
    setup { ctx ⇒
      receiveMessagePartial {
        case ClientConnected(nick, client) =>
          if (players.keySet.contains(nick)) {
            client ! Client.Denied("Nick already taken")
            same
          } else {
            val player = ctx.spawn(Player.behavior(client), s"player-${nick.literal}")
            client ! Client.Enrolled
            val up = players.updated(nick, player)
            up.values.foreach( _ ! Player.UpdateRoaster(Roaster(up.keySet)) )
            ctx.self ! CheckRoaster
            // send out roaster
            behavior(up, roaster, games)
          }
        case CheckRoaster if roaster.nicks.size > 2 =>
          val gamePlayers = players.collect {
            case (nick, player) if roaster.nicks(nick) => player
          }.toList
          val game = ctx.spawn(Game.behavior(gamePlayers), s"game-${games.size + 1}")
          behavior(players, Roaster(), game :: games)
      }
    }
  }
}

object Lobby {
  type Roster = List[String]

  sealed trait Command
  case class Enroll(train: Train.Id, nick: Nick) extends Command
  case class Cancel(train: Train.Id, nick: Nick) extends Command
  case class ClientConnected(train: Train.Id, nick: Nick, client: Client) extends Command

  def behavior(trains: Map[Train.Id, Train] = Map.empty, clients: List[Client] = Nil): Behavior[Command] = {
    setup { ctx ⇒
      receiveMessagePartial {
        case Enroll(trainId, nick) =>
          val train = trains.getOrElse(trainId, ctx.spawn(Train.behavior(), s"train-$trainId"))
          train ! Train.Enroll(nick)
          behavior(trains.updated(trainId, train))
        case ClientConnected(trainId, nick, client) =>
          val train = trains.getOrElse(trainId, ctx.spawn(Train.behavior(), s"train-$trainId"))
          train ! Train.ClientConnected(nick, client)
          behavior(trains.updated(trainId, train), client :: clients)
      }
    }
  }
}


object Words {
  val all =
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
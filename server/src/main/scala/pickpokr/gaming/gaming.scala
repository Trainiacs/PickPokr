package pickpokr
package gaming

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
  case class BadGuess(nick:Nick, guess: String) extends Command
  case class Winner(nick: Nick, keyword: String) extends Command
  case class Pin(value: Int) extends Command
  case class ExchangeCommit(player: Player) extends Command
  case class ExchangeChallenge(index: Int, challenge: Challenge) extends Command
  case class Challenge(query: Option[String], length: Int)

  def waiting(client: WSClient, nick: Nick): Behavior[Command] = {
    receiveMessagePartial {
      case UpdateRoaster(roaster) =>
        client ! Client.Roaster(roaster.nicks).toTextMessage
        same
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
      case BadGuess(nick, guess) ⇒
        client ! Client.BadGuess(nick, guess).toTextMessage
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
  sealed trait Event
  sealed trait Command
  case class Guess(nick: Nick, guess: String) extends Command
  case class Winner(nick: Nick) extends Command

  def behavior(players: List[Player]): Behavior[Command] = playing(players)

  def playing(players: List[Player]): Behavior[Command] = {
    setup { ctx =>
      val questionSet = QuestionSet(players.size)
      players.zipWithIndex.foreach {
        case (player, plyerIndex) =>
          val challenges = questionSet.questions.zipWithIndex.map {
            case (q, questionIndex) ⇒
              val question = if (plyerIndex == questionIndex) Some(q.query) else None
              Player.Challenge(question, q.length)
          }
          player ! Player.JoinGame(plyerIndex, challenges, ctx.self)
      }
      receiveMessage {
        case Guess(nick, guess) if guess == questionSet.keyword =>
          players foreach (_ ! Player.Winner(nick, guess))
          finished(players, nick, guess)
        case Guess(nick, guess) ⇒ 
          players foreach (_ ! Player.BadGuess(nick, guess))
          same
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
        case Winner(nick, keyword) ⇒
          s"""{"type":"winner", "payload":{"nick": $nick, "keyword": "$keyword"}"""
        case BadGuess(nick, guess) ⇒
          s"""{"type":"badGuess", "payload":{"nick": $nick, "guess": "$guess"}"""
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
  case class BadGuess(nick: Nick, guess: String) extends Message
}

object Train extends JsonSupport {
  type Id = Long

  case class Roaster(nicks: List[Nick] = Nil)

  sealed trait Event
  final case object Enrolled extends Event
  final case class Denied(reason: String) extends Event

  sealed trait Command
  case class ClientConnected(nick: Nick, client: WSClient) extends Command
  case object StartGame extends Command
  private case object CheckRoaster extends Command
  case class Guess(nick: Nick, value: String) extends Command
  case class RequestExchange(nick: Nick) extends Command
  case class ExchangeCommit(nick: Nick, pin: Pin) extends Command

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
//            ctx.self ! CheckRoaster
            behavior(up, updatedRoaster, games)
          }
        case StartGame if roaster.nicks.size > minGameSize =>
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
  case class StartGame(trainId:Int) extends Command

  def behavior(trains: Map[Train.Id, Train] = Map.empty, clients: List[WSClient] = Nil): Behavior[Command] = {
    setup { ctx ⇒
      receiveMessagePartial {
        case ClientConnected(trainId, nick, client) =>
          val train = trains.getOrElse(trainId, ctx.spawn(Train.behavior(), s"train-$trainId"))
          train ! Train.ClientConnected(nick, client)
          behavior(trains.updated(trainId, train), client :: clients)
        case StartGame(trainId) ⇒
          trains(trainId) ! Train.StartGame
          same
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

case class Question(query: String, answer: String) {
  val length: Int = answer.length
}
case class QuestionSet(keyword: String, questions: List[Question])
object QuestionSet {
  //private def keyword(index:Int) = questions(index).map(_.answer.head).mkString

  def apply(n: Int): QuestionSet = {
    val words = wordsByLength(n).filterNot(w => w.exists(c ⇒ "cijpuwxyz".contains(c)))
    val keyword = words(random.nextInt(words.size))
    println(s"words = ${words.mkString("[", ", ", "]")}, keyword = $keyword")
    val qs = keyword.toUpperCase.map(questionsByInitialCharacter).map(_.head).map {
      case (answer, query) => Question(query, answer)
    }.toList
    println(s"keyword = $keyword, questions = ${qs.mkString("\n\t", "\n\t", "")}")
    QuestionSet(keyword, qs)
  }
  lazy val wordsByLength = wordsData.
    split('\n').
    map(_.trim).
    filterNot(w => w.exists(_.isDigit) || w.exists(_.isSpaceChar)).
    toList.
    distinct.
    groupBy(_.length)
  private val questions1 = List[List[Question]](
    List(
      Question("Är en resenär", "Turist"),
      Question("Blixt och dunder", "Åska"),
      Question("Finns i Falun", "Gruva")),
    List(
      Question("Kan bära skägg", "Haka"),
      Question("Blixt och dunder", "A"),
      Question("Finns i Falun", "V")),

  )
  lazy val questionsByInitialCharacter = questionsData.
    split('\n').
    map(_.split('=')).
    filter(_.size >= 2).
    map(a => a(0).trim -> a(1).trim).
    toList.
    groupBy(_._1.head)
  val wordsData =
    """
      |25
      |25
      |25
      |30
      |30
      |anhängare
      |arkiv
      |beslut
      |beslut
      |beslut
      |beslut
      |beslut
      |beslut
      |beslut
      |beslut
      |besökare
      |besökare
      |besökare
      |besökare
      |besökare
      |besökare
      |besökare
      |boende
      |brand
      |brand
      |cancer
      |cancer
      |dans
      |dator
      |dator
      |dator
      |december
      |december
      |december
      |december
      |december
      |december
      |djup
      |djup
      |djup
      |djup
      |djup
      |djup
      |djup
      |dollar
      |dollar
      |dollar
      |dollar
      |dollar
      |dom
      |dom
      |dom
      |dom
      |drev
      |drev
      |drottning
      |drottning
      |dubbel
      |dubbel
      |dubbel
      |dubbel
      |dubbel
      |dubbel
      |egendom
      |egendom
      |egendom
      |egendom
      |ekonomi
      |ekonomi
      |ekonomi
      |en dag
      |en dag
      |en dag
      |engelska
      |engelska
      |fall
      |fall
      |fall
      |fall
      |filmografi
      |filmografi
      |filmografi
      |flygbolag
      |flygbolag
      |flygbolag
      |flygplan
      |flygplan
      |främsta
      |främsta
      |främsta
      |främsta
      |fyra
      |fyra
      |fyra
      |fyra
      |fyra
      |fysiker
      |fysiker
      |fängelse
      |fängelse
      |fängelse
      |fängelse
      |fängelse
      |fängelse
      |fängelse
      |först
      |först
      |geografi
      |geografi
      |geografi
      |geografi
      |grå
      |grå
      |gud
      |gud
      |gud
      |gud
      |gud
      |halva
      |hälsa
      |hälsa
      |hälsa
      |hälsa
      |hälsa
      |hälsa
      |identitet
      |identitet
      |identitet
      |inne i
      |inne i
      |inne i
      |inne i
      |inriktning
      |inslag
      |inslag
      |intryck
      |intryck
      |intryck
      |januari
      |januari
      |januari
      |japan
      |japan
      |japan
      |japan
      |japan
      |japan
      |japanska
      |japanska
      |japanska
      |japanska
      |klimat
      |konflikt
      |konstant
      |konstant
      |kraft
      |kraft
      |kraft
      |kropp
      |kropp
      |kropp
      |kropp
      |kropp
      |liv
      |liv
      |liv
      |liv
      |lugn
      |låga
      |låga
      |låga
      |låga
      |låga
      |låga
      |mandat
      |mandat
      |media
      |media
      |media
      |media
      |medlem
      |medlem
      |medlem
      |mina
      |mjölk
      |mjölk
      |mod
      |mod
      |mod
      |mod
      |mod
      |mod
      |mod
      |mod
      |människa
      |natt
      |natt
      |natt
      |natt
      |natt
      |natt
      |natt
      |objekt
      |objekt
      |offer
      |offer
      |offer
      |offer
      |ombord
      |ombord
      |ombord
      |ombord
      |pengar
      |pengar
      |pengar
      |pengar
      |pengar
      |pengar
      |pengar
      |regissör
      |regissör
      |regissör
      |regissör
      |rike
      |rike
      |rike
      |roman
      |roman
      |roman
      |roman
      |röra
      |röra
      |rött
      |saker
      |saker
      |serie
      |serie
      |serie
      |serie
      |serie
      |serie
      |serie
      |serie
      |serie
      |självmord
      |självmord
      |självmord
      |skick
      |start
      |start
      |stjärna
      |stjärna
      |stjärna
      |stjärna
      |stjärna
      |stjärna
      |stjärna
      |stopp
      |stopp
      |straff
      |straff
      |straff
      |stöd
      |tal
      |tal
      |tal
      |tillverkning
      |tillverkning
      |tillverkning
      |tillverkning
      |tillverkning
      |tillverkning
      |ton
      |trä
      |trä
      |trä
      |turné
      |turné
      |turné
      |turné
      |utrustning
      |utsträckning
      |verktyg
      |verktyg
      |verktyg
      |verktyg
      |värde
      |värde
      |värde
      |värde
      |värde
      |webbplats
      |ära
      |ära
      |ära
      |ära
    """.stripMargin.toLowerCase()
  val questionsData =
    """
      |Allingsås=Everying sauce
      |Bara=Only
      |Boden=Liveit
      |Bollnäs=Kula Udde
      |Borlänge=Livelongtime
      |Dalarö=Ramlar du?
      |Djursholm=Islet of animals
      |Eksjö=Trädvatten
      |Enköping=Onebying
      |Fagersta=Fairtown
      |Göteborg=Pojkfästning
      |Halmstad=Stråort
      |Hisingen=ElevatorNobody
      |Hjo=Yes
      |Husqvarna=Housemill
      |Härnösand=Heresneezedduck
      |Höganäs=Highnose
      |Karlskoga=Manforest
      |Karlstad=Charlestown
      |Karlstad=Manort
      |Kivik=Nobay
      |Kramfors=Omfamningsfall
      |Kristinehamn=Christine Harbour
      |Krokom=Hookif
      |Kungsbacka=Regentbakåt
      |Kungälv=King River
      |Kålbäck=Cabbage Creek
      |Köpenhamn=Buy a harbour
      |Landskrona=Crown of country
      |Landskrona=Rikepeng
      |Ljungby=Växtsamhälle
      |Lund=Trädsamling
      |Malmberget=Metallhöjden
      |Malmö=Insektsflicka
      |Malung=Insektsbarn
      |Mariefred=Marypeace
      |Mora Träsk=Mother of roor
      |Mottala=Speakagainst
      |Nacka=Wring the neck of
      |Nybro=Newbridge
      |Nynäshamn=New nose habour
      |Nässjö=Noselake
      |Osby=Olympic village
      |Ostvik=Cheesbay
      |Oxelösund=Healthy of oxloose
      |Råneå=Hands up river
      |Skara=Crowd
      |Skara=Folksamling
      |Skövde=Did they push
      |Strängnäs=Barsk udde
      |Strömstad=Spänningsort
      |Sundbyberg=Healthyvillagemountain
      |Sälen=Valrossen
      |Söderköping=Väderstreckssamhälle
      |Tranås=Fågelhöjd
      |Trelleborg=Castle of threesmile
      |Trollhättan=Skogsväsenmössan
      |Trosa=Flickbyxa
      |Trosa=Smallpants
      |Trångsviken=Tightbay
      |Tungelsta=Heavy elecric town
      |Vadstena=Whatstone
      |Vara=Be
      |Varberg=Infektionshärdshöjd
      |Varberg=Stor böld
      |Varberg=Wheremountain
      |Vargön=Wolfisland
      |Vetlanda=Knowland
      |Visby=Klokt samhälle
      |Vällingby=Barnmatsamhälle
      |Vänersborg=Castle of friends
      |Västerås=West Ridge
      |Växsjö=Growlake
      |Vårgårda=Springyard
      |Åmål=Rivergoal
      |Åmål=Vattenmat
      |Åtvidaberg=Mountain of atewide
      |Ängelholm=Islet of angel
      |Ödeshög=Destiny High
      |Örebro=Myntspång
      |Örebro=Pennybridge
      |Örnsköldsvik=Bay of eagleshield
      |Östersund=East of healthy
    """.stripMargin
}

object Words2 {
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
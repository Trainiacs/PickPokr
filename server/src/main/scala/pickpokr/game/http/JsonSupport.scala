package pickpokr.game.http
import pickpokr.game.http.UserRegistryActor.ActionPerformed
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import pickpokr.game.Player.Challenge
import pickpokr.game.Train.Roaster
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat3(User)
  implicit val usersJsonFormat = jsonFormat1(Users)
  implicit val roasterJsonFormat = jsonFormat1(Roaster)
  implicit val challengeJsonFormat = jsonFormat3(Challenge)
  implicit val actionPerformedJsonFormat = jsonFormat1(ActionPerformed)
}
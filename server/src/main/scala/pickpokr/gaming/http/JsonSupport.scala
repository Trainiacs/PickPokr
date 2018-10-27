package pickpokr
package gaming
package http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{ DefaultJsonProtocol, JsonFormat }

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val nickJsonFormat: JsonFormat[Nick] = jsonFormat1(Nick)
  implicit val roasterJsonFormat: JsonFormat[Train.Roaster] = jsonFormat1(Train.Roaster)
  implicit val challengeJsonFormat: JsonFormat[Client.Challenge] = jsonFormat1(Client.Challenge)
  implicit val deniedJsonFormat: JsonFormat[Client.Denied] = jsonFormat1(Client.Denied)
  implicit val updateRoasterJsonFormat: JsonFormat[Client.Roaster] = jsonFormat1(Client.Roaster)
  implicit val pinJsonFormat: JsonFormat[Client.Pin] = jsonFormat1(Client.Pin)
  implicit val outcomeJsonFormat: JsonFormat[Client.Outcome] = jsonFormat2(Client.Outcome)
}
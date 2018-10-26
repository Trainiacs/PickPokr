package pickpokr
package gaming
package http

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import pickpokr.gaming.Client.Challenge
import pickpokr.gaming.Train.Roaster
import spray.json.DefaultJsonProtocol

trait JsonSupport extends SprayJsonSupport {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val nickJsonFormat = jsonFormat1(Nick)
  implicit val roasterJsonFormat = jsonFormat1(Roaster)
  implicit val challengeJsonFormat = jsonFormat3(Challenge)
}
package pickpokr
package game

sealed trait Color
object Color {
  case object Hearts extends Color {
    override def toString = "♥"
  }
  case object Spades extends Color {
    override def toString = "♠"
  }
  case object Diamonds extends Color {
    override def toString = "♦"
  }
  case object Clubs extends Color {
    override def toString = "♣"
  }
}
case class Rank(value: Int) extends AnyVal
case class Card(color: Color, rank: Rank) {
}

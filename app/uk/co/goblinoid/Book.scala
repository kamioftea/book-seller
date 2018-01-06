package uk.co.goblinoid

import java.time.LocalDate

case class Author(id: Option[Long], name: String)

case class Book(id: Option[Long],
                title: String,
                description: String,
                authors: Seq[Author],
                published: String,
                isbn: Long,
                thumbnailUrl: String,
               )
{
  private val Splitter = "(\\d{3})(\\d{1})(\\d{4})(\\d{4})(\\d{1})".r

  lazy val formattedIsbn:String = isbn.toString match {
    case Splitter(a, b, c, d, e) => Seq(a,b,c,d,e).mkString("-")
    case str => str
  }
}

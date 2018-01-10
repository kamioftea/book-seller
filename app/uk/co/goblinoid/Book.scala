package uk.co.goblinoid

object BookType {
  def apply(dbAlias: String): Option[BookType] = dbAlias match {
    case "novel" => Some(Novel)
    case "short_stories" => Some(ShortStories)
    case "non_fiction" => Some(NonFiction)
    case "graphic_novel" => Some(GraphicNovel)
    case _ => None
  }
}

sealed abstract class BookType(val dbAlias: String)

case object Novel extends BookType("novel")

case object ShortStories extends BookType("short_stories")

case object NonFiction extends BookType("non_fiction")

case object GraphicNovel extends BookType("graphic_novel")

object BookFormat {
  def apply(dbAlias: String): Option[BookFormat] = dbAlias match {
    case "paperback" => Some(Paperback)
    case "hardback" => Some(Hardback)
    case _ => None
  }
}

sealed abstract class BookFormat(val dbAlias: String)

case object Paperback extends BookFormat("paperback")

case object Hardback extends BookFormat("hardback")

case class Author(id: Option[Long], name: String)

case class Book(id: Option[Long],
                title: String,
                description: String,
                authors: Seq[Author],
                published: String,
                isbn: Long,
                thumbnailUrl: String,
                bookType: Option[BookType] = None,
                bookFormat: Option[BookFormat] = None,
                oversized: Boolean = false,
                notes: String = "",
                series: Option[String] = None,
                seriesPosition: Option[Int] = None
               ) {
  private val Splitter = "(\\d{3})(\\d{1})(\\d{4})(\\d{4})(\\d{1})".r

  lazy val formattedIsbn: String = isbn.toString match {
    case Splitter(a, b, c, d, e) => Seq(a, b, c, d, e).mkString("-")
    case str => str
  }
}

case class Location(id: Option[Long] = None, name: String)

case class Stock(id: Option[Long], bookId: Long, locationId: Long, conditionDescription: Option[String])

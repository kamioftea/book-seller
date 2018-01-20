package controllers

import java.sql.Connection
import java.util.Locale
import javax.inject._

import anorm.SQL
import anorm.SqlParser._
import play.api._
import play.api.db._
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc._
import uk.co.goblinoid._
import play.api.data._
import play.api.data.Forms._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cc: ControllerComponents,
                               config: Configuration,
                               ws: WSClient,
                               db: Database,
                               ec: ExecutionContext) extends AbstractController(cc) {

  implicit val _: ExecutionContext = ec

  /**
    * Create an Action to render an HTML page.
    *
    * The configuration in the `routes` file means that this method
    * will be called when the application receives a `GET` request with
    * a path of `/`.
    */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  import java.time.format.DateTimeFormatter

  val formatter: DateTimeFormatter =
    DateTimeFormatter
      .ofPattern("yyyy-mm-dd")
      .withLocale(Locale.UK)

  implicit val conn: Connection = db.getConnection()

  implicit val locationLookup: Map[Long, Location] =
    SQL("SELECT id, name FROM location")
      .as((long("id") ~ str("name") map flatten map { case (id, n) => id -> Location(Some(id), n) }).*)
      .toMap

  def getAuthors(bookId: Long): Seq[Author] =
    SQL(
      """SELECT a.id, a.name
        |FROM author a
        |JOIN book_author ba ON (ba.author_id = a.id)
        |WHERE ba.book_id = {bookId}""".stripMargin
    )
      .on("bookId" -> bookId)
      .as((long("id") ~ str("name") map flatten map { case (i, n) => Author(Some(i), n) }).*)


  def getStock(b: Book): Seq[Stock] =
    SQL("SELECT id, location_id, condition_description FROM stock WHERE book_id = {bookId}")
      .on("bookId" -> b.id.get)
      .as((long("id") ~ long("location_id") ~ str("condition_description").? map flatten map { case (id, l_id, sd) => Stock(Some(id), b.id.get, l_id, sd) }).*)

  def book(id: Long): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] => {
      val res = SQL(
        """SELECT
           b.id,
           title,
           description,
           isbn,
           published,
           thumbnail_url,
           published,
           type,
           format,
           oversized,
           notes,
           series,
           series_position
           FROM book b
           WHERE b.id = {id}
        """
      )
        .on("id" -> id)
        .as(
          (long("id") ~
            str("title") ~
            str("description") ~
            str("isbn") ~
            str("thumbnail_url") ~
            str("published") ~
            str("type").? ~
            str("format").? ~
            bool("oversized") ~
            str("notes").? ~
            str("series").? ~
            int("series_position").? map flatten).*
        )

      res.headOption.map {
        case (i, t, d, isbn, tu, p, bt, f, o, n, s, sp) =>
          Book(Some(i), t, d, getAuthors(id), p, isbn.toLong, tu, BookType(bt.getOrElse("")), BookFormat(f.getOrElse("")), o, n.getOrElse(""), s, sp)
      }
        .map(b => Ok(views.html.index(Some(b), getStock(b))))
        .getOrElse(NotFound)


    }
  }

  def addBook(title: String, description: String, isbn: Long, publishedDate: String, imgUrl: String): Option[Long] =
    SQL("INSERT INTO book SET title = {title}, description = {description}, isbn = {isbn}, published = {published}, thumbnail_url = {url}")
      .on(
        "title" -> title,
        "description" -> description,
        "isbn" -> isbn,
        "published" -> publishedDate,
        "url" -> imgUrl
      ).executeInsert()

  def getAuthorId(name: String): Option[Long] =
    SQL("SELECT id FROM author WHERE name = {name}").on("name" -> name).as(long("id").singleOpt)
      .orElse(SQL("INSERT INTO author SET name = {name}").on("name" -> name).executeInsert())


  def addAuthor(bookId: Long, authorId: Long): Unit =
    SQL("INSERT IGNORE INTO book_author SET book_id = {book_id}, author_id = {author_id}")
      .on(
        "book_id" -> bookId,
        "author_id" -> authorId
      )
      .execute()

  def barcode(barcode: Long): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      val bookId: Option[Long] = SQL("SELECT id FROM book WHERE isbn = {isbn}").on("isbn" -> barcode).as(long("id").singleOpt)
      bookId.map(id => Future.successful(Redirect(routes.HomeController.book(id))))
        .getOrElse {
          val key = config.get[String]("uk.co.goblinoid.googleBooks.key")
          val req = ws.url("https://www.googleapis.com/books/v1/volumes")
            .withQueryStringParameters(
              "q" -> s"isbn:$barcode",
              "key" -> key
            )

          req.get.map(res => {
            val bookId = for {
              json <- (res.json \ "items" \ 0 \ "volumeInfo").toOption
              title <- (json \ "title").asOpt[String]
              description <- (json \ "description").asOpt[String]
              authors <- (json \ "authors").asOpt[Seq[String]]
              publishedDate <- (json \ "publishedDate").asOpt[String]
              isbn <- (json \ "industryIdentifiers")
                .asOpt[JsArray]
                .flatMap(seq =>
                  seq.value
                    .find(t => (t \ "type").asOpt[String].contains("ISBN_13"))
                    .flatMap(t => (t \ "identifier").asOpt[String])
                )
              imgUrl <- (json \ "imageLinks" \ "thumbnail").asOpt[String]
              bookId <- addBook(title, description, isbn.toLong, publishedDate, imgUrl)
            } yield {
              authors.flatMap(getAuthorId).foreach(addAuthor(bookId, _))

              bookId
            }

            bookId.map(id => Redirect(routes.HomeController.book(id)))
              .getOrElse(Ok(views.html.index()))
          })
        }
  }

  def updateBook(bookId: Long): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>

    Form(tuple("key" -> text, "value" -> text)).bindFromRequest().fold(
      _ => BadRequest,
      {
        case ("notes", v) =>
          if (SQL("UPDATE book SET notes = {value} WHERE id = {bookId}").on(
            "value" -> v,
            "bookId" -> bookId
          ).executeUpdate() == 1) Ok else InternalServerError

        case ("type", v) =>
          if (SQL("UPDATE book SET type = {value} WHERE id = {bookId}").on(
            "value" -> BookType(v).map(_.dbAlias),
            "bookId" -> bookId
          ).executeUpdate() == 1) Ok else InternalServerError

        case ("format", v) =>
          if (SQL("UPDATE book SET format = {value} WHERE id = {bookId}").on(
            "value" -> BookFormat(v).map(_.dbAlias),
            "bookId" -> bookId
          ).executeUpdate() == 1) Ok else InternalServerError

        case ("oversized", v) =>
          if (SQL("UPDATE book SET oversized = {value} WHERE id = {bookId}").on(
            "value" -> (v == "true"),
            "bookId" -> bookId
          ).executeUpdate() == 1) Ok else InternalServerError

        case ("series", v) =>
          if (SQL("UPDATE book SET series = {value} WHERE id = {bookId}").on(
            "value" -> v,
            "bookId" -> bookId
          ).executeUpdate() == 1) Ok else InternalServerError

        case ("series_position", v) =>
          if (SQL("UPDATE book SET series_position = {value} WHERE id = {bookId}").on(
            "value" -> Try(v.toInt).toOption,
            "bookId" -> bookId
          ).executeUpdate() == 1) Ok else InternalServerError

        case _ => BadRequest
      }
    )
  }

  def addStock(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Form(tuple(
      "book_id" -> number,
      "location_id" -> number
    )).bindFromRequest.fold(
      _ => BadRequest,
      {
        case (b_id, l_id) =>
          SQL("INSERT INTO stock SET book_id = {b_id}, location_id = {l_id}")
            .on("b_id" -> b_id, "l_id" -> l_id)
            .executeInsert()
          Redirect(routes.HomeController.book(b_id))
      }
    )
  }

  def updateStock(stockId: Long) = Action { implicit request: Request[AnyContent] =>
    Form(tuple(
      "key" -> text,
      "value" -> text
    )).bindFromRequest().fold(
      _ => BadRequest,
      {
        case ("location", v) =>
          if (SQL("UPDATE stock SET location_id = {value} WHERE id = {stockId}")
            .on("value" -> v.toLong, "stockId" -> stockId)
            .executeUpdate() == 1) Ok else InternalServerError

        case ("condition", v) =>
          if (SQL("UPDATE stock SET condition_description = {value} WHERE id = {stockId}")
            .on("value" -> Some(v).filter(_ != ""), "stockId" -> stockId)
            .executeUpdate() == 1) Ok else InternalServerError

        case _ => BadRequest
      }
    )
  }

  def removeStock(stockId: Long) = Action { implicit request: Request[AnyContent] =>
    val bookId = SQL("SELECT book_id FROM stock WHERE id = {stockId}")
      .on("stockId" -> stockId)
      .as(long("book_id").single)

    SQL("DELETE FROM stock WHERE id = {stockId}")
      .on("stockId" -> stockId)
      .executeUpdate()

    Redirect(routes.HomeController.book(bookId))
  }
}

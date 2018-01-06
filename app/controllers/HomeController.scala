package controllers

import java.sql.Connection
import java.time.LocalDate
import java.util.Locale
import javax.inject._

import play.api._
import play.api.libs.ws.WSClient
import play.api.db._
import play.api.mvc._
import play.api.libs.json._
import anorm.{SQL, SqlParser}
import anorm.SqlParser._
import play.api.libs.functional.syntax._
import uk.co.goblinoid.{Author, Book}

import scala.concurrent.{ExecutionContext, Future}

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

  def book(id: Long): Action[AnyContent] = Action {
    implicit request: Request[AnyContent] => {
      val res: Seq[(Long, String, String, String, String, String, Long, String)] = SQL(
        """SELECT
           b.id, title, description, isbn, published, thumbnail_url, published, a.id AS author_id, a.name
           FROM book b
           JOIN book_author ba ON b.id = ba.book_id
           JOIN author a ON ba.author_id = a.id
           WHERE b.id = {id}
        """
      )
        .on("id" -> id)
        .as(
          (long("id") ~ str("title") ~ str("description") ~ str("isbn") ~ str("thumbnail_url") ~ str("published") ~ long("author_id") ~ str("name") map flatten).*
        )

      res.headOption.map {
        case (id, t, d, i, tu, p, _, _) =>
          Book(Some(id), t, d, res.map {case (_, _, _, _, _, _, aid, n) => Author(Some(aid), n)}, p, i.toLong, tu)
      }
        .map(b => Ok(views.html.index(Some(b))))
        .getOrElse(NotFound)


    }}

  def addBook(title:String, description:String, isbn:Long, publishedDate:String, imgUrl: String): Option[Long] =
    SQL("INSERT INTO book SET title = {title}, description = {description}, isbn = {isbn}, published = {published}, thumbnail_url = {url}")
      .on(
        "title" -> title,
        "description" -> description,
        "isbn" -> isbn,
        "published" -> publishedDate,
        "url" -> imgUrl
      ).executeInsert()

  def getAuthorId(name: String): Option[Long] = {
    println(name);
    SQL("SELECT id FROM author WHERE name = {name}").on("name" -> name).as(long("id").singleOpt)
      .orElse(SQL("INSERT INTO author SET name = {name}").on("name" -> name).executeInsert())
  }

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
}

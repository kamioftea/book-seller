package controllers

import java.time.LocalDate
import java.util.Locale
import javax.inject._

import play.api._
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.co.goblinoid.Book

import scala.concurrent.ExecutionContext

/**
  * This controller creates an `Action` to handle HTTP requests to the
  * application's home page.
  */
@Singleton
class HomeController @Inject()(cc: ControllerComponents,
                               config: Configuration,
                               ws: WSClient,
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


  def barcode(barcode: Long): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      val key = config.get[String]("uk.co.goblinoid.googleBooks.key")
      val req = ws.url("https://www.googleapis.com/books/v1/volumes")
        .withQueryStringParameters(
          "q" -> s"isbn:$barcode",
          "key" -> key
        )

      req.get.map(res => {
        val book = for{
          json <- (res.json \ "items" \ 0 \ "volumeInfo").toOption
          title <- (json \ "title").get.asOpt[String]
          description <- (json \ "description").get.asOpt[String]
          authors <- (json \ "authors").get.asOpt[Seq[String]]
          publishedDate <- (json \ "publishedDate").get.asOpt[String]
          isbn <- (json \ "industryIdentifiers" \ 1 \ "identifier").get.asOpt[String]
          imgUrl <- (json \ "imageLinks" \ "thumbnail").get.asOpt[String]
        } yield Book(title, description, authors, publishedDate, isbn.toLong, imgUrl)

        Ok(views.html.index(book))
      })
  }
}

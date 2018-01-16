package controllers

import javax.inject.Inject

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.name.Named
import db.books.Tables._
import db.books.Tables.profile.api._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import slick.jdbc.JdbcProfile
import uk.co.goblinoid.GoogleBooks
import uk.co.goblinoid.GoogleBooks.{FetchBook, GoogleBook}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class BookController @Inject()(cc: ControllerComponents,
                               val dbConfigProvider: DatabaseConfigProvider,
                               @Named("google-books") googleBooks: ActorRef
                              )(implicit ex: ExecutionContext)
  extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  implicit val timeout: Timeout = 5.seconds

  def view(id: Long): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      db.run(
        Book.filter(_.id === id).result
      )
        .map(
          bs => bs.headOption.fold
          (NotFound("Nope"))
          ((b: BookRow) => Ok(b.toString))
        )
  }

  def barcode(barcode: String): Action[AnyContent] =
    Action.async { implicit req: Request[AnyContent] =>
      (googleBooks ? FetchBook(barcode))
        .mapTo[Option[GoogleBook]]
        .flatMap {
          case Some(book) =>
            val row = BookRow(
              0L,
              isbn = barcode,
              title = Some(book.title),
              description = Some(book.description),
              thumbnailUrl = book.imgUrl
            )

            val bookId = db.run((Book returning Book.map(_.id)) += row)
            val authorIds = db.run(
              (Author returning Author.map(_.id)) ++=
                book.authors.map(name => AuthorRow(0L, name))
            )
            val futureIds = for {
              b <- bookId
              as <- authorIds
            } yield as.map(a => BookAuthorRow(b, a))

            futureIds.foreach(seq => db.run(BookAuthor ++= seq))

            bookId

          case None =>
            db.run((Book returning Book.map(_.id)) += BookRow(0L, isbn = barcode))
        }
        .map(bookId => Redirect(routes.BookController.view(bookId)))
    }
}

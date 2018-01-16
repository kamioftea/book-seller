package uk.co.goblinoid

import javax.inject.Inject

import akka.actor.{Actor, Props}
import play.api.Configuration
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

object GoogleBooks {

  def props = Props[GoogleBooks]

  case class FetchBook(isbn: String)

  case class GoogleBook(title: String,
                        description: String,
                        authors: Seq[String],
                        publishedDate: Option[String],
                        imgUrl: Option[String])

  implicit val BookReads: Reads[GoogleBook] = (
    (JsPath \ "title").read[String]
      and (JsPath \ "description").read[String]
      and (JsPath \ "authors").read[Seq[String]]
      and (JsPath \ "publishedDate").readNullable[String]
      and (JsPath \ "imageLinks" \ "thumbnail").readNullable[String]
    ) (GoogleBook.apply _)
}

class GoogleBooks @Inject()(config: Configuration,
                            ws: WSClient) extends Actor {

  import GoogleBooks._

  val key = config.get[String]("uk.co.goblinoid.googleBooks.key")
  val req =
    ws.url("https://www.googleapis.com/books/v1/volumes")
      .withQueryStringParameters("key" -> key)

  implicit val ec: ExecutionContext = context.dispatcher

  override def receive: Receive = {
    case FetchBook(isbn) =>
      val replyTo = sender()
      req
        .withQueryStringParameters("q" -> s"isbn:$isbn")
        .get.map(res => (res.json \ "items" \ 0 \ "volumeInfo").validate[GoogleBooks.GoogleBook].asOpt)
        .onComplete(res => replyTo ! res.toOption.flatten)
  }
}

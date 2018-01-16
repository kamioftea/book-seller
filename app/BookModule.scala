import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport
import uk.co.goblinoid.GoogleBooks

class BookModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[GoogleBooks]("google-books")
  }
}

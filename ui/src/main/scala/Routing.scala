package default

import com.github.lavrov.bittorrent.InfoHash
import com.raquo.waypoint._
import com.raquo.laminar.api.L
import upickle.default._
import urldsl.errors.DummyError
import urldsl.vocabulary.{FromString, Printer}
import com.github.lavrov.bittorrent.app.protocol.CommonFormats.infoHashRW


object Routing {

  sealed trait Page
  object Page {
    case object Root extends Page
    case class Torrent(infoHash: InfoHash) extends Page
  }

  implicit val torrentPageRW: ReadWriter[Page.Torrent] = macroRW
  implicit val pageRW: ReadWriter[Page] = macroRW

  val pathPrefix = root / "laminar-bulma"

  implicit val infoHashFromString: FromString[InfoHash, DummyError] =
    FromString
      .factory(InfoHash.fromString.lift.andThen(_.toRight(DummyError.dummyError)))

  implicit val infoHashPrinter: Printer[InfoHash] = Printer.factory(_.toString)

  val router = new Router[Page](
    routes = List(
      Route.static(
        staticPage = Page.Root,
        pattern = pathPrefix / endOfSegments
      ),
      Route[Page.Torrent, InfoHash](
        _.infoHash,
        infoHash => Page.Torrent(infoHash),
        pattern = pathPrefix / "torrent" / segment[InfoHash] / endOfSegments
      )
    ),
    getPageTitle = _.toString,
    serializePage = page => write(page)(pageRW),
    deserializePage = pageStr => read(pageStr)(pageRW),
    routeFallback = _ => Page.Root
  )(
    $popStateEvent = L.windowEvents.onPopState,
    owner = L.unsafeWindowOwner,
  )
}

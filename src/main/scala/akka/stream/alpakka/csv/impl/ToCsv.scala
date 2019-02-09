package akka.stream.alpakka.csv.impl

import java.nio.charset.StandardCharsets

import akka.stream.alpakka.csv.scaladsl.CsvFormatting.{Backslash, Comma, DoubleQuote}
import akka.stream.alpakka.csv.scaladsl.CsvQuotingStyle
import akka.util.ByteString

import scala.collection.immutable.Iterable

// TODO relying on akka internals is bad practice.
object ToCsv {

  private val csvFormatter = new CsvFormatter(
    delimiter = Comma,
    quoteChar = DoubleQuote,
    escapeChar = Backslash, endOfLine = "\r\n",
    quotingStyle = CsvQuotingStyle.Required,
    charset = StandardCharsets.UTF_8)


  /** returns empty ByteString on None */
  def format(elems: Option[Iterable[Any]]): ByteString = elems.fold(ByteString.empty)(csvFormatter.toCsv)

}

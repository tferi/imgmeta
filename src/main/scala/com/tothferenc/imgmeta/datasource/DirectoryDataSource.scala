package com.tothferenc.imgmeta.datasource

import java.io.{BufferedInputStream, File, FileFilter, FileInputStream}
import java.nio.file.Path

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{FileIO, Source, StreamConverters}
import com.tothferenc.imgmeta.model.{Album, Image, StreamIn}

import scala.concurrent.duration._

final case class DirectoryDataSource(root: Path)(implicit m: Materializer) extends DataSource {
  private val dataSourceName = root.toString

  private def getFilePaths(path: Path): Source[Path, NotUsed] = {
    val (dirs, rest) = path.toFile.listFiles().partition(_.isDirectory)
    val thisAlbum =
      if (rest.nonEmpty)
        Source.single(path).concat(Source.fromIterator(() => rest.iterator.map(_.toPath)))
      else
        Source.empty
    dirs.foldLeft(thisAlbum)((s, f) => s concat Source.lazily(() => getFilePaths(f.toPath)))
  }

  override def getImageFiles(): Source[StreamIn, NotUsed] = getFilePaths(root).mapConcat { filePath =>
    if (filePath.toFile.isDirectory) {

      val filesInDir = filePath.toFile.listFiles(new FileFilter {
        override def accept(pathname: File): Boolean = pathname.isFile
      }).length // it's not optimal that we crawl the dir twice, but it'll do for now.

      if (filesInDir == 0)
        Nil
      else
        List(StreamIn.AlbumAnnouncement(Album(
          dataSourceName,
          root.relativize(filePath).toString,
          Some(filesInDir))))
    }
    else List(StreamIn.Elem(Image(
      dataSourceName,
      root.relativize(filePath.getParent).toString,
      filePath.getFileName.toString,
      () => new BufferedInputStream(new FileInputStream(filePath.toFile), 8192 * 128 ))))
  }
}

package com.tothferenc.imgmeta.datasource

import java.io.{File, FileFilter, FileInputStream}
import java.nio.file.{Files, Path}

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.StreamConverters.fromJavaStream
import com.tothferenc.imgmeta.model.{Album, Image, StreamIn}

final case class DirectoryDataSource(path: Path) extends DataSource {
  val dataSourceName = path.toString

  private def getFilePaths(): Source[Path, NotUsed] = {
    fromJavaStream(() => Files.walk(path))
  }

  override def getImageFiles(): Source[StreamIn, NotUsed] = getFilePaths().mapConcat { filePath =>
    if (filePath.toFile.isDirectory) {

      val filesInDir = filePath.toFile.listFiles(new FileFilter {
        override def accept(pathname: File): Boolean = pathname.isFile
      }).length // it's not optimal that we crawl the dir twice, but it'll do for now.

      if (filesInDir == 0)
        Nil
      else
        List(StreamIn.AlbumAnnouncement(Album(
          dataSourceName,
          path.relativize(filePath).toString,
          Some(filesInDir))))
    }
    else List(StreamIn.Elem(Image(
      dataSourceName,
      path.relativize(filePath.getParent).toString,
      filePath.getFileName.toString,
      () => new FileInputStream(filePath.toFile))))
  }
}

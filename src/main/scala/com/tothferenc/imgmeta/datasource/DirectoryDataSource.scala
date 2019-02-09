package com.tothferenc.imgmeta.datasource

import java.io.{BufferedInputStream, FileInputStream}
import java.nio.file.Path

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.tothferenc.imgmeta.model.{Album, Image, StreamIn}


final case class DirectoryDataSource(root: Path)(implicit m: Materializer) extends DataSource {
  private val dataSourceName = root.toString

  private case class Input(path: Path, numFilesInDir: Option[Int])

  private def getFilePaths(path: Path): Source[Input, NotUsed] = {
    val (dirs, rest) = path.toFile.listFiles().partition(_.isDirectory)
    val thisAlbum =
      if (rest.nonEmpty)
        Source.single(Input(path, Some(rest.length))).concat(Source.fromIterator(() => rest.iterator.map(f => Input(f.toPath, None))))
      else
        Source.empty
    dirs.foldLeft(thisAlbum)((s, f) => s concat Source.lazily(() => getFilePaths(f.toPath)))
  }

  override def getImageFiles(): Source[StreamIn, NotUsed] = getFilePaths(root).mapConcat { in =>
    val filePath = in.path
    in.numFilesInDir.fold[List[StreamIn]] {
      List(StreamIn.Elem(Image(
        dataSourceName,
        root.relativize(filePath.getParent).toString,
        filePath.getFileName.toString,
        () => new BufferedInputStream(new FileInputStream(filePath.toFile), 8192 * 128))))
    } { filesInDir =>
      if (filesInDir == 0)
        Nil
      else
        List(StreamIn.AlbumAnnouncement(Album(
          dataSourceName,
          root.relativize(filePath).toString,
          Some(filesInDir))))
    }
  }
}

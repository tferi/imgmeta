package com.tothferenc.imgmeta.datasource

import java.io.{File, FileFilter, FileInputStream}
import java.nio.file.{Files, Path}
import java.util.stream

import com.tothferenc.imgmeta.model.{Album, Image, StreamIn}

final case class DirectoryDataSource(path: Path) extends DataSource {
  val dataSourceName = path.toString

  private def getFilePaths(): stream.Stream[Path] = {
    Files.walk(path)//.filter((p: Path) => !p.toFile.isDirectory)
  }

  override def getImageFiles(): stream.Stream[StreamIn] = getFilePaths().map { filePath =>
    val albumName = path.relativize(filePath.getParent).toString
    if (filePath.toFile.isDirectory)
      StreamIn.AlbumAnnouncement(Album(
        dataSourceName,
        albumName,
        Some(filePath.toFile.listFiles(new FileFilter {
          override def accept(pathname: File): Boolean = pathname.isFile
        }).length)))
    else StreamIn.Elem(Image(
      dataSourceName,
      albumName,
      filePath.getFileName.toString,
      () => new FileInputStream(filePath.toFile)))
  }
}

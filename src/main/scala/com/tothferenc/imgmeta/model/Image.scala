package com.tothferenc.imgmeta.model

import java.io.InputStream

final case class Image(dataSource: String, album: String, name: String, getContents: () => InputStream)

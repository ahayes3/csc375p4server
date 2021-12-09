import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

/*
packet design
index - 4
padding - 4
width - 4
height -4
cells - a lot
 */

/*
Response design
index - 4
width -4
height - 4
values - a bunch
 */

class Responder(val sChannel: SocketChannel) extends Runnable {
  val buffSize = 10000
  val buff = ByteBuffer.allocate(buffSize)

  override def run(): Unit = {
    try {
      var totalBytes = 0
      sChannel.configureBlocking(false)
      val sizeBuff = ByteBuffer.allocate(8)
      while ( {
        val bytesRead = sChannel.read(sizeBuff)
        if (bytesRead == -1)
          throw new SocketException("Connection closed")
        totalBytes += bytesRead
        (bytesRead != 0 || totalBytes != 8)
      }) {}
      sizeBuff.flip()
      val width = sizeBuff.getInt()
      val height = sizeBuff.getInt()

      val bytesToRead = 16 + (8 * 7 * width * height)
      totalBytes = 0
      while ( {
        val bytesRead = sChannel.read(buff)
        if (bytesRead == -1)
          throw new SocketException("Connection closed")
        totalBytes += bytesRead
        (totalBytes != bytesToRead)
      }) {}
      buff.flip()
      val index = buff.getInt()
      val padding = buff.getInt()
      //      val width = buff.getInt()
      //      val height = buff.getInt()
      val tl = Coord(if ((padding & Padding.LEFT) == Padding.LEFT) 1 else 0, if ((padding & Padding.TOP) == Padding.TOP) 1 else 0)
      val br = Coord(if ((padding & Padding.RIGHT) == Padding.RIGHT) width - 1 else width, if ((padding & Padding.BOTTOM) == Padding.BOTTOM) height - 1 else height)
      val cells = for (i <- 0 until width) yield {
        (for (j <- 0 until height) yield {
          Cell.from(buff)
        })
      }

      val out = for (i <- tl.x until br.x) yield {
        for (j <- tl.y until br.y) yield {
          val neighbors = getNeighbors(i, j, cells)
          val oldCell = cells(i)(j)


          val partTemps = neighbors.map(p => p.tempProps()).reduce((a, b) => ((a._1 + b._1), (a._2 + b._2), (a._3 + b._3)))
          val adjusted: (Double, Double, Double) = ((partTemps._1 * oldCell.cm1), ((partTemps._2 * oldCell.cm2)), ((partTemps._3 * oldCell.cm3)))
          (adjusted._1 + adjusted._2 + adjusted._3) / neighbors.length //new temp
        }
      }
      buff.clear()

      buff.putInt(index)
      buff.putInt(br.x - tl.x) //width
      buff.putInt(br.y - tl.y) //height
      out.flatten.foreach(p => buff.putDouble(p))
      buff.flip()
      var t = 0
      while ( {
        val written = sChannel.write(buff)
        t += written
        //println(s"Wrote index $index")
        buff.hasRemaining
      }) {}
      println(s"Index $index   Total bytes: $t")
      val tmp = ByteBuffer.allocate(4)
      var tot = 0
      while ( {
        val read = sChannel.read(tmp)
        tot += read
        if (tot == 4) {
          tmp.flip()
          if (tmp.getInt() == -2)
            false
          else
            true
        }
        else
          true
      }
      ) {}

      sChannel.close()
    } catch {
      case e: SocketException => sChannel.close()
    }
    //sChannel.close() //might cause end before all written

  }

  def getNeighbors(x: Int, y: Int, arr: Seq[Seq[Cell]]): IndexedSeq[Cell] = {
    var neighbors = IndexedSeq[Cell]()
    if (x > 0)
      neighbors :+= arr(x - 1)(y)

    if (x < arr.length - 1)
      neighbors :+= arr(x + 1)(y)

    if (y > 0)
      neighbors :+= arr(x)(y - 1)

    if (y < arr(x).length - 1)
      neighbors :+= arr(x)(y + 1)

    neighbors
  }
}

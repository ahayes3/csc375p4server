import java.nio.ByteBuffer
import java.nio.channels.SocketChannel
/*
packet design
padding - 4
width - 4
height -4
cells - a lot
 */

class Responder(val sChannel:SocketChannel) extends Runnable{
  val buffSize = 10000
  val buff = ByteBuffer.allocate(buffSize)
  override def run(): Unit = {
    var totalBytes = 0
    while({
      val bytesRead = sChannel.read(buff)
      if(bytesRead == -1)
        throw new IndexOutOfBoundsException
      totalBytes += bytesRead
      bytesRead != 0
    }){}
    buff.flip()
    val padding = buff.getInt()
    val width = buff.getInt()
    val height = buff.getInt()
    val tl = Coord(if((padding & Padding.LEFT)==Padding.LEFT) 1 else 0, if((padding & Padding.TOP)==Padding.TOP) 1 else 0)
    val br = Coord(if((padding & Padding.RIGHT)==Padding.RIGHT) width-1 else width, if((padding & Padding.BOTTOM)==Padding.BOTTOM) height -1 else height)
    val cells = for(i <- 0 until width) yield {
       (for(j <- 0 until height) yield {
         Cell.from(buff)
       })
     }

    val out = for(i <- tl.x until br.x) yield {
      for(j <- tl.y until br.y) yield {
        val neighbors = getNeighbors(i,j,cells)
        val oldCell = cells(i)(j)

        val thermConsts = (neighbors(0).cm1, neighbors(0).cm2, neighbors(0).cm3)
        val partTemps = neighbors.map(p => p.tempProps()).reduce((a, b) => ((a._1 + b._1), (a._2 + b._2), (a._3 + b._3)))
        val adjusted: (Double, Double, Double) = ((partTemps._1 * oldCell.cm1), ((partTemps._2 * oldCell.cm2)), ((partTemps._3 * oldCell.cm3)))
        (adjusted._1 + adjusted._2 + adjusted._3) / neighbors.length //new temp
      }
    }
    buff.clear()
    out.flatten.foreach(p => buff.putDouble(p))
    buff.flip()
    sChannel.write(buff)
    sChannel.close() //might cause end before all written

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
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.Executors
import scala.util.Using

object Main {
  def main(args: Array[String]): Unit = {
    val server = ServerSocketChannel.open()
    server.configureBlocking(false)
    var port = 8001 //default
    for(i <- args.indices) {
      if(args(i) == "-p" && args.length > i+1) {
        port = args(i+1).toInt
      }

    }

    server.socket().bind(new InetSocketAddress(port))
    //todo wait until connection, fork
    while(true) {
      val a = server.accept()
      if(a!= null) {
        println("accepted one")
        new Thread(new Responder(a)).start()
      }

    }
  }
}

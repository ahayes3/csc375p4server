import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.Executors
import scala.util.Using

object Main {
  def main(args: Array[String]): Unit = {
    val server = ServerSocketChannel.open()
    server.configureBlocking(false)
    val port = 8001
    server.socket().bind(new InetSocketAddress(port))
    //todo wait until connection, fork
    while(true) {
      val a = server.accept()
      if(a!= null)
        new Thread(new Responder(a)).start()

    }
  }
}

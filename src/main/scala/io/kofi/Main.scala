package io.kofi

import com.twitter.finagle._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.loadbalancer.{BalancerRegistry, Balancers}
import com.twitter.finagle.util.DefaultTimer
import com.twitter.server.TwitterServer
import com.twitter.server.util.JsonConverter
import com.twitter.util.{Await, Closable, Future, Var}
import com.twitter.util.TimeConversions._

object Main extends TwitterServer {
  implicit val _timer = DefaultTimer

  val service = new Service[Request, Response] {
    override def apply(request: Request): Future[Response] = {

      val jsonString = JsonConverter.writeToString(
        BalancerRegistry.get.allMetadata.map { md =>
          Map(
            "label" -> md.label,
            "info" -> Map(
              "balancer_class" -> md.balancerClass,
              "status" -> md.status,
              "number_available" -> md.numAvailable,
              "number_busy" -> md.numBusy,
              "number_closed" -> md.numClosed,
              "total_pending" -> md.totalPending,
              "total_load" -> md.totalLoad,
              "size" -> md.size,
              "additional_info" -> md.additionalInfo
            )
          )
        }
      )
      val rsp = Response()
      rsp.contentString = jsonString
      Future.value(rsp)
    }
  }

  private[this] val addrs = Var.async[Addr](Addr.Pending) { state =>
    def loop(addrToggle: Boolean): Future[Unit] = {
      if (addrToggle) {
        println("Updating addr set with IP")
        state.update(Addr.Bound(Address("34.213.54.27", 80)))
      } else {
        println("Emptying address set")
        state.update(Addr.Bound(Set[Address]()))
      }
      BalancerRegistry.get.allMetadata.map { md =>
        Map(
          "label" -> md.label,
          "info" -> Map(
            "balancer_class" -> md.balancerClass,
            "status" -> md.status,
            "number_available" -> md.numAvailable,
            "number_busy" -> md.numBusy,
            "number_closed" -> md.numClosed,
            "total_pending" -> md.totalPending,
            "total_load" -> md.totalLoad,
            "size" -> md.size,
            "additional_info" -> md.additionalInfo
          )
        )
      }
      Future.sleep(3.seconds).before(loop(!addrToggle))
    }

    loop(true)

    Closable.make { _ =>
      Future.Unit
    }
  }

  def main() = {
    Http.client.withLoadBalancer(Balancers.p2c())
      .newService(Name.Bound(addrs, "addrSet"), "balancer-leak")

    val server = Http.serve(":8888", service)
    onExit {
      server.close()
    }

    Await.ready(server)
  }
}

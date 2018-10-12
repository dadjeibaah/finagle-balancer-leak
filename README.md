### BalancerRegistry Leak

This repo is a reproduction scenario used to show an potential back in Finagle's LoadBalancer
module. The project includes a TwitterServer that exposes the Finagle's 
[`BalancerRegistry`](https://github.com/twitter/finagle/blob/develop/finagle-core/src/main/scala/com/twitter/finagle/loadbalancer/BalancerRegistry.scala) that 
shows all currently available load balancers that Finagle clients use.

The app contains a Finagle client that sets up a client with a changeable address set. This address
set changes every three seconds where in one moment it contains a valid IP address pointing to a an
arbitrary endpoint and after 3 seconds contains an empty address set.

Finagle seems to create a new load balancer for each updated address set. The bug seems to be that
when the app transitions from an empty address set to a valid address set, it is evident that the 
old load balancer with the empty address is switched to `Closed`, however, the balancer is never
unregistered from the `BalancerRegistry`

To run this project:
```bash
sbt run 

# Then in another terminal window
open localhost:8888
```

Every 3 seconds, refresh the page to observe the content of the balancer registry. You should see
the json list continue to grow unbounded.

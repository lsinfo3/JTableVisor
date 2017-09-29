from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.net import Mininet
from mininet.node import RemoteController


def myNetwork():
    net = Mininet(topo=None, build=False)

    # Add hosts and switches
    host1 = net.addHost("h1", mac="00:00:10:00:00:11")
    host2 = net.addHost("h2", mac="00:00:10:00:00:22")
    switch_h1 = net.addSwitch("s_h1", protocols="OpenFlow13", dpid="00:00:00:00:00:01")
    switch_h2 = net.addSwitch("s_h2", protocols="OpenFlow13", dpid="00:00:00:00:00:02")
    # Pipeline switches
    switch_p1 = net.addSwitch("s_p1", protocols="OpenFlow13", dpid="00:00:00:00:00:03")
    switch_p2 = net.addSwitch("s_p2", protocols="OpenFlow13", dpid="00:00:00:00:00:04")

    # Add links (hosts towards pipeline)
    net.addLink(host1, switch_h1)
    net.addLink(host2, switch_h2)
    net.addLink(switch_h1, switch_p1)
    net.addLink(switch_h2, switch_p1)
    # Pipeline internal links (two links between the stages)
    net.addLink(switch_p1, switch_p2)
    net.addLink(switch_p1, switch_p2)

    # Add two controllers:
    # - c_onos is connected directly to ONOS (6653)
    # - c_tv is connected to our TableVisor instance (6654)
    c_onos = net.addController("c1", controller=RemoteController, ip="127.0.0.1", port=6653)
    c_tv = net.addController("c2", controller=RemoteController, ip="127.0.0.1", port=6654)

    net.build()

    switch_h1.start( [c_onos] )
    switch_h2.start( [c_onos] )
    switch_p1.start( [c_tv] )
    switch_p2.start( [c_tv] )

    CLI( net )
    net.stop()



if __name__ == "__main__":
    setLogLevel("info")
    myNetwork()

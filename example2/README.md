# Example 2: Label Edge Router using P4 Hardware

## Summary: What do we want to do

ACL, remove MPLS header, set eth_dst, forward packet

## P4 Program (including annotations)

The used P4 programm is located in [example2/mplsProgram.p4](mplsProgram.p4).
Here, the annotations are particularly important for TableVisor.
For example, note the `table` and `field` annotations, as seen in the `acl_tbl`:

```P4
// @TV table 0
table acl_tbl {
    reads {
        // @TV field eth_type
        eth.etype : exact;
        // @TV field eth_dst
        eth.dst : exact;
    }
    // [...] (actions)
}
```

Hereby, the table ID `0` is mapped to the name `acl_tbl` when used with the RTECLI tool, and the OpenFlow field `ETH_TYPE` is translated to the P4 field `eth.etype`.

In addition, multiple OpenFlow actions must be mapped to a single P4 action, as P4 only supports one action per match.
In the following section:

```P4
// @TV action MPLS_POP   POP_ETHERTYPE=etype
// @TV action GOTO_TABLE_2
action mpls_pop(etype) {
    remove_header(mpls);
    modify_field(eth.etype, etype);
}
```

... the `mpls_pop` P4-action is linked to the `MPLS_POP` and `GOTO_TABLE` instructions in OpenFlow.
Note that P4 implicitly executes the `GOTO_TABLE` instruction towards the next table in its control flow.

## Setup and Startup

#### Adjustments (IP and CLI path)

**You may skip this section if everything (TableVisor, ONOS and the P4 card) is running on the same machine.**

In the file [example2/TVconfig.yml](TVconfig.yml):

```YAML
# Connection from TV to the Controller
upperLayerEndpoints:
  - name: MyConnectionToOnos
    type: OPENFLOW            # upperlayer.UpperLayerType
    ip: 127.0.0.1
    port: 6653
    reconnectInterval: 5000   # default: 10000 ms
```

... adjust the `ip` such that TableVisor is able to connect to the ONOS instance.

In the switch section:

```YAML
# Connection from TV to the Switches
lowerLayerEndpoints:
  - name: P4Switches
    type: P4_NETRONOME
    rtecliPath: /opt/nfp-sdk-6.0.1/p4/bin/rtecli
    switches:
      - dataplaneId: 100
        rteIp: 172.16.35.11   # You probably need to change this!
        rtePort: 20206
        numberOfPorts: 2
        [...]
```

... check the `rtecliPath` and the `rteIp` to point towards the CLI binary and your Netronome P4 host.

#### ONOS

Assuming ONOS was unpacked into the folder `./onos-1.11.1`, execute the following command:

```Shell
./onos-1.11.1/bin/onos-service start
```

ONOS should initialize and present its own terminal.
If no errors occur, you can access its web interface via `http://127.0.0.1:8181/onos/ui/login.html` to test whether it's running properly.
Log in with the user `onos` and password `rocks` and open the Applications settings (click on the three bars in the top left).
Here, only enable the **OpenFlow Base Provider** _(org.onosproject.openflow-base)_ application.
Therefor, select it and click on the Play-Button in the top right of the screen.

(Make sure that the applications from _example1_ are disabled at this point! In order to reset the flow rules afterwards, type `wipe-out please` in the ONOS terminal.)

#### TableVisor

When located in the main repository folder, after compiling TableVisor via `mvn package`, run the following command:

```Shell
java -jar target/tablevisor-standalone-3.0.0-SNAPSHOT-jar-with-dependencies.jar example2/TVconfig.yml
```

The output should look as follows:

```
2017-10-09 14:48:29,951 [main] INFO  - Using configuration file /home/alex/w/17/tablevisor-github/example2/TVconfig.yml
2017-10-09 14:48:30,043 [main] INFO  - Starting TableVisor
2017-10-09 14:48:30,044 [main] INFO  - Initializing ControllerLogApplication...
2017-10-09 14:48:30,044 [main] INFO  - Initializing OneTransparentSwitchApplication...
2017-10-09 14:48:30,044 [main] INFO  - Initializing P4ControlApplication...
2017-10-09 14:48:30,141 [main] INFO  - Initializing SwitchLogApplication...
2017-10-09 14:48:30,144 [main] INFO  - Waiting for all (1) switches to connect...
2017-10-09 14:48:30,147 [main] INFO  - Switch ID '100' of type 'P4_NETRONOME' registered
2017-10-09 14:48:30,147 [main] INFO  - All switches connected.
2017-10-09 14:48:30,153 [Thread-1] INFO  - MyConnectionToOnos - Attempting connection to control plane 127.0.0.1:6653
[...]
```

#### Flow Rules

The following flow rules are going to be installed in this simple scenario:

| Table             | Match                                           | Actions                                        |
| ----------------- | ----------------------------------------------- | ---------------------------------------------- |
| acl_tbl (`0`)     | `ETH_TYPE=MPLS` and `ETH_DST=00:00:00:00:00:01` | `GOTO_TABLE 1`                                 |
| mpls_tbl (`1`)    | (empty)                                         | `MPLS_POP` and `GOTO_TABLE 2`                  |
| routing_tbl (`2`) | `ETH_TYPE=IPv4` and `IPv4_DST=10.100.100.10`    | `ETH_DST=00:00:00:00:00:02` and `GOTO_TABLE 3` |
| switch_tbl (`3`)  | `ETH_DST=00:00:00:00:00:02`                     | `OUTPUT PORT=1`                                |

Again, while located in the main folder, execute the following command to install the flow rules:

```Shell
curl -u onos:rocks -H 'Content-type: application/json' -X POST -d '@example2/onosRules.json' 'http://127.0.0.1:8181/onos/v1/flows'
```

Note that you may have to adjust the IP address towards ONOS again.

You can now observe that ONOS installs the rules on the device (in the Devices section, select `of:0102030405060708` and click on the flow view icon in the top right).
However, note that P4 does not implement default packet and byte counters, hence their information is omitted here.

You may check the rules on the P4 card, e.g., via the RTECLI interface:

```Shell
rtecli --rte-port 20206 --rte-host 172.16.35.11 tables --table-name routing_tbl list-rules
```

The result should look as follows:

```Shell
[TableEntry(priority=15, rule_name='r008500002447597e', default_rule=False, actions='{  "type" : "set_dst_mac",  "data" : { "mac" : { "value" : "00:00:00:00:00:02" } } }', match='{ "eth.etype" : {  "value" : "0x800" }, "ipv4.dstAddr" : {  "value" : "10.100.100.10" } }')]
```

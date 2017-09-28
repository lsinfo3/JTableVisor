# Configuration

The configuration file of TableVisor is written in [YAML](http://yaml.org/). A full example is provided in [src/main/resources/config.yml](src/main/resources/config.yml).
It is comprised of four main components:
* ourDatapathId _(The datapath ID that the controller sees for our emulated switch.)_
* applications
* upperLayerEndpoints
* lowerLayerEndpoints

## Applications

The applications section defines the functionality of TableVisor.
Here, different apps can be enabled or disabled, effectively adding or removing capabilities of the proxy layer.
The following apps are currently available:

```YAML
applications:
  - ControllerLogApplication
  - SwitchLogApplication
  - MultiSwitchApplication
  #- OneTransparentSwitchApplication
  #- P4ControlApplication
```

Note that those apps that are commented out with a hash symbol `#` are disabled.

### ControllerLogApplication and SwitchLogApplication
These apps should always be enabled, as they are responsible for the basic communication information on both ends of TableVisor.
They display the types of exchanged OpenFlow messages between TableVisor and the controller, and between TableVisor and all of the data plane devices, respectively.

### MultiSwitchApplication and OneTransparentSwitchApplication
Only one of these apps should be enabled at the same time, as they operate in the same domain.
The simpler one, the `OneTransparentSwitchApplication`, uses TableVisor as a transparent proxy and simply forwards all OpenFlow messages of the controller to the first dataplane device and vice versa, without modifying them.
The `MultiSwitchApplication` implements the *Staged Pipeline* scenario as described in the paper. Thereby, OpenFlow messages are distributed towards all dataplane devices as necessary.
They are also modified to allow the abstraction of TableVisor as a single switch, for instance by turning `GOTO_TABLE` instructions into `OUTPUT` actions and installing additional rules, hidden to the controller.

### P4ControlApplication
Finally, the `P4ControlApplication` enables the inclusion of non-OpenFlow devices (namely the Netronome Agilio CX P4 SmartNICs) into the pipeline.
OpenFlow messages from the controller are either answered by TableVisor directly (such as SwitchFeatures), or translated into a dedicated JSON format and handed to the cards' command line tool (RTECLI).
Thereby, the OpenFlow SDN controller can install flow rules on these non-OpenFlow-compliant devices.
It should be enabled together with one of the above apps (`MultiSwitchApplication` or `OneTransparentSwitchApplication`) if their application is desired.

## Upper-Layer Endpoints

This section defines parameters of the connection between TableVisor and the SDN controller.

```YAML
# Connection from TV to the Controller
upperLayerEndpoints:
  - name: OnosEndpoint
    type: OPENFLOW
    ip: 127.0.0.1
    port: 6653
    reconnectInterval: 5000
```

In theory, multiple upper-layer endpoints may be specified here, but for our considered use cases, only one is required.
Thereby, each endpoints has its own `name` to identify them in the log messages.
The `type` attribute indicates the protocol used for the communication. Currently, only `OPENFLOW` is available here.
The `ip` and `port` fields should point towards the controller for connection establishment.
Finally, the `reconnectInterval` delays new connection attempts after connection failures (in milliseconds).

## Lower-Layer Endpoints

This final section describes the properties of the underlying data plane devices.

```YAML
# Connection from TV to the Switches
lowerLayerEndpoints:
  - name: OpenFlowSwitches
    type: OPENFLOW
    port: 6654
    switches:
      - dataplaneId: 1
        datapathId: 00:00:00:00:00:00:00:01
        tableMap:
            # [Our TableID towards Controller] : [TableID of Switch]
            0: 0
            4: 0
        portMap:
            #  [Local Port of Device] : [Our DataplaneID]
            3: 2
            4: 2
      - dataplaneId: 2
        datapathId: 00:00:00:00:00:00:00:02
        tableMap:
            # [Our TableID towards Controller] : [TableID of Switch]
            1: 0
            2: 1
        portMap:
            #  [Local Port of Device] : [Our DataplaneID]
            1: 1
            2: 1
            3: 100
            4: 100
  - name: P4Switches
    type: P4_NETRONOME
    rtecliPath: /opt/nfp-sdk-6.0.1/p4/bin/rtecli
    switches:
      - dataplaneId: 100
        rteIp: 127.0.0.1
        rtePort: 20206
        numberOfPorts: 2
        tableSpecs:
          # List all files with relevant P4 table specs here
          - path/to/my_p4_program.p4
        tableMap:
            # [Our TableID towards Controller] : [TableID of Switch]
            3: 0
        portMap:
            # [Local Port of Device] : [Our DataplaneID]
            1: 2
            2: 2
```

Similarly to upper-layer endpoints, each lower-layer endpoint has a name for identification purposes.
Below each endpoint, multiple switches can be defined.
Currently, two `type`s of endpoints are recognized here: `OPENFLOW` endpoints and `P4_NETRONOME` enpoints.

For the `OPENFLOW` type, a `port` must be specified. TableVisor will listen for incoming connections on this port and pretend to be an SDN controller.
All switches below this endpoint must be configured to connect to TableVisor through this port.

The `switches` are enumerated below.
Each switch has a `dataplaneId`, used for internal identification in TableVisor, and a `datapathId`, used by the switch to identify itself.
In addition, a `tableMap` and a `portMap` must be provided so TableVisor knows about the internal topology of the emulated switch.

The `tableMap` is used by the `MultiSwitchApplication` to map global table IDs (as the controller sees them) to actual, physical tables on the switch.
There are some circumstances to be respected here.
First, the physical ID `0` of every switch should be included here.
Second, keep in mind that OpenFlow only allows `GOTO_TABLE` instructions towards higher table IDs than the current. Hence, the table IDs of switches that appear later in the pipeline should be higher than those in the beginning.
Finally, for the same reason, table ID `0` of the first switch in the pipeline should be mapped twice. It should correspond to both the global ID `0` and the highest used ID (`4` in the above example) to allow packets to be sent back to the first switch by `GOTO_TABLE` instructions from the controller.

The `portMap` describes how the switches are wired together physically. The number of each port that is connected to another switch in the pipeline is mapped to the other switch's `dataplaneId`.

### P4 Endpoint Configuration

For `P4_NETRONOME` types, there is no `port` attribute required in the endpoint as there is no consistent connection between the controller and the NICs.
However, the path towards their command line tool must be provided in `rtecliPath`.
Each switch of this type holds their own `rteIp` and `rtePort` so TableVisor can pass messages to them.
In addition, the total `numberOfPorts` is required, so TableVisor can reply to the controller's SwitchStatsRequests accordingly.

Finally, P4-switches must have a list of `.p4` source files in the `tableSpecs` attribute containing their configuration with dedicated `@TV` annotations in it.
These are used to translate table IDs, header field names and actions between the OpenFlow protocol and the respective P4 program.
For more information on the required annotations, see [P4 Table Specs](P4TABLESPECS.md).
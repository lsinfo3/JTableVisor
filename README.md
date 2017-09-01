# JTableVisor

**This is the continued TableVisor project. The discontinued project (written in Erlang) can be found [here](https://github.com/lsinfo3/TableVisor).**

# Abstract

Software Defined Networking aims to separate the control and data plane in networks by moving the control logic from network devices into a logically centralized controller. Using a well-defined, unified protocol, such as OpenFlow, the controller is able to configure the forwarding behavior of data plane devices. Here, the OpenFlow protocol is translated to vendor- and device-specific instructions that, for instance, manipulate the flow table entries in the device.
  
In reality, SDN-enabled switches often feature different hardware capabilities and configurations with respect to the number of flow tables, how they are implemented, and which kind of data plane features they support. Furthermore, most devices only support a subset of the OpenFlow specification. This leads to an ever growing device heterogeneity within the SDN landscape. Accordingly, developers either have to restrict control plane applications to a set of well known devices or provide feature limited solutions for a broad spectrum of devices.

To overcome this challenge we propose TableVisor, a transparent proxy-layer for the OpenFlow control channel.
It enables a flexible and scalable abstraction of multiple data plane devices into one emulated data plane switch, meeting the requirements of the control plane applications. 
Therefor, TableVisor registers with the SDN controller as a single switch with use-case specific capabilities. It translates instructions and rules from control applications towards the appropriate data plane device where they are executed.

# Installation

JTableVisor uses [maven](https://maven.apache.org/) to build an executable `.jar` file.

Executing `mvn package` will create the binary in `./target/`.

# Configuration

TBD

# Authors
Alexej Grigorjew - <alexej.grigorjew@informatik.uni-wuerzburg.de>  
Stefan Geissler - <stefan.geissler@informatik.uni-wuerzburg.de>  
Stefan Herrnleben - <stefan.herrnleben@informatik.uni-wuerzburg.de>  
Thomas Zinner - <zinner@informatik.uni-wuerzburg.de>

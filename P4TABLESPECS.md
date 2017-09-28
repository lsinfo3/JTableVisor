# P4 Table Specs

This page describes the annotations required in P4 source files to enable the translation of OpenFlow FlowMod messages into P4 CLI calls.
Annotations are single-line comments, beginning with `@TV` and located directly above the annotated line.
Example:

```P4
// @TV table 0
table acl_tbl {
    reads {
        ...
```

There are three types of annotations available.

(1) `table`: This annotation creates a mapping between the P4 table name from the source code and the provided ID in the annotation.
An example of this type is provided above.

(2) `field` annotations are used to tell TableVisor which OpenFlow-Match-Fields correspond to which P4-Match-Fields.
This annotation is to be used inside the `reads` block of `table` declarations.
Example:

```P4
// @TV table 2
table routing_tbl {
    reads {
        // @TV field in_port
        standard_metadata.ingress_port: exact;
        eth.etype : exact;
        // @TV field ipv4_dst
        ipv4.dstAddr : exact;      
    }
    actions {
        set_dst_mac;
    }
}
```

Each field must only be annotated once, even if it is read by multiple tables. The mapping is done in a global scope.

(3) Finally, `action` annotations realize the mapping between (possibly multiple) OpenFlow actions and a single P4 action.
Additionally, they also include the mappings of the respective parameters.
Example:

```P4
// @TV action SET_FIELD_ETH_DST   ETH_DST=mac
// @TV action GOTO_TABLE_123
action set_dst_mac(mac) {
    modify_field(eth.dst, mac);
}
```

Here, the P4 action `set_dst_mac` is mapped to the two OpenFlow actions/instructions `SET_FIELD` and `GOTO_TABLE`.
Note that these two OpenFlow actions are special, as they also contain the modified field (`ETH_DST`) and the destination table (`123`) as a part of their name, not as a parameter.
In addition, the OpenFlow field `ETH_DST` is mapped to the P4 parameter `mac`.
This also happens in a global scope, therefore, all parameter names should be unique in the P4 program.

A full example mapping can be found in the 2nd example use case (**TODO**).
//////////////////////////////////////
////////// PARSER VALUE SETS /////////
//////////////////////////////////////

parser_value_set pvs_ipv4;


//////////////////////////////////////
//////////       HEADERS     /////////
//////////////////////////////////////

header_type eth_hdr {
    fields {
        dst : 48;
        src : 48;
        etype : 16;
    }
}

header_type mpls_hdr {
    fields {
        label: 20;
        traffic_class: 3;
        bottom_of_stack: 1;
        ttl: 8;
    }
}

header_type ipv4_hdr {
    fields {
        version : 4;
        ihl : 4;
        diffserv : 8;
        totalLen : 16;
        identification : 16;
        flags : 3;
        fragOffset : 13;
        ttl : 8;
        protocol : 8;
        hdrChecksum : 16;
        srcAddr : 32;
        dstAddr: 32;
    }
}

header_type tcp_hdr {
    fields {
        srcPort : 16;
        dstPort : 16;
        seqNo : 32;
        ackNo : 32;
        dataOffset : 4;
        res : 4;
        flags : 8;
        window : 16;
        checksum : 16;
        urgentPtr : 16;
    }
}

header_type udp_hdr {
    fields {
        srcPort : 16;
        dstPort : 16;
        length_ : 16;
        checksum : 16;
    }
}

header eth_hdr eth;
header mpls_hdr mpls;
header ipv4_hdr ipv4;
header tcp_hdr tcp;
header udp_hdr udp;

//////////////////////////////////////
//////////       PARSING     /////////
//////////////////////////////////////

#define ETHERTYPE_IPV4 0x0800
#define ETHERTYPE_MPLS 0x8847
#define IP_PROT_TCP 0x06
#define IP_PROT_UDP 17

parser start {
    return eth_parse;
}

parser eth_parse {
    extract(eth);
    return select(latest.etype) {
        ETHERTYPE_IPV4 : ipv4_parse;
        ETHERTYPE_MPLS : mpls_parse;
        default: ingress;
    }
}

parser mpls_parse {
    extract(mpls);
    return select(mpls.label) {
        pvs_ipv4 : ipv4_parse;
        default: ingress;
    }
}

parser ipv4_parse {
    extract(ipv4);
    return select(ipv4.protocol) {
        IP_PROT_TCP : tcp_parse;
        IP_PROT_UDP : udp_parse;
        default : ingress;
    }
}

parser tcp_parse {
    extract(tcp);
    return ingress;
}

parser udp_parse {
    extract(udp);
    return ingress;
}

//////////////////////////////////////
//////////       ACTIONS     /////////
//////////////////////////////////////

// @TV action DROP
action drop_act() {
    drop();
}

// @TV action GOTO_TABLE_1
action no_op_act() {
    no_op();
}

// @TV action OUTPUT   OUT_PORT=prt
action fwd_act(prt) {
    modify_field(standard_metadata.egress_spec, prt);
}

// @TV action MPLS_POP   POP_ETHERTYPE=etype
// @TV action GOTO_TABLE_2
action mpls_pop(etype) {
    remove_header(mpls);
    modify_field(eth.etype, etype);
}

// @TV action SET_FIELD_ETH_DST   ETH_DST=mac
// @TV action GOTO_TABLE_3
action set_dst_mac(mac) {
    modify_field(eth.dst, mac);
}

//////////////////////////////////////
//////////       TABLES      /////////
//////////////////////////////////////

// @TV table 0
table acl_tbl {
    reads {
        // @TV field eth_type
        eth.etype : exact;
        // @TV field eth_dst
        eth.dst : exact;
    }
    actions {
        drop_act;
        no_op_act;
    }
}

// @TV table 1
table mpls_tbl {
    actions {
        mpls_pop;
    }
}

// @TV table 2
table routing_tbl {
    reads {
        eth.etype : exact;
        // @TV field ipv4_dst
        ipv4.dstAddr : exact;
    }
    actions {
        set_dst_mac;
    }
}

// @TV table 3
table switch_tbl {
    reads {
        eth.dst : exact;
    }
    actions {
        fwd_act;
    }
}

//////////////////////////////////////
//////////       PIPELINE    /////////
//////////////////////////////////////

control ingress {
    apply(acl_tbl);
    apply(mpls_tbl);
    apply(routing_tbl);
    apply(switch_tbl);
}

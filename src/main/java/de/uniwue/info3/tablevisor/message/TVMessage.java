package de.uniwue.info3.tablevisor.message;

import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFMessage;

public class TVMessage {
	private TVMsgProtocol protocol;
	private OFMessage ofMessage;
	private int dataplaneId = -1;
	private String errorMsg;
	private OFParseError e;

	public TVMessage(TVMessage copy) {
		this.protocol = copy.protocol;
		this.ofMessage = copy.ofMessage.createBuilder().build();
		this.dataplaneId = copy.dataplaneId;
	}

	public TVMessage(OFMessage ofMessage) {
		this.ofMessage = ofMessage;
		this.protocol = TVMsgProtocol.OPENFLOW;
	}

	public TVMessage(OFMessage ofMessage, int dataplaneId) {
		this(ofMessage);
		this.dataplaneId = dataplaneId;
	}

	public TVMessage(OFParseError e, String errorMsg) {
		this.e = e;
		this.errorMsg = errorMsg;
		this.protocol = TVMsgProtocol.ERROR;
	}

	public <T extends OFMessage> T getOFMessage() {
		return (T) ofMessage;
	}

	public void setOFMessage(OFMessage ofMessage) {
		if (!isOpenFlow()) {
			throw new IllegalStateException("This TVMessage object does not represent an OFMessage; protocol="+protocol);
		}
		this.ofMessage = ofMessage;
	}

	public TVMsgProtocol getProtocol() {
		return protocol;
	}

	public boolean isOpenFlow() {
		return protocol == TVMsgProtocol.OPENFLOW;
	}

	public boolean isP4() {
		return protocol == TVMsgProtocol.P4_NETRONOME;
	}

	public boolean isError() {
		return protocol == TVMsgProtocol.ERROR;
	}

	public int getDataplaneId() {
		return dataplaneId;
	}

	public void setDataplaneId(int dataplaneId) {
		this.dataplaneId = dataplaneId;
	}

	public TVMessage copy() {
		return new TVMessage(this);
	}

	public TVMessage copyWithDpId(int newDataplaneId) {
		TVMessage newMsg = copy();
		newMsg.setDataplaneId(newDataplaneId);
		return newMsg;
	}

	public String getTypeAsString() {
		if (isOpenFlow()) {
			return ofMessage.getType().toString();
		}
		else if (isError()) {
			return "OF_PARSE_ERROR";
		}
		return "UNKNOWN";
	}
}

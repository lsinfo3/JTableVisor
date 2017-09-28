package de.uniwue.info3.tablevisor.message;

import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;

import java.util.LinkedList;

public class TVMessage {
	// used by all types
	private TVMsgProtocol protocol;
	private String errorMsg;
	private OFParseError e;
	private int dataplaneId = -1;

	// used by openflow types
	private OFMessage ofMessage;

	// used by p4 types
	private LinkedList<String> cmdLine;
	private OFType thisMsgType;
	private TVMessage originalRequest;
	private String reply;

	public TVMessage(TVMessage copy) {
		this.protocol = copy.protocol;
		this.errorMsg = copy.errorMsg;
		this.e = copy.e;
		this.dataplaneId = copy.dataplaneId;
		this.ofMessage = copy.ofMessage.createBuilder().build();
		if (cmdLine != null) this.cmdLine = new LinkedList<>(copy.cmdLine);
		this.thisMsgType = copy.thisMsgType;
		this.originalRequest = copy.originalRequest;
		this.reply = copy.reply;
	}

	/**
	 * OpenFlow-type constructor
	 */
	public TVMessage(OFMessage ofMessage) {
		this.ofMessage = ofMessage;
		this.protocol = TVMsgProtocol.OPENFLOW;
	}

	/**
	 * OpenFlow-type constructor
	 */
	public TVMessage(OFMessage ofMessage, int dataplaneId) {
		this(ofMessage);
		this.dataplaneId = dataplaneId;
	}

	/**
	 * P4-type request constructor
	 */
	public TVMessage(LinkedList<String> cmdLine, TVMessage originalRequest) {
		this.cmdLine = cmdLine;
		this.originalRequest = originalRequest;
		this.protocol = TVMsgProtocol.P4_NETRONOME;
	}

	/**
	 * P4-type request constructor
	 */
	public TVMessage(LinkedList<String> cmdLine, int dataplaneId, TVMessage originalRequest) {
		this(cmdLine, originalRequest);
		this.dataplaneId = dataplaneId;
	}

	/**
	 * P4-type reply constructor
	 */
	public TVMessage(String reply, TVMessage originalRequest) {
		this.reply = reply;
		this.originalRequest = originalRequest;
		this.protocol = TVMsgProtocol.P4_NETRONOME;
	}

	/**
	 * P4-type reply constructor
	 */
	public TVMessage(String reply, TVMessage originalRequest, int dataplaneId) {
		this(reply, originalRequest);
		this.dataplaneId = dataplaneId;
	}

	/**
	 * Error-type constructor
	 */
	public TVMessage(OFParseError e, String errorMsg) {
		this.e = e;
		this.errorMsg = errorMsg;
		this.protocol = TVMsgProtocol.ERROR;
	}

	public <T extends OFMessage> T getOFMessage() {
		if (!isOpenFlow()) {
			throw new IllegalStateException("This TVMessage object does not represent an OFMessage; protocol="+protocol);
		}
		return (T) ofMessage;
	}

	public void setOFMessage(OFMessage ofMessage) {
		if (!isOpenFlow()) {
			throw new IllegalStateException("This TVMessage object does not represent an OFMessage; protocol="+protocol);
		}
		this.ofMessage = ofMessage;
	}

	public LinkedList<String> getCmdLine() {
		if (!isP4()) {
			throw new IllegalStateException("This TVMessage object does not represent a P4 message; protocol="+protocol);
		}
		return cmdLine;
	}

	public void setCmdLine(LinkedList<String> cmdLine) {
		if (!isP4()) {
			throw new IllegalStateException("This TVMessage object does not represent a P4 message; protocol="+protocol);
		}
		this.cmdLine = cmdLine;
	}

	public TVMsgProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(TVMsgProtocol protocol) {
		this.protocol = protocol;
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

	public TVMessage getOriginalRequest() {
		return originalRequest;
	}

	public void setOriginalRequest(TVMessage originalRequest) {
		this.originalRequest = originalRequest;
	}

	public OFType getThisMsgType() {
		return (thisMsgType != null ? thisMsgType : (isOpenFlow() ? ofMessage.getType() : null));
	}

	public void setThisMsgType(OFType thisMsgType) {
		this.thisMsgType = thisMsgType;
	}

	public String getReply() {
		return reply;
	}

	public void setReply(String reply) {
		this.reply = reply;
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
		else if (isP4()) {
			String type = "unknown";
			if (thisMsgType != null) {
				return "P4_NETRONOME RTECLI ("+thisMsgType+")";
			}
			if (reply != null && reply.contains("TableEntry")) {
				return "P4_NETRONOME RTECLI (TableEntry)";
			}
			if (cmdLine != null) for (String s : cmdLine) {
				switch (s) {
					case "add":
					case "edit":
					case "delete":
					case "list-rules":
						type = s;
						break;
					default:
						break;
				}
			}
			return "P4_NETRONOME RTECLI ("+type+")";
		}
		else if (isError()) {
			return "OF_PARSE_ERROR";
		}
		return "UNKNOWN";
	}
}

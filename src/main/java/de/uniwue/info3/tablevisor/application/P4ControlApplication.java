package de.uniwue.info3.tablevisor.application;

import de.uniwue.info3.tablevisor.config.P4Dict;
import de.uniwue.info3.tablevisor.config.SwitchConfig;
import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionPopMpls;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.*;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class P4ControlApplication extends BaseP4Application {
	private static final OFFactory fac = OFFactories.getFactory(OFVersion.OF_13);
	private static final TableVisor TV = TableVisor.getInstance();
	private static final Logger logger = LogManager.getLogger();

	private static final Pattern pTableEntry = Pattern.compile("TableEntry\\(priority=(\\d+), rule_name='([^']+)', default_rule=(True|False), actions='\\{ ([^']*) \\}', match='\\{ ([^']*) \\}'");
	private static final Pattern pMatch = Pattern.compile("\"([^\"]+)\" : \\{  \"value\" : \"([^\"]+)\" \\}");
	private static final Pattern pAction = Pattern.compile("\"type\" : \"([^\"]+)\",  \"data\" : \\{ (.*) \\}");
	private static final Pattern pData = Pattern.compile("\"([^\"]+)\" : \\{ \"value\" : \"([^\"]+)\" \\}");

	private HashMap<Integer, LinkedList<TVMessage>> cachedStatsReplies = new HashMap<>();

	public P4ControlApplication(IApplication controlPlaneConnector) {
		super(controlPlaneConnector);
	}

	@Override
	public void p4ToControlPlane(TVMessage tvMessage) {
		if (tvMessage.getOriginalRequest() != null) {
			OFType replyType = null;
			switch (tvMessage.getOriginalRequest().getThisMsgType()) {
				case STATS_REQUEST:
					replyType = OFType.STATS_REPLY;
					break;
				default:
					logger.warn("{} - No reply type specified for original request '{}'", getClass().getSimpleName(), tvMessage.getOriginalRequest().getThisMsgType());
					break;
			}

			if (replyType != null) {
				tvMessage.setThisMsgType(replyType);
				super.allToControlPlane(tvMessage);
			}
		}
	}

	@Override
	public void switchFeaturesToDataPlane(TVMessage tvMessage) {
		OFFeaturesReply rep = fac.buildFeaturesReply()
				.setDatapathId(DatapathId.of(TV.getConfig().ourDatapathId))
				.setNBuffers(1)
				.setNTables((short) TV.getConfig().getSwitchConfigById(tvMessage.getDataplaneId()).tableMap.size())
				.setAuxiliaryId(OFAuxId.MAIN)
				.setCapabilities(Collections.emptySet())
				.setReserved(0)
				.setXid(tvMessage.getOFMessage().getXid())
				.build();
		TVMessage msg = new TVMessage(rep, tvMessage.getDataplaneId());
		getSuccessingControlPlaneConnector().allToControlPlane(msg);
	}

	@Override
	public void switchStatsToDataPlane(TVMessage tvMessage) {
		SwitchConfig swc = TV.getConfig().getSwitchConfigById(tvMessage.getDataplaneId());
		OFStatsRequest req = tvMessage.getOFMessage();
		OFMessage resp = null;

		switch (req.getStatsType()) {
			case PORT_DESC:
				ArrayList<OFPortDesc> ports = new ArrayList<>();
				HashSet<OFPortFeatures> features = new HashSet<>();
				features.add(OFPortFeatures.PF_10GB_FD);
				features.add(OFPortFeatures.PF_COPPER);

				for (int i = 0; i < swc.numberOfPorts - swc.portMap.size(); i++) {
					ports.add(fac.buildPortDesc()
							.setPortNo(OFPort.of(i))
							.setName(tvMessage.getDataplaneId() + "_" + i)
							.setHwAddr(MacAddress.of(String.format("%02X:00:00:00:00:%02X", tvMessage.getDataplaneId(), i)))
							.setCurrSpeed(10000000)
							.setMaxSpeed(0)
							.setCurr(features)
							.build());
				}
				resp = fac.buildPortDescStatsReply()
						.setEntries(ports)
						.setXid(tvMessage.getOFMessage().getXid())
						.build();
				break;

			case METER_FEATURES:
				OFMeterFeatures meterFeatures = fac.buildMeterFeatures()
						.build();
				resp = fac.buildMeterFeaturesStatsReply()
						.setFeatures(meterFeatures)
						.setXid(tvMessage.getOFMessage().getXid())
						.build();
				break;

			case DESC:
				resp = fac.buildDescStatsReply()
						.setHwDesc("P4_Netronome")
						.setMfrDesc("TableVisor")
						.setSwDesc(TableVisor.VERSION)
						.setXid(tvMessage.getOFMessage().getXid())
						.build();
				break;

			case FLOW:
				OFFlowStatsRequest flowStatsReq = (OFFlowStatsRequest) req;
				int[] tableIds;
				if (flowStatsReq.getTableId().equals(TableId.ALL)) {
					tableIds = swc.tableMap.values().stream().mapToInt(Integer::intValue).toArray();
				}
				else {
					tableIds = new int[]{(int) flowStatsReq.getTableId().getValue()};
				}

				LinkedList<TVMessage> cache = cachedStatsReplies.get(swc.dataplaneId);
				if (cache != null && !cache.isEmpty()) {
					logger.warn("Cached Flow Stats replies cleared by subsequent FlowStats-Request");
				}
				cachedStatsReplies.put(swc.dataplaneId, new LinkedList<>());

				for (int t : tableIds) {
					TVMessage tvCopy = tvMessage.copy();
					tvCopy.setOFMessage(flowStatsReq.createBuilder().setTableId(TableId.of(t)).build());

					LinkedList<String> cliParams = new LinkedList<>();
					cliParams.add("tables");
					cliParams.add("--table-name " + swc.getP4Dict().tableIdToP4Name(t));
					cliParams.add("list-rules");
					TVMessage call = new TVMessage(cliParams, tvMessage.getDataplaneId(), tvCopy);
					super.allToDataPlane(call);
				}
				return;
		}

		if (resp != null) {
			TVMessage msg = new TVMessage(resp, tvMessage.getDataplaneId());
			getSuccessingControlPlaneConnector().allToControlPlane(msg);
		}
		else {
			logger.warn("No reply constructed for STATS_REQUEST '{}'", req.getStatsType());
		}
	}

	@Override
	public void switchStatsToControlPlane(TVMessage tvMessage) {
		SwitchConfig swc = TV.getConfig().getSwitchConfigById(tvMessage.getDataplaneId());

		OFMessage orig = tvMessage.getOriginalRequest().getOFMessage();
		if (orig.getType() != OFType.STATS_REQUEST) {
			logger.error("{} - Original message for switch-stats-reply is not switch-stats-request but '{}'", getClass().getSimpleName(), orig.getType());
			return;
		}
		OFStatsRequest origReq = (OFStatsRequest) orig;
		TVMessage newMsg;
		switch (origReq.getStatsType()) {
			case FLOW:
				LinkedList<TVMessage> cache = cachedStatsReplies.get(swc.dataplaneId);
				cache.add(tvMessage);
				if (cache.size() < swc.tableMap.size()) {
					logger.trace("Cached {} of {} flow stats replies.", cache.size(), swc.tableMap.size());
					return;
				}
				cachedStatsReplies.remove(swc.dataplaneId);
				List<OFFlowStatsEntry> entries = new LinkedList<>();
				for (TVMessage msg : cache) {

					OFFlowStatsRequest origFlowReq = msg.getOriginalRequest().getOFMessage();
					int tableId = (int) origFlowReq.getTableId().getValue();

					String replyS = msg.getReply().trim();
					if (replyS.startsWith("[") && replyS.endsWith(")]")) {
						replyS = replyS.substring(1, replyS.length() - 2);
					}

					String[] replyParts = replyS.split("\\),");
					for (String part : replyParts) {
						part = part.trim();
						if (part.equals("[]")) continue;

						Matcher mTableEntry = pTableEntry.matcher(part);
						if (!mTableEntry.matches()) {
							logger.error("{} - The given TableEntry does not match the required pattern: {}", getClass().getSimpleName(), part);
							return;
						}
						int priority = Integer.parseInt(mTableEntry.group(1));
						String ruleName = mTableEntry.group(2);
						if (ruleName.startsWith("r")) ruleName = ruleName.substring(1);
						//String defaultRule = mTableEntry.group(3);
						String actions = mTableEntry.group(4);
						String match = mTableEntry.group(5);

						Match m = parseMatch(match, swc.getP4Dict());
						if (m == null) {
							logger.error("{} - Error parsing match: {}", getClass().getSimpleName(), match);
							return;
						}

						List<OFInstruction> insts = parseActions(actions, swc.getP4Dict());
						if (insts == null) {
							logger.error("{} - Error parsing action: {}", getClass().getSimpleName(), actions);
							return;
						}

						U64 cookie = U64.ZERO;
						try {
							cookie = U64.parseHex(ruleName);
						}
						catch (NumberFormatException e) {
							// Ignore. Use zero-cookie.
						}
						OFFlowStatsEntry entry = fac.buildFlowStatsEntry()
								.setTableId(TableId.of(tableId))
								.setPriority(priority)
								.setMatch(m)
								.setInstructions(insts)
								.setCookie(cookie)
								.build();
						entries.add(entry);
					}
				}
				OFFlowStatsReply rep = fac.buildFlowStatsReply()
						.setXid(orig.getXid())
						.setEntries(entries)
						.build();
				newMsg = new TVMessage(rep, tvMessage.getDataplaneId());
				break;

			default:
				logger.error("{} - Unsupported flow stats request type '{}'", getClass().getSimpleName(), origReq.getStatsType());
				return;
		}
		super.switchStatsToControlPlane(newMsg);
	}

	private List<OFInstruction> parseActions(String actions, P4Dict dict) {
		actions = actions.trim();
		Matcher mAction = pAction.matcher(actions);
		if (!mAction.matches()) {
			return null;
		}

		String type = mAction.group(1).trim();
		String data = mAction.group(2).trim();
		HashSet<String> ofInsts = dict.p4ActionToOfAction(type);
		if (ofInsts == null) {
			return null;
		}

		LinkedList<OFAction> retActions = new LinkedList<>();
		LinkedList<OFInstruction> retInsts = new LinkedList<>();

		HashMap<String, String> dataMap = new HashMap<>();
		String[] dataParts = data.split(",");
		for (String d : dataParts) {
			d = d.trim();
			if (d.isEmpty()) {
				continue;
			}

			Matcher mData = pData.matcher(d);
			if (!mData.matches()) {
				return null;
			}

			String name = dict.p4ParamToOfParam(mData.group(1));
			String val = mData.group(2);
			dataMap.put(name.toLowerCase(), val);
		}

		for (String inst : ofInsts) {
			inst = inst.toUpperCase();

			if (inst.startsWith("SET_FIELD_")) {
				String field = inst.substring(10);
				switch (field) {
					case "ETH_DST":
						retActions.add(fac.actions().setField(fac.oxms().ethDst(MacAddress.of(dataMap.get("eth_dst")))));
						break;
					default:
						logger.warn("Unknown SET_FIELD field: {}", field);
						break;
				}
			}
			else if (inst.startsWith("GOTO_TABLE_")) {
				String table = inst.substring(11);
				if (!table.matches("\\d+")) {
					logger.warn("Cannot parse table ID of {}", inst);
				}
				else {
					int tableId = Integer.parseInt(table);
					retInsts.add(fac.instructions().gotoTable(TableId.of(tableId)));
				}
			}
			else switch (inst) {
					case "DROP":
						if (ofInsts.size() != 1) {
							logger.warn("DROP action specified together with other actions");
						}
						break;

					case "OUTPUT":
						String portStr = dataMap.get("out_port");
						if (portStr.startsWith("p")) portStr = portStr.substring(1);
						if (portStr != null && !portStr.matches("\\d+")) {
							logger.warn("Cannot parse port number of instr. {}, param. PORT={}", inst, portStr);
							break;
						}
						int portNr = Integer.parseInt(portStr);
						retActions.add(fac.actions().output(OFPort.of(portNr), 0xffff));
						break;

					case "MPLS_POP":
						String etypeStr = dataMap.get("pop_ethertype");
						if (etypeStr == null) {
							logger.warn("Cannot parse EthType of instr. {}, param. POP_ETHERTYPE={}", inst, etypeStr);
							break;
						}
						if (etypeStr.startsWith("0x")) {
							etypeStr = etypeStr.substring(2);
						}
						try {
							int etype = Integer.parseInt(etypeStr, 16);
							retActions.add(fac.actions().popMpls(EthType.of(etype)));
						}
						catch (NumberFormatException e) {
							logger.warn("Cannot parse EthType of instr. {}, param. POP_ETHERTYPE={}", inst, etypeStr);
						}
						break;

					case "MPLS_PUSH":
						String ethertypeStr = dataMap.get("push_ethertype");
						if (ethertypeStr == null) {
							logger.warn("Cannot parse EthType of instr. {}, param. PUSH_ETHERTYPE={}", inst, ethertypeStr);
							break;
						}
						if (ethertypeStr.startsWith("0x")) {
							ethertypeStr = ethertypeStr.substring(2);
						}
						try {
							int etype = Integer.parseInt(ethertypeStr, 16);
							retActions.add(fac.actions().pushMpls(EthType.of(etype)));
						}
						catch (NumberFormatException e) {
							logger.warn("Cannot parse EthType of instr. {}, param. PUSH_ETHERTYPE={}", inst, ethertypeStr);
						}
						break;

					case "MPLS_LABEL":
						String labelStr = dataMap.get("label");
						try {
							long label = Long.parseLong(labelStr);
							retActions.add(fac.actions().setMplsLabel(label));
						}
						catch (NumberFormatException e) {
							logger.warn("Cannot parse Label of instr. {}, param. MPLS_LABEL={}", inst, labelStr);
						}
						break;

					default:
						logger.warn("Unknown OF-instruction of type {}", inst);
						break;
			}
		}

		if (!retActions.isEmpty()) {
			retInsts.add(fac.instructions().buildApplyActions().setActions(retActions).build());
		}
		return retInsts;
	}

	private Match parseMatch(String full, P4Dict dict) {
		Match.Builder builder = fac.buildMatch();
		for (String part : full.split(",")) {
			part = part.trim();
			if (part.isEmpty()) {
				continue;
			}

			Matcher mMatch = pMatch.matcher(part);
			if (!mMatch.matches()) {
				return null;
			}

			String field = mMatch.group(1);
			String val = mMatch.group(2);
			getOfMatchFromP4String(builder, field, val, dict);
		}
		return builder.build();
	}

	// Only exact matches supported for now
	private void getOfMatchFromP4String(Match.Builder builder, String sField, String sVal, P4Dict dict) {
		String ofField = dict.p4FieldToOfField(sField).toUpperCase();

		switch (ofField) {
			case "ETH_DST":
				builder.setExact(MatchField.ETH_DST, MacAddress.of(sVal));
				break;
			case "ETH_TYPE":
				if (sVal.startsWith("0x")) sVal = sVal.substring(2);
				int ethtype = Integer.parseInt(sVal, 16);
				builder.setExact(MatchField.ETH_TYPE, EthType.of(ethtype));
				break;
			case "IPV4_DST":
				builder.setExact(MatchField.IPV4_DST, IPv4Address.of(sVal));
				break;
			default:
				logger.warn("Unimplemented P4-Match -> Of-Match translation with P4-Field '{}', OF-Field '{}'", sField, ofField);
				break;
		}
	}

	@Override
	public void switchGetConfigToDataPlane(TVMessage tvMessage) {
		OFMessage resp = fac.buildGetConfigReply()
				.setMissSendLen(128)
				.setXid(tvMessage.getOFMessage().getXid())
				.build();
		getSuccessingControlPlaneConnector().allToControlPlane(new TVMessage(resp, tvMessage.getDataplaneId()));
	}

	@Override
	public void setConfigToDataPlane(TVMessage tvMessage) {
		logger.warn("SET_CONFIG discarded.");
	}

	@Override
	public void roleToDataPlane(TVMessage tvMessage) {
		OFMessage resp = fac.buildRoleReply()
				.setRole(OFControllerRole.ROLE_MASTER)
				.setXid(tvMessage.getOFMessage().getXid())
				.build();
		getSuccessingControlPlaneConnector().allToControlPlane(new TVMessage(resp, tvMessage.getDataplaneId()));
	}

	@Override
	public void flowModToDataPlane(TVMessage tvMessage) {
		SwitchConfig swc = TV.getConfig().getSwitchConfigById(tvMessage.getDataplaneId());
		String actionString, matchString;
		OFFlowMod flowmod = tvMessage.getOFMessage();

		// The table ID to store the rule in
		short tableID = flowmod.getTableId().getValue();

		// The priority to store the flow with
		int priority = flowmod.getPriority();

		// OFCommand (ADD, MODIFY, DELETE)
		String command = flowmod.getCommand().toString();

		// Create action string
		LinkedList<String> actionData = new LinkedList<>();
		HashSet<String> commands = new HashSet<>();

		for (OFInstruction inst : flowmod.getInstructions()) {
			switch (inst.getType()) {
				case APPLY_ACTIONS:
					for (OFAction ofAction : ((OFInstructionApplyActions) inst).getActions()) {
						switch (ofAction.getType()) {
							case OUTPUT:
								OFActionOutput outAction = (OFActionOutput) ofAction;
								// We can't pass a packet to the controller. --> Use P4 with proactive forwarding.
								if (outAction.getPort() == OFPort.CONTROLLER) {
									logger.warn("FLOW_MOD discarded. OFPort.CONTROLLER is not possible.");
									return;
								}

								String portName = swc.getP4Dict().ofParamToP4Param("OUT_PORT");
								String portVal = "" + outAction.getPort().getShortPortNumber();
								actionData.add(f("'%s': { 'value': 'p%s' }", portName, portVal));
								commands.add(ofAction.getType().toString());
								break;

							case POP_MPLS:
								OFActionPopMpls mplsPopAction = (OFActionPopMpls) ofAction;
								String ethertypeName = swc.getP4Dict().ofParamToP4Param("POP_ETHERTYPE");
								String ethertypeVal = mplsPopAction.getEthertype().toString();
								actionData.add(f("'%s': { 'value': '%s' }", ethertypeName, ethertypeVal));
								commands.add("MPLS_POP");
								break;

							case SET_FIELD:
								OFActionSetField setFieldAction = (OFActionSetField) ofAction;
								OFOxm<?> field = setFieldAction.getField();
								String name = swc.getP4Dict().ofParamToP4Param(field.getMatchField().getName());
								String value = field.getValue().toString();
								if (name == null) {
									logger.warn("Unknown parameter {}", field.getMatchField().getName());
									break;
								}
								actionData.add(f("'%s': { 'value': '%s' }", name, value));
								commands.add(ofAction.getType().toString() + "_" + field.getMatchField().getName());
								break;

							default:
								logger.warn("Unknown action {}", ofAction.getType());
								break;
						}
					}
					break;

				case GOTO_TABLE:
					OFInstructionGotoTable gotoInst = (OFInstructionGotoTable) inst;
					commands.add("GOTO_TABLE_" + gotoInst.getTableId().getValue());
					break;

				default:
					logger.warn("Unknown instruction '{}'", inst.getType().toString());
					break;
			}
		}

		if (commands.isEmpty()) {
			commands.add("DROP");
		}
		String actionType = swc.getP4Dict().ofActionToP4Action(commands);
		actionString = f("{ 'type': '%s', 'data': { %s } }", actionType, actionData.stream().collect(Collectors.joining(", ")));

		// Create match string
		Match match = flowmod.getMatch();
		List<String> matchList = new LinkedList<>();
		for(MatchField mField : match.getMatchFields()) {
			String p4Field = swc.getP4Dict().ofFieldToP4Field(mField.getName());
			if (p4Field == null) {
				logger.warn("Unknown match {}", mField.getName());
			}
			else {
				String value = match.get(mField).toString();
				matchList.add(f("'%s': { 'value': '%s' }", p4Field, value));
			}
		}

		matchString = "{ " + matchList.stream().collect(Collectors.joining(", ")) + " }";
		String ruleName = "r" + flowmod.getCookie().toString().substring(2);

		LinkedList<String> cliParams = new LinkedList<>();
		cliParams.add("tables");

		String tblString = swc.getP4Dict().tableIdToP4Name((int) tableID);
		if (tblString == null) {
			logger.error("Invalid table id {}. FLOW_MOD discarded.", tableID);
			return;
		}

		cliParams.add("--table-name " + tblString);

		// ADD, DELETE, MODIFY
		switch (command) {
			case "ADD":
				cliParams.add("add");
				break;
			case "DELETE_STRICT":
				cliParams.add("delete");
				break;
			case "MODIFY_STRICT":
				cliParams.add("edit");
				break;

			default:
				logger.warn("Unknown FlowMod command '{}'", command);
				return;
		}
		cliParams.add("--rule " + ruleName);
		cliParams.add("--match " + matchString);
		cliParams.add("--action " + actionString);
		cliParams.add("--priority " + priority);

		if (matchList.isEmpty())
			cliParams.add("--default");

		TVMessage translated = new TVMessage(cliParams, tvMessage.getDataplaneId(), null);
		super.allToDataPlane(translated);
	}

	private String f(String format, String... params) {
		return String.format(format, (Object[]) params).replaceAll("'", "\"");
	}

	@Override
	public void barrierToDataPlane(TVMessage tvMessage) {
		// logger.warn("BARRIER_REQUEST discarded.");

		OFBarrierReply rep = fac.buildBarrierReply().setXid(tvMessage.getOFMessage().getXid()).build();
		TVMessage repMsg = new TVMessage(rep, tvMessage.getDataplaneId());
		super.allToControlPlane(repMsg);
	}
}

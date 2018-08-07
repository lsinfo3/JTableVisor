package de.uniwue.info3.tablevisor.application;

import com.google.common.collect.ImmutableSet;
import de.uniwue.info3.tablevisor.config.IdPair;
import de.uniwue.info3.tablevisor.core.TableVisor;
import de.uniwue.info3.tablevisor.message.TVMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;

import java.util.*;

public class MultiSwitchApplication extends BaseApplication {
    private static final Logger logger = LogManager.getLogger();
    private final TableVisor TV = TableVisor.getInstance();

    // Count number of switches that replied to the requests
    private Set<Integer> switchConfigCounter = new HashSet<>();
    private Set<Integer> featuresCounter = new HashSet<>();
    private Set<Integer> portStatsCounter = new HashSet<>();
    private Set<Integer> flowStatsCounter = new HashSet<>();
    private Set<Integer> meterStatsCounter = new HashSet<>();
    private Set<Integer> meterFeaturesCounter = new HashSet<>();
    private Set<Integer> portDesccounter = new HashSet<>();
    private Set<Integer> tableCounter = new HashSet<>();
    private Set<Integer> roleCounter = new HashSet<>();
    private Set<Integer> barrierCounter = new HashSet<>();
    private Set<Integer> groupCounter = new HashSet<>();
    private Set<Integer> groupDescCounter = new HashSet<>();
    private Set<Integer> groupFeaturesCounter = new HashSet<>();
    // Keep track of replied content
    private List<OFPortStatsEntry> portStats = new ArrayList<>();
    private List<OFTableStatsEntry> tableEntries = new ArrayList<>();
    private List<OFFlowStatsEntry> flowStats = new ArrayList<>();
    private List<OFPortDesc> portDesc = new ArrayList<>();
    private List<OFMeterStats> meterStats = new ArrayList<>();
    private List<OFMeterFeaturesStatsReply> meterFeaturesReplies = new ArrayList<>();
    private List<OFFeaturesReply> featuresReplies = new ArrayList<>();
    private Set<OFCapabilities> fearturesCapas = new HashSet<>();
    private Set<OFConfigFlags> configFlags = new HashSet<>();
    private List<OFGroupStatsEntry> groupStats = new ArrayList<>();
    private List<OFGroupDescStatsEntry> groupDescStats = new ArrayList<>();
    private Set<OFStatsReplyFlags> groupFlags = new HashSet<>();
    private Set<OFStatsReplyFlags> groupDescFlags = new HashSet<>();
    private List<OFGroupFeaturesStatsReply> groupFeaturesReplies = new ArrayList<>();

    public MultiSwitchApplication(IApplication controlPlaneConnector) {
        super(controlPlaneConnector);
    }

    @Override
    public synchronized void switchFeaturesToControlPlane(TVMessage tvMessage) {
        OFFactory factory = OFFactories.getFactory(tvMessage.getOFMessage().getVersion());
        OFFeaturesReply reply = tvMessage.getOFMessage();

        if (featuresCounter.add(tvMessage.getDataplaneId())) {
            fearturesCapas.addAll(reply.getCapabilities());
            featuresReplies.add(reply);
        }

        if (allAnswered(featuresCounter)) {
            long minBuffer = featuresReplies.stream()
                    .mapToLong(r -> r.getNBuffers())
                    .filter(l -> l != 0)
                    .min().orElse(0);
            short nTables = (short) TV.getConfig().getTotalNumberOfTables();

            OFFeaturesReply featuresReply = factory.buildFeaturesReply()
                    .setCapabilities(new HashSet<>(fearturesCapas))
                    .setNBuffers(minBuffer)
                    .setNTables(nTables)
                    .setDatapathId(TV.getConfig().getOurDatapathId())
                    .build();
            TVMessage newMsg = new TVMessage(featuresReply, tvMessage.getDataplaneId());
            getSuccessingControlPlaneConnector().allToControlPlane(newMsg);

            fearturesCapas.clear();
            featuresReplies.clear();
            featuresCounter.clear();
        }
    }

    @Override
    public synchronized void switchGetConfigToControlPlane(TVMessage tvMessage) {
        OFFactory factory = OFFactories.getFactory(tvMessage.getOFMessage().getVersion());
        OFGetConfigReply reply = tvMessage.getOFMessage();

        if (switchConfigCounter.add(tvMessage.getDataplaneId())) {
            configFlags.addAll(reply.getFlags());
        }

        if (allAnswered(switchConfigCounter)) {
            OFGetConfigReply confReply = factory.buildGetConfigReply()
                    .setXid(reply.getXid())
                    .setFlags(new HashSet<>(configFlags))
                    .setMissSendLen(reply.getMissSendLen())
                    .build();
            TVMessage newMsg = new TVMessage(confReply, tvMessage.getDataplaneId());
            getSuccessingControlPlaneConnector().allToControlPlane(newMsg);

            switchConfigCounter.clear();
            configFlags.clear();
        }
    }

    @Override
    public synchronized void switchStatsToControlPlane(TVMessage tvMessage) {
        OFFactory factory = OFFactories.getFactory(tvMessage.getOFMessage().getVersion());
        OFStatsReply switchReply = tvMessage.getOFMessage();
        OFStatsReply ourReply = null;

        switch (switchReply.getStatsType()) {
            case PORT_DESC:
                if (portDesccounter.add(tvMessage.getDataplaneId())) {
                    portDesc.addAll(((OFPortDescStatsReply) switchReply).getEntries());
                }
                if (allAnswered(portDesccounter)) {
                    ourReply = factory.buildPortDescStatsReply()
                            .setXid(switchReply.getXid())
                            .setEntries(new ArrayList<>(portDesc))
                            .build();
                    portDesccounter.clear();
                    portDesc.clear();
                }
                break;

            case PORT:
                if (portStatsCounter.add(tvMessage.getDataplaneId())) {
                    portStats.addAll(((OFPortStatsReply) switchReply).getEntries());
                }
                if (allAnswered(portStatsCounter)) {
                    ourReply = factory.buildPortStatsReply()
                            .setXid(switchReply.getXid())
                            .setEntries(new ArrayList<>(portStats))
                            .build();
                    portStatsCounter.clear();
                    portStats.clear();
                }
                break;

            case GROUP:
                if (groupCounter.add(tvMessage.getDataplaneId())) {
                    groupStats.addAll(((OFGroupStatsReply) switchReply).getEntries());
                    groupFlags.addAll(((OFGroupStatsReply) switchReply).getFlags());
                }
                if (allAnswered(groupCounter)) {
                    ourReply = factory.buildGroupStatsReply()
                            .setXid(switchReply.getXid())
                            .setEntries(new ArrayList<>(groupStats))
                            .setFlags(new HashSet<>(groupFlags))
                            .build();
                    groupCounter.clear();
                    groupStats.clear();
                    groupFlags.clear();
                }
                break;

            case GROUP_DESC:
                if (groupDescCounter.add(tvMessage.getDataplaneId())) {
                    groupDescStats.addAll(((OFGroupDescStatsReply) switchReply).getEntries());
                    groupDescFlags.addAll(((OFGroupDescStatsReply) switchReply).getFlags());
                }
                if (allAnswered(groupDescCounter)) {
                    ourReply = factory.buildGroupDescStatsReply()
                            .setXid(switchReply.getXid())
                            .setEntries(new ArrayList<>(groupDescStats))
                            .setFlags(new HashSet<>(groupDescFlags))
                            .build();
                    groupDescCounter.clear();
                    groupDescStats.clear();
                    groupDescFlags.clear();
                }
                break;

            case GROUP_FEATURES:
                if (groupFeaturesCounter.add(tvMessage.getDataplaneId())) {
                    groupFeaturesReplies.add((OFGroupFeaturesStatsReply) switchReply);
                }
                if (allAnswered(groupFeaturesCounter)) {
                    long actionsAll = 0;
                    long actionsFf = 0;
                    long actionsIndircet = 0;
                    long actionsSelect = 0;
                    long maxGroupsAll = 0;
                    long maxGroupsFf = 0;
                    long maxGroupsIndirect = 0;
                    long maxGroupsSelect = 0;
                    long types = 0;
                    Set<OFStatsReplyFlags> flags = new HashSet<>();
                    Set<OFGroupCapabilities> capabilities = new HashSet<>();


                    for (OFGroupFeaturesStatsReply rep : groupFeaturesReplies) {
                        flags.addAll(rep.getFlags());
                        capabilities.addAll(capabilities);
                        actionsAll = (actionsAll == -1 ? rep.getActionsAll() : Math.min(actionsAll, rep.getActionsAll()));
                        actionsFf = (actionsFf == -1 ? rep.getActionsFf() : Math.min(actionsFf, rep.getActionsFf()));
                        actionsIndircet = (actionsIndircet == -1 ? rep.getActionsIndirect() : Math.min(actionsIndircet, rep.getActionsIndirect()));
                        actionsSelect = (actionsSelect == -1 ? rep.getActionsSelect() : Math.min(actionsSelect, rep.getActionsSelect()));
                        maxGroupsAll = (maxGroupsAll == -1 ? rep.getMaxGroupsAll() : Math.min(maxGroupsAll, rep.getMaxGroupsAll()));
                        maxGroupsFf = (maxGroupsFf == -1 ? rep.getMaxGroupsFf() : Math.min(maxGroupsFf, rep.getMaxGroupsFf()));
                        maxGroupsIndirect = (maxGroupsIndirect == -1 ? rep.getMaxGroupsIndirect() : Math.min(maxGroupsIndirect, rep.getMaxGroupsIndirect()));
                        maxGroupsSelect = (maxGroupsSelect == -1 ? rep.getMaxGroupsSelect() : Math.min(maxGroupsSelect, rep.getMaxGroupsSelect()));
                        types = (types == -1 ? rep.getTypes() : Math.min(types, rep.getTypes()));
                    }
                    ourReply = factory.buildGroupFeaturesStatsReply()
                            .setXid(switchReply.getXid())
                            .setFlags(flags)
                            .setActionsAll(actionsAll)
                            .setActionsFf(actionsFf)
                            .setActionsIndirect(actionsIndircet)
                            .setActionsSelect(actionsSelect)
                            .setMaxGroupsAll(maxGroupsAll)
                            .setMaxGroupsFf(maxGroupsFf)
                            .setMaxGroupsIndirect(maxGroupsIndirect)
                            .setMaxGroupsSelect(maxGroupsSelect)
                            .setTypes(types)
                            .setCapabilities(capabilities)
                            .build();
                    groupFeaturesCounter.clear();
                    groupFeaturesReplies.clear();
                }
                break;

            case DESC:
                ourReply = factory.buildDescStatsReply()
                        .setXid(switchReply.getXid())
                        .setHwDesc("Emulated MultiSwitch")
                        .setSwDesc(TableVisor.VERSION)
                        .setMfrDesc("TableVisor")
                        .build();
                initializePipeline(tvMessage,factory);
                break;

            case TABLE:
                if (tableCounter.add(tvMessage.getDataplaneId())) {
                    tableEntries.addAll(
                            adaptTableStatsEntries(((OFTableStatsReply) switchReply).getEntries(), tvMessage.getDataplaneId())
                    );
                }
                if (allAnswered(tableCounter)) {
                    ourReply = factory.buildTableStatsReply()
                            .setXid(switchReply.getXid())
                            .setEntries(new ArrayList<>(tableEntries))
                            .build();
                    tableCounter.clear();
                    tableEntries.clear();
                }
                break;

            case METER:
                if (meterStatsCounter.add(tvMessage.getDataplaneId())) {
                    meterStats.addAll(((OFMeterStatsReply) switchReply).getEntries());
                }
                if (allAnswered(meterStatsCounter)) {
                    ourReply = factory.buildMeterStatsReply()
                            .setXid(switchReply.getXid())
                            .setEntries(new ArrayList<>(meterStats))
                            .build();
                    meterStatsCounter.clear();
                    meterStats.clear();
                }
                break;

            case METER_FEATURES:
                if (meterFeaturesCounter.add(tvMessage.getDataplaneId())) {
                    meterFeaturesReplies.add((OFMeterFeaturesStatsReply) switchReply);
                }
                if (allAnswered(meterFeaturesCounter)) {
                    long capabilities = 0;
                    long bandTypes = 0;
                    int maxBands = -1;
                    int maxColors = -1;
                    long maxMeter = -1;
                    for (OFMeterFeaturesStatsReply rep : meterFeaturesReplies) {
                        capabilities = capabilities | rep.getFeatures().getCapabilities();
                        bandTypes = bandTypes | rep.getFeatures().getBandTypes();
                        maxBands = (maxBands == -1 ? rep.getFeatures().getMaxBands() : Math.min(maxBands, rep.getFeatures().getMaxBands()));
                        maxColors = (maxColors == -1 ? rep.getFeatures().getMaxBands() : Math.min(maxColors, rep.getFeatures().getMaxBands()));
                        maxMeter = (maxMeter == -1 ? rep.getFeatures().getMaxBands() : Math.min(maxMeter, rep.getFeatures().getMaxBands()));
                    }

                    OFMeterFeatures feat = factory.buildMeterFeatures()
                            .setCapabilities(capabilities)
                            .setBandTypes(bandTypes)
                            .setMaxBands((short) maxBands)
                            .setMaxColor((short) maxColors)
                            .setMaxMeter(maxMeter)
                            .build();
                    ourReply = factory.buildMeterFeaturesStatsReply()
                            .setXid(switchReply.getXid())
                            .setFeatures(feat)
                            .build();
                    meterFeaturesCounter.clear();
                    meterFeaturesReplies.clear();
                }
                break;

//            case FLOW:
//                if (flowStatsCounter.add(tvMessage.getDataplaneId())) {
//                    ArrayList<OFFlowStatsEntry> lastTableEntries = new ArrayList<>();
//                    ArrayList<OFFlowStatsEntry> firstTableEntries = new ArrayList<>();
//
//                    if (tvMessage.getDataplaneId() == TV.getConfig().smallestDataplaneId()) {
//                        for (OFFlowStatsEntry entry : ((OFFlowStatsReply) switchReply).getEntries()) {
//                            if (matchContainsInPortFromHigherDevice(entry, tvMessage.getDataplaneId())) {
//                                entry = entry.createBuilder().setMatch(
//                                        removeInPortFromMatch(entry.getMatch())
//                                ).build();
//                                lastTableEntries.add(entry);
//                            }
//                            else {
//                                firstTableEntries.add(entry);
//                            }
//                        }
//                    }
//                    else {
//                        firstTableEntries.addAll(((OFFlowStatsReply) switchReply).getEntries());
//                    }
//
//                    flowStats.addAll(
//                            adaptFlowStatsEntries(lastTableEntries, tvMessage.getDataplaneId(), true)
//                    );
//                    flowStats.addAll(
//                            adaptFlowStatsEntries(firstTableEntries, tvMessage.getDataplaneId(), false)
//                    );
//                }
//                else {
//                    logger.warn("Flow Stats Replies of Data Plane Devices out of sync (duplicate reply from {})", tvMessage.getDataplaneId());
//                }
//                if (allAnswered(flowStatsCounter)) {
//                    ourReply = factory.buildFlowStatsReply()
//                            .setXid(switchReply.getXid())
//                            .setFlags(switchReply.getFlags())
//                            .setEntries(new ArrayList<>(flowStats))
//                            .build();
//                    flowStatsCounter.clear();
//                    flowStats.clear();
//                }
//                break;

            case FLOW:
                if (flowStatsCounter.add(tvMessage.getDataplaneId())) {
                    ArrayList<OFFlowStatsEntry> lastTableEntries = new ArrayList<>();
                    ArrayList<OFFlowStatsEntry> firstTableEntries = new ArrayList<>();

                    if (tvMessage.getDataplaneId() == TV.getConfig().smallestDataplaneId()) {
                        for (OFFlowStatsEntry entry : ((OFFlowStatsReply) switchReply).getEntries()) {
                            if (matchContainsInPortFromHigherDevice(entry, tvMessage.getDataplaneId())) {
                                entry = entry.createBuilder().setMatch(
                                        removeInPortFromMatch(entry.getMatch())
                                ).build();
                                lastTableEntries.add(entry);
                            }
                            else {
                                firstTableEntries.add(entry);
                            }
                        }
                    }
                    else {
                        firstTableEntries.addAll(((OFFlowStatsReply) switchReply).getEntries());
                    }

                    flowStats.addAll(
                            adaptFlowStatsEntries(lastTableEntries, tvMessage.getDataplaneId(), true)
                    );
                    flowStats.addAll(
                            adaptFlowStatsEntries(firstTableEntries, tvMessage.getDataplaneId(), false)
                    );

                    Set<OFStatsReplyFlags> ourFlags = new HashSet<>(switchReply.getFlags());
                    if (!allAnswered(flowStatsCounter)) {
                        ourFlags.add(OFStatsReplyFlags.REPLY_MORE);
                    }
                    else {
                        flowStatsCounter.clear();
                    }
                    ourReply = factory.buildFlowStatsReply()
                            .setXid(switchReply.getXid())
                            .setFlags(ourFlags)
                            .setEntries(new ArrayList<>(flowStats))
                            .build();
                    flowStats.clear();
                }
                else {
                    logger.warn("Flow Stats Replies of Data Plane Devices out of sync (duplicate reply from {})", tvMessage.getDataplaneId());
                }
                break;

            default:
                logger.warn("Unrecognized SwitchStatsReply type '{}'", switchReply.getStatsType());
                break;
        }

        if (ourReply != null) {
            TVMessage newMsg = new TVMessage(ourReply, tvMessage.getDataplaneId());
            getSuccessingControlPlaneConnector().allToControlPlane(newMsg);
        }
    }

    private Match removeInPortFromMatch(Match oldMatch) {
        Match.Builder builder = OFFactories.getFactory(oldMatch.getVersion()).buildMatch();
        for (MatchField field : oldMatch.getMatchFields()) {
            if (field.id != MatchFields.IN_PORT) {
                if (oldMatch.isExact(field)) {
                    builder.setExact(field, oldMatch.get(field));
                }
                else {
                    builder.setMasked(field, oldMatch.getMasked(field));
                }
            }
        }
        return builder.build();
    }

    private boolean matchContainsInPortFromHigherDevice(OFFlowStatsEntry entry, int dataplaneId) {
        Match m = entry.getMatch();
        for (MatchField field : m.getMatchFields()) {
            if (m.isExact(field) && field.id == MatchFields.IN_PORT) {
                MatchField<OFPort> portMatch = (MatchField<OFPort>) field;
                OFPort ofport = m.get(portMatch);
                if (ofport != null) {
                    Integer targetDpId = TV.getConfig().getDatapleIdOfOutputPort(dataplaneId, ofport.getShortPortNumber());

                    if (targetDpId != null && targetDpId > dataplaneId) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void tableModToDataPlane(TVMessage tvMessage) {
        OFTableMod tableMod = tvMessage.getOFMessage();
        IdPair dest = TV.ourTableIdToSwitchId(tableMod.getTableId());
        TVMessage newMsg = new TVMessage(
                tableMod.createBuilder().setTableId(dest.tableIdObj).build(),
                dest.dataplaneId
        );
        getSuccessingDataPlaneConnector().allToDataPlane(newMsg);
    }

    @Override
    public void flowModToControlPlane(TVMessage tvMessage) {
        OFFlowRemoved rmv = tvMessage.getOFMessage();
        TVMessage newMsg = new TVMessage(
                rmv.createBuilder().setTableId(TableId.of(
                        TV.switchIdToOurTableId(tvMessage.getDataplaneId(), rmv.getTableId())[0]
                )).build(),
                tvMessage.getDataplaneId()
        );
        getSuccessingControlPlaneConnector().allToControlPlane(newMsg);
    }

    @Override
    public void flowModToDataPlane(TVMessage tvMessage) {
        OFFactory factory = OFFactories.getFactory(tvMessage.getOFMessage().getVersion());
        OFFlowMod flowMod = tvMessage.getOFMessage();
        IdPair dest = TV.ourTableIdToSwitchId(flowMod.getTableId());
        //Add elemetns to checkLastTable if addressed to Last table by onos

        ArrayList<OFInstruction> newInsts = new ArrayList<>();
        ArrayList<OFAction> newActions = new ArrayList<>();
        int foundIndex = -1;
        int currentIndex = 0;
        for (OFInstruction inst : flowMod.getInstructions()) {
            if (inst.getType() == OFInstructionType.GOTO_TABLE) {
                OFInstructionGotoTable instGoto = (OFInstructionGotoTable) inst;
                IdPair pairFrom = TV.ourTableIdToSwitchId(flowMod.getTableId());
                IdPair pairGoto = TV.ourTableIdToSwitchId(instGoto.getTableId());

                if (pairGoto.dataplaneId != pairFrom.dataplaneId) {
                    int outPort = TV.getConfig().getOutPortOfDataplaneId(pairFrom.dataplaneId, pairGoto.dataplaneId);
                    if (pairFrom.dataplaneId > pairGoto.dataplaneId) {
                        outPort = TV.getConfig().getInPortOfDataplaneId(pairFrom.dataplaneId, pairGoto.dataplaneId);
                    }

                    OFActionOutput action = factory.actions().buildOutput().setPort(
                            OFPort.of(outPort)
                    ).build();
                    newActions.add(action);
                } else {
                    newInsts.add(instGoto.createBuilder().setTableId(
                            TableId.of(TV.getConfig().getSwitchConfigById(pairFrom.dataplaneId).tableMap.get(
                                    (int) instGoto.getTableId().getValue()
                            ))
                    ).build());
                }
            } else if (inst.getType() == OFInstructionType.APPLY_ACTIONS) {
                newActions.addAll(((OFInstructionApplyActions) inst).getActions());
                foundIndex = currentIndex;
            } else {
                newInsts.add(inst);
            }
            currentIndex++;
        }

        // If we have multiple Instructions with individual action lists, aggregate them here.
        if (foundIndex == -1) {
            foundIndex = currentIndex;
        }
        if (!newActions.isEmpty()) {
            OFInstructionApplyActions outInst = factory.instructions().buildApplyActions().setActions(
                    newActions
            ).build();
            if (newInsts.isEmpty()){
                newInsts.add(outInst);
            }
            else {
                newInsts.add(foundIndex, outInst);
            }
        }

        flowMod = flowMod.createBuilder().setInstructions(newInsts).build();
        if (flowMod.getTableId().getValue() == TV.maxTableId()) {
            Match oldMatch = flowMod.getMatch();
            Match.Builder builder = OFFactories.getFactory(oldMatch.getVersion()).buildMatch();
            for (MatchField field : oldMatch.getMatchFields()) {
                if (oldMatch.isExact(field)) {
                    builder.setExact(field, oldMatch.get(field));
                    if (field.id == MatchFields.IN_PORT) {
                        logger.warn("Cannot match for IN_PORT in the highest table ID ({})", TV.maxTableId());
                    }
                }
                else {
                    builder.setMasked(field, oldMatch.getMasked(field));
                }
            }
            builder.setExact(
                    MatchField.IN_PORT, OFPort.of(
                            TV.getConfig().getInPortOfDataplaneId(dest.dataplaneId, TV.getConfig().nextBiggerDataplaneId(dest.dataplaneId))
                    ));
            Match newMatch = builder.build();
            flowMod = flowMod.createBuilder().setMatch(newMatch).build();
        }

        TVMessage newMsg = new TVMessage(
                flowMod.createBuilder().setTableId(dest.tableIdObj).build(),
                dest.dataplaneId
        );

        getSuccessingDataPlaneConnector().allToDataPlane(newMsg);
    }

    @Override
    public void packetInToControlPlane(TVMessage tvMessage) {
        OFPacketIn packInSwitch = tvMessage.getOFMessage();
        TVMessage packInOurs = new TVMessage(
                packInSwitch.createBuilder().setTableId(TableId.of(
                        TV.switchIdToOurTableId(tvMessage.getDataplaneId(), packInSwitch.getTableId())[0]
                )).build(),
                tvMessage.getDataplaneId()
        );
        getSuccessingControlPlaneConnector().allToControlPlane(packInOurs);
    }

    @Override
    public void packetOutToDataPlane(TVMessage tvMessage) {
        IdPair pair = TV.ourTableIdToSwitchId(0);
        tvMessage.setDataplaneId(pair.dataplaneId);
        getSuccessingDataPlaneConnector().allToDataPlane(tvMessage);
    }

    @Override
    public synchronized void roleToControlPlane(TVMessage tvMessage) {
        roleCounter.add(tvMessage.getDataplaneId());

        if (allAnswered(roleCounter)) {
            getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
            roleCounter.clear();
        }
    }

    @Override
    public synchronized void barrierToControlPlane(TVMessage tvMessage) {
        barrierCounter.add(tvMessage.getDataplaneId());

        if (allAnswered(barrierCounter)) {
            getSuccessingControlPlaneConnector().allToControlPlane(tvMessage);
            barrierCounter.clear();
        }
    }

    @Override
    public void switchFeaturesToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void switchGetConfigToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void switchStatsToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void roleToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void groupModToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void portModToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void meterModToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void miscToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void setConfigToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void barrierToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void p4ToDataPlane(TVMessage tvMessage) {
        sendToAllSwitches(tvMessage);
    }

    @Override
    public void errorToControlPlane(TVMessage tvMessage) {
        // Do nothing. Eat errors.
    }

    @Override
    public void errorToDataPlane(TVMessage tvMessage) {
        // Do nothing. Eat errors.
    }

    private void sendToAllSwitches(TVMessage tvMessage) {
        for (int i : TV.getLowerEndpointManager().getSockets().keySet()) {
            getSuccessingDataPlaneConnector().allToDataPlane(tvMessage.copyWithDpId(i));
        }
    }

    private boolean allAnswered(Collection seen) {
        return seen.size() == TV.getLowerEndpointManager().getSockets().size();
    }

    private List<OFTableStatsEntry> adaptTableStatsEntries(List<OFTableStatsEntry> entries, int dataplaneId) {
        ArrayList<OFTableStatsEntry> filteredStats = new ArrayList<>();
        for (OFTableStatsEntry e : entries) {
            int tableId = e.getTableId().getValue();
            IdPair pair = new IdPair(dataplaneId, tableId);
            Integer[] ourTableId = TV.switchIdToOurTableId(pair);

            if (ourTableId != null) {
                for (int i = 0; i < ourTableId.length; i++) {
                    filteredStats.add(
                            e.createBuilder().setTableId(TableId.of(ourTableId[i])).build()
                    );
                }
            }
        }
        return filteredStats;
    }

    private List<OFFlowStatsEntry> adaptFlowStatsEntries(List<OFFlowStatsEntry> entries, int dataplaneId, boolean lastTable) {
        if (entries.isEmpty()) {
            return new ArrayList<>();
        }
        OFFactory factory = OFFactories.getFactory(entries.get(0).getVersion());

        ArrayList<OFFlowStatsEntry> filteredStats = new ArrayList<>();
        loop:
        for (OFFlowStatsEntry e : entries) {
            int tableId = e.getTableId().getValue();
            IdPair pair = new IdPair(dataplaneId, tableId);
            Integer[] ourTableId = TV.switchIdToOurTableId(pair);
            // Translate OUTPUT to another switch back into GOTO-TABLE
            ArrayList<OFInstruction> insts = new ArrayList<>();
            OFInstruction gotoInst = null;
            for (OFInstruction inst : e.getInstructions()) {
                if (inst.getType() == OFInstructionType.APPLY_ACTIONS) {
                    OFInstructionApplyActions applyInst = (OFInstructionApplyActions) inst;

                    ArrayList<OFAction> actions = new ArrayList<>();
                    for (OFAction act : applyInst.getActions()) {
                        if (act.getType() == OFActionType.OUTPUT) {
                            if (dataplaneId != TV.getConfig().smallestDataplaneId() && dataplaneId != TV.getConfig().biggestDataplaneId()) {
                                if (e.getMatch() != null) {
                                    Match hideRules = factory.buildMatch()
                                            .setExact(MatchField.IN_PORT, OFPort.of(
                                                    TV.getConfig().nextBiggerDataplaneId(dataplaneId)
                                            )).build();
                                    if (e.getMatch().toString().equals(hideRules.toString())){
                                        continue loop;
                                    }
                                }
                            }
                            OFActionOutput actOut = (OFActionOutput) act;
                            Integer dataplaneIdOfOutputPort = TV.getConfig().getDatapleIdOfOutputPort(dataplaneId, actOut.getPort().getPortNumber());
                            if (dataplaneIdOfOutputPort != null) {
                                // Get external table ID with internal ID 0 and GOTO there
                                Map<Integer, Integer> tableMap = TV.getConfig().getSwitchConfigById(dataplaneIdOfOutputPort).tableMap;
                                if (tableMap == null || tableMap.isEmpty()) {
                                    logger.warn("Empty table map for dataplane ID '{}' encountered", dataplaneIdOfOutputPort);
                                } else {
                                    int outTable = -1;
                                    IdPair pairGOTO = new IdPair(dataplaneIdOfOutputPort, 0);
                                    Integer[] translateGOTO = TV.switchIdToOurTableId(pairGOTO);
                                    if (translateGOTO != null) {
                                        outTable = translateGOTO[translateGOTO.length - 1];
                                    }
                                    if (outTable == -1) {
                                        logger.warn("No table with internal ID '0' found for dataplane ID '{}'", dataplaneIdOfOutputPort);
                                    } else {
                                        gotoInst = factory.instructions().buildGotoTable()
                                                .setTableId(TableId.of(outTable))
                                                .build();
                                    }
                                }
                            } else {
                                actions.add(act);
                            }
                        } else {
                            actions.add(act);
                        }
                    }

                    if (!actions.isEmpty()) {
                        insts.add(applyInst.createBuilder().setActions(actions).build());
                    }
                } else if (inst.getType() == OFInstructionType.GOTO_TABLE) {
                    OFInstructionGotoTable origGotoInst = (OFInstructionGotoTable) inst;
                    Map<Integer, Integer> tableMap = TV.getConfig().getSwitchConfigById(dataplaneId).tableMap;

                    int targetTable = -1;
                    for (Map.Entry<Integer, Integer> entry : tableMap.entrySet()) {
                        if (entry.getValue().equals((int) origGotoInst.getTableId().getValue())) {
                            targetTable = entry.getKey();
                            break;
                        }
                    }
                    if (targetTable == -1) {
                        logger.warn("No controlPlane-tableID found for switch-tableID '{}' of dataplane-ID '{}'", origGotoInst.getTableId().getValue(), dataplaneId);
                    } else {
                        insts.add(origGotoInst.createBuilder().setTableId(TableId.of(targetTable)).build());
                    }
                } else {
                    insts.add(inst);
                }
                if (gotoInst != null) {
                    insts.add(gotoInst);
                    gotoInst = null;
                }
            }
            if (ourTableId != null) {
                filteredStats.add(
                        e.createBuilder()
                                .setTableId(TableId.of(ourTableId[lastTable ? ourTableId.length - 1 : 0]))
                                .setInstructions(insts)
                                .build()
                );
            }
        }
        return filteredStats;
    }

    private void initializePipeline(TVMessage tvMessage,OFFactory factory){
        if (tvMessage.getDataplaneId() != TV.getConfig().biggestDataplaneId()
                && tvMessage.getDataplaneId() != TV.getConfig().smallestDataplaneId()){
            ArrayList<OFInstruction> hiddenInsts = new ArrayList<>();
            ArrayList<OFAction> hiddenActions = new ArrayList<>();
            OFActionOutput action = factory.actions().buildOutput()
                    .setPort(OFPort.of(
                            TV.getConfig().getInPortOfDataplaneId(
                                    tvMessage.getDataplaneId(), TV.getConfig().nextSmallerDataplaneId(tvMessage.getDataplaneId())
                            )
                    )).build();
            hiddenActions.add(action);
            OFInstructionApplyActions outInst = factory.instructions().buildApplyActions().setActions(
                    hiddenActions
            ).build();
            hiddenInsts.add(outInst);
            OFFlowAdd.Builder builder = factory.buildFlowAdd();
            builder.setXid(0x12345678)
                    .setCookie(U64.parseHex("FEDCBA98765432"))
                    .setCookieMask(U64.parseHex("0000000000000000"))
                    .setTableId(TableId.of(0))
                    .setIdleTimeout(0)
                    .setHardTimeout(0)
                    .setPriority(45678)
                    .setBufferId(OFBufferId.NO_BUFFER)
                    .setFlags(ImmutableSet.<OFFlowModFlags> of())
                    .setActions(hiddenActions)
                    .setMatch(factory.buildMatch()
                            .setExact(MatchField.IN_PORT, OFPort.of(
                                    TV.getConfig().getInPortOfDataplaneId(
                                            tvMessage.getDataplaneId(), TV.getConfig().nextBiggerDataplaneId(tvMessage.getDataplaneId())
                                    )
                            )).build())
                    .setInstructions(hiddenInsts);

            OFFlowAdd flowMod = builder.build();
            TVMessage newMsg = new TVMessage(flowMod, tvMessage.getDataplaneId());
            getSuccessingDataPlaneConnector().allToDataPlane(newMsg);
        }
    }
}



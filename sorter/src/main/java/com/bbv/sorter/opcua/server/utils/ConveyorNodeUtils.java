package com.bbv.sorter.opcua.server.utils;

import com.bbv.sorter.hardware.conveyor.Conveyor;
import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import com.bbv.sorter.opcua.server.methods.ChangeConveyorModeMethod;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.BaseVariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.MultiStateDiscreteNode;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static com.bbv.sorter.opcua.server.utils.NodePredicates.isEqualVariableNode;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * Created by lorenzodemicheli on 16.03.2018.
 */
public interface ConveyorNodeUtils {
    Logger logger = LoggerFactory.getLogger(ConveyorNodeUtils.class);

    LocalizedText MODE_STARTED = LocalizedText.english("STARTED");
    LocalizedText MODE_STOPPED = LocalizedText.english("STOPPED");
    LocalizedText[] MODES = {MODE_STARTED, MODE_STOPPED};

    String BROWSE_NAME_CONVEYOR_MODE = "Mode";
    String BROWSE_NAME_CONVEYOR_STATUS = "Status";
    String BROWSE_NAME_CHANGE_CONVEYOR_MODE = "Change Mode";


    static AttributeDelegate getAttributeDelegate(Function<Conveyor, Object> consumer) {
        return
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(consumer.apply(ConveyorFactory.getInstance())));
                    }
                };
    }


    static UaObjectTypeNode createConveyorTypeNode(OpcUaServer server, UShort namespaceIndex) {
        // Define a new ObjectType called "ConveyorType".
        return UaObjectTypeNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/ConveyorType"))
                .setBrowseName(new QualifiedName(namespaceIndex, "ConveyorType"))
                .setDisplayName(LocalizedText.english("ConveyorType"))
                .setDescription(LocalizedText.english("Fisher Conveyor Belt 24V"))
                .setIsAbstract(false)
                .build();
    }


    static UaVariableNode addStatusInstanceDeclaration(UaObjectTypeNode conveyorTypeNode, OpcUaServer server, UShort namespaceIndex) {
        UaVariableNode conveyorTypeVariableNodeStatus = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/ConveyorType.Status"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, BROWSE_NAME_CONVEYOR_STATUS))
                .setDisplayName(LocalizedText.english("Status"))
                .setDescription(LocalizedText.english("The Status , On/Off"))
                .setDataType(Identifiers.Boolean)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .setHistorizing(true)
                .build();

        conveyorTypeVariableNodeStatus.setValue(new DataValue(new Variant(false)));
        conveyorTypeVariableNodeStatus.setMinimumSamplingInterval(1.0);
        conveyorTypeNode.addComponent(conveyorTypeVariableNodeStatus);
        return conveyorTypeVariableNodeStatus;
    }


    static UaVariableNode addModeInstanceDeclaration(UaObjectTypeNode conveyorTypeNode, OpcUaServer server, UShort namespaceIndex) {
        // "Mode" and "Status" are members. These nodes are what are called "instance declarations" by the spec.
        NodeId nodeId = new NodeId(namespaceIndex, "ObjectTypes/ConveyorType.Mode");
        UByte accessLevel = ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE));
        QualifiedName browseName = new QualifiedName(namespaceIndex, BROWSE_NAME_CONVEYOR_MODE);
        LocalizedText displayName = LocalizedText.english("Mode");
        LocalizedText description = LocalizedText.english("The Mode , Started/Stopped");
        Reference typeDefinition = new Reference(nodeId, Identifiers.HasTypeDefinition, new ExpandedNodeId(Identifiers.MultiStateDiscreteType), NodeClass.VariableType, true);

        MultiStateDiscreteNode conveyorTypeVariableNodeMode = new MultiStateDiscreteNode(server.getNodeMap(), nodeId, browseName, displayName, description, uint(0), uint(0));
        conveyorTypeVariableNodeMode.setValue(new DataValue(new Variant(0)));
        conveyorTypeVariableNodeMode.setMinimumSamplingInterval(1.0);
        conveyorTypeVariableNodeMode.setEnumStrings(ConveyorNodeUtils.MODES);
        conveyorTypeVariableNodeMode.setDataType(Identifiers.UInteger);
        conveyorTypeVariableNodeMode.addReference(typeDefinition);

        conveyorTypeNode.addComponent(conveyorTypeVariableNodeMode);
        return conveyorTypeVariableNodeMode;
    }


    static UaObjectNode createConveyorInstance(UaObjectTypeNode conveyorTypeNode, NodeFactory nodeFactory, UShort namespaceIndex) {
        // Use NodeFactory to create instance of ConveyorType called "Conveyor".
        // NodeFactory takes care of recursively instantiating Conveyor member nodes
        // as well as adding all nodes to the address space.
        UaObjectNode conveyor = nodeFactory.createObject(
                new NodeId(namespaceIndex, "Sorter/Conveyor"),
                new QualifiedName(namespaceIndex, "Conveyor"),
                LocalizedText.english("Conveyor"),
                conveyorTypeNode.getNodeId()
        );

        ConveyorNodeUtils.enhanceConveyorModeInstance(conveyor);
        ConveyorNodeUtils.enhanceConveyorStatusInstance(conveyor);
        ConveyorSpeedIndicatorUtils.enhanceSpeedIndicatorInstance(conveyor);

        return conveyor;
    }


    static void enhanceConveyorStatusInstance(UaObjectNode conveyor) {
        conveyor.getComponentNodes().stream()
                .filter(isEqualVariableNode(BROWSE_NAME_CONVEYOR_STATUS, BaseVariableNode.class)).map(x -> ((BaseVariableNode) x))
                .forEach(variable -> {
                    variable.setAttributeDelegate(ConveyorNodeUtils.getAttributeDelegate(Conveyor::getStatus));
                });
    }

    static void enhanceConveyorModeInstance(UaObjectNode conveyor) {
        conveyor.getComponentNodes().stream()
                .filter(isEqualVariableNode(BROWSE_NAME_CONVEYOR_MODE, MultiStateDiscreteNode.class)).map(x -> ((MultiStateDiscreteNode) x))
                .forEach((MultiStateDiscreteNode multiStateDiscreteNode) -> {
                    multiStateDiscreteNode.setDisplayName(LocalizedText.english("M1 Mode"));
                    multiStateDiscreteNode.setAttributeDelegate(ConveyorNodeUtils.getAttributeDelegate(Conveyor::getMode));
                });
    }


    static void addChangeConveyorMethodNode(UaObjectNode conveyor, OpcUaServer server, UShort namespaceIndex) {
        UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "Sorter/Conveyor/ChangeMode"))
                .setBrowseName(new QualifiedName(namespaceIndex, BROWSE_NAME_CHANGE_CONVEYOR_MODE))
                .setDisplayName(new LocalizedText(null, "M1 Change Mode"))
                .setDescription(LocalizedText.english("Start Stop the Conveyor"))
                .build();
        try {
            NodeUtils.addMethod(conveyor, server, methodNode, new ChangeConveyorModeMethod());
        } catch (Exception e) {
            logger.error("Error creating ChangeConveyorModeMethod() method.", e);
        }
    }


}

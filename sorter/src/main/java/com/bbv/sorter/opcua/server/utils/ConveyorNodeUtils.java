package com.bbv.sorter.opcua.server.utils;

import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.MultiStateDiscreteNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

/**
 * Created by lorenzodemicheli on 16.03.2018.
 */
public interface ConveyorNodeUtils {
    LocalizedText MODE_STARTED = LocalizedText.english("STARTED");
    LocalizedText MODE_STOPPED = LocalizedText.english("STOPPED");
    LocalizedText[] modes = {MODE_STARTED, MODE_STOPPED};

    static AttributeDelegate getModeAttributeDelegate() {
        return new AttributeDelegate() {
            @Override
            public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                MultiStateDiscreteNode multiStateDiscreteNode = (MultiStateDiscreteNode) node;
                LocalizedText[] enumStrings = multiStateDiscreteNode.getEnumStrings();
                if (ConveyorFactory.createConveyor().getMode()){
                    return new DataValue(new Variant(ConveyorNodeUtils.MODE_STARTED));
                }
                return new DataValue(new Variant(ConveyorNodeUtils.MODE_STOPPED));

            }
        };
    }

    static AttributeDelegate getStatusAttributeDelegate() {
        return new AttributeDelegate() {
            @Override
            public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                return new DataValue(new Variant(ConveyorFactory.createConveyor().getStatus()));
            }
        };
    }

    static UaObjectTypeNode createConveyorTypeNode(  OpcUaServer server, UShort namespaceIndex ) {
        // Define a new ObjectType called "ConveyorType".
        return UaObjectTypeNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/ConveyorType"))
                .setBrowseName(new QualifiedName(namespaceIndex, "ConveyorType"))
                .setDisplayName(LocalizedText.english("ConveyorType"))
                .setDescription(LocalizedText.english("Fisher Conveyor Belt 24V"))
                .setIsAbstract(false)
                .build();
    }

    static UaVariableNode createConveyorStatusInstanceDeclaration(UaObjectTypeNode conveyorTypeNode,OpcUaServer server, UShort namespaceIndex ) {
        UaVariableNode conveyorTypeVariableNodeStatus = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/ConveyorType.Status"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, "Status"))
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

    static UaVariableNode createConveyorModeInstanceDeclaration(UaObjectTypeNode conveyorTypeNode,OpcUaServer server, UShort namespaceIndex ) {
        // "Mode" and "Status" are members. These nodes are what are called "instance declarations" by the spec.
        UaVariableNode conveyorTypeVariableNodeMode = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/ConveyorType.Mode"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, "Mode"))
                .setDisplayName(LocalizedText.english("Mode"))
                .setDescription(LocalizedText.english("The Mode , Started/Stopped"))
                .setDataType(Identifiers.ObjectNode)
                .setTypeDefinition(Identifiers.MultiStateDiscreteType)
                .build();

        conveyorTypeVariableNodeMode.setValue(new DataValue(new Variant(ConveyorNodeUtils.MODE_STOPPED)));
        conveyorTypeVariableNodeMode.setMinimumSamplingInterval(1.0);
        conveyorTypeNode.addComponent(conveyorTypeVariableNodeMode);
        return conveyorTypeVariableNodeMode;
    }
}

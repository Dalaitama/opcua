package com.bbv.sorter.opcua.server.utils;

import com.bbv.sorter.hardware.conveyor.Conveyor;
import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import com.bbv.sorter.opcua.server.ValueLoggingDelegate;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegateChain;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

import java.util.function.Consumer;
import java.util.function.Function;

import static com.bbv.sorter.opcua.server.utils.NodePredicates.isEqualVariableNode;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;


public interface LightBarrierUtils {


    static void addLightBarriers(UaObjectNode conveyor, OpcUaServer server, UShort namespaceIndex ) {
        addLightBarrier(conveyor, server, namespaceIndex, "Sorter/Conveyor/LB1", "LB1 Handover Pickup Position Conveyor", "Handover of Color Buttons", getReadLBDelegate(Conveyor::readLightBarrier1));
        addLightBarrier(conveyor, server, namespaceIndex, "Sorter/Conveyor/LB2", "LB2 Control Position Conveyor", "Control Position after detection of Color Buttons", getReadLBDelegate(Conveyor::readLightBarrier2));
        addLightBarrier(conveyor, server, namespaceIndex, "Sorter/Conveyor/LB3", "LB3 Reject Position Conveyor", "Reject position of bad read Buttons", getReadLBDelegate(Conveyor::readLightBarrier3));

    }

    static void addLightBarrier(UaObjectNode conveyor, OpcUaServer server, UShort namespaceIndex, String nodeIdentifier, String name, String description, AttributeDelegate delegate) {

        UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, nodeIdentifier))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, name))
                .setDisplayName(LocalizedText.english(name))
                .setDataType(Identifiers.Boolean)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .setMinimumSamplingInterval(1.0)
                .build();

        node.setValue(new DataValue(new Variant(false)));

        node.setAttributeDelegate(delegate);

        server.getNodeMap().addNode(node);
        conveyor.addComponent(node);
    }

    static AttributeDelegate getReadLBDelegate(Function<Conveyor,Boolean> consumer){
        return
                new AttributeDelegate() {
                    @Override
                    public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                        return new DataValue(new Variant(consumer.apply(ConveyorFactory.getInstance())));
                    }
                };

    }




}

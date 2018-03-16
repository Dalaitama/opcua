package com.bbv.sorter.opcua.server.utils;

import com.bbv.sorter.hardware.conveyor.Conveyor;
import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import com.bbv.sorter.opcua.server.SorterNamespace;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.WriteMask;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.AnalogItemNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.structured.EUInformation;
import org.eclipse.milo.opcua.stack.core.types.structured.Range;
import org.eclipse.milo.opcua.stack.core.util.Unit;

import java.awt.font.NumericShaper;
import java.util.function.Consumer;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;


public interface ConveyorSpeedUtils {




    static AttributeDelegate getSpeedDelegate() {
        return new AttributeDelegate() {
            @Override
            public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                return new DataValue(new Variant(ConveyorFactory.getInstance().readSpeed()));
            }
        };
    }


    @Deprecated
    static UaVariableNode addSpeedInticatorInstanceDeclaration(UaObjectTypeNode conveyorTypeNode, OpcUaServer server, UShort namespaceIndex ) {

        NodeId nodeId = new NodeId(namespaceIndex, "Sorter/Conveyor.SpeedIndicator");
        UByte ubyte = ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE));
        uint(WriteMask.WriteMask.getValue());
        QualifiedName browseName = new QualifiedName(namespaceIndex, "SpeedIndicator");
        LocalizedText display = LocalizedText.english("Speed Indicator");
        LocalizedText description = LocalizedText.english("Speed Indicator");


        UaVariableNode speedIndicatorType = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(nodeId)
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(browseName)
                .setDisplayName(display)
                .setDescription(description)
                .setDataType(Identifiers.Double)
                .setTypeDefinition(Identifiers.AnalogItemType)
                .setHistorizing(true)
                .build();

        speedIndicatorType.setValue(new DataValue(new Variant(0.0)));
        speedIndicatorType.setMinimumSamplingInterval(1.0);

        conveyorTypeNode.addComponent(speedIndicatorType);
        return speedIndicatorType;
    }


}

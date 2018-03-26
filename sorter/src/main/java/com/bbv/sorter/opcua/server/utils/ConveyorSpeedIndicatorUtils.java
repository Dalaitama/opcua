package com.bbv.sorter.opcua.server.utils;

import com.bbv.sorter.hardware.conveyor.Conveyor;
import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import com.bbv.sorter.opcua.server.SorterNamespace;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
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
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.structured.EUInformation;
import org.eclipse.milo.opcua.stack.core.types.structured.Range;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;


public interface ConveyorSpeedIndicatorUtils {


    String BROWSE_NAME_SPEED_INDICATOR = "SpeedIndicator";

    static UaVariableNode addSpeedIndicatorInstanceDeclaration(UaObjectTypeNode conveyorTypeNode, OpcUaServer server, UShort namespaceIndex) {

        NodeId nodeId = new NodeId(namespaceIndex, "Sorter/Conveyor.SpeedIndicator");
        UByte accessLevel = ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE));
        QualifiedName browseName = new QualifiedName(namespaceIndex, BROWSE_NAME_SPEED_INDICATOR);
        LocalizedText display = LocalizedText.english("Speed Indicator");
        LocalizedText description = LocalizedText.english("Speed Indicator");
        Reference typeDefinition = new Reference(nodeId, Identifiers.HasTypeDefinition, new ExpandedNodeId(Identifiers.AnalogItemType), NodeClass.VariableType, true);

        AnalogItemNode speedIndicatorType = new AnalogItemNode(server.getNodeMap(), nodeId, browseName, display, description, uint(0), uint(0));
        speedIndicatorType.setMinimumSamplingInterval(1.0);
        speedIndicatorType.setDataType(Identifiers.UInteger);
        speedIndicatorType.addReference(typeDefinition);
        speedIndicatorType.setEngineeringUnits(new EUInformation(SorterNamespace.NAMESPACE_URI, 177, LocalizedText.english("cm/s"), LocalizedText.english("cm per second as UN/CEFACT Recomendation 20")));
        speedIndicatorType.setInstrumentRange(new Range(1.0, 10.0));
        speedIndicatorType.setEURange(new Range(1.0, Double.MAX_VALUE));
        speedIndicatorType.setValue(new DataValue(new Variant(0.0)));


        conveyorTypeNode.addComponent(speedIndicatorType);
        return speedIndicatorType;
    }

    static void enhanceSpeedIndicatorInstance(UaObjectNode conveyor) {
        conveyor.getComponentNodes().stream()
                .filter(NodePredicates.isEqualVariableNode(BROWSE_NAME_SPEED_INDICATOR, AnalogItemNode.class)).map(x -> ((AnalogItemNode) x))
                .forEach((AnalogItemNode speedIndicatorNode) -> {
                    speedIndicatorNode.setDisplayName(LocalizedText.english("S1 Speed Indicator"));
                    speedIndicatorNode.setEngineeringUnits(new EUInformation(SorterNamespace.NAMESPACE_URI, 177, LocalizedText.english("cm/s"), LocalizedText.english("cm per second as UN/CEFACT Recomendation 20")));
                    speedIndicatorNode.setInstrumentRange(new Range(1.0, 10.0));
                    speedIndicatorNode.setEURange(new Range(1.0, Double.MAX_VALUE));
                    speedIndicatorNode.setValue(new DataValue(new Variant(0.0)));
                    speedIndicatorNode.setAttributeDelegate(ConveyorNodeUtils.getAttributeDelegate(Conveyor::readSpeed));
                });
    }


}

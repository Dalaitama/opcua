package com.bbv.sorter.opcua.server.utils;

import com.bbv.sorter.hardware.conveyor.Conveyor;
import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.MultiStateDiscreteNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UByte;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;

import static com.bbv.sorter.opcua.server.utils.NodePredicates.isEqualVariableNode;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

/**
 * Created by lorenzodemicheli on 18.03.2018.
 */
public interface ColorDetectorUtils {


    LocalizedText NO_READ = LocalizedText.english("NO READ");
    LocalizedText WHITE = LocalizedText.english("WHITE");
    LocalizedText YELLOW = LocalizedText.english("YELLOW");
    LocalizedText RED = LocalizedText.english("RED");
    LocalizedText GREEN = LocalizedText.english("GREEN");
    LocalizedText BLUE = LocalizedText.english("BLUE");
    LocalizedText BLACK = LocalizedText.english("BLACK");
    LocalizedText[] COLOR_READS = {NO_READ, WHITE, YELLOW, RED, GREEN, BLUE, BLACK};
    String BROWSE_NAME_TYPE_COLOR_DETECTOR = "ColorDetector";



    static UaVariableNode addColorDetector(UaObjectNode conveyor, OpcUaServer server, UShort namespaceIndex) {

        NodeId nodeId = new NodeId(namespaceIndex, "Sorter/Conveyor/ColorDetector");
        UByte accessLevel = ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE));
        QualifiedName browseName = new QualifiedName(namespaceIndex, BROWSE_NAME_TYPE_COLOR_DETECTOR);
        LocalizedText display = LocalizedText.english("CS1 Color Detector");
        LocalizedText description = LocalizedText.english("Fisher Color Detector");
        Reference typeDefinition = new Reference(nodeId, Identifiers.HasTypeDefinition, new ExpandedNodeId(Identifiers.MultiStateDiscreteType), NodeClass.VariableType, true);

        MultiStateDiscreteNode compressorTypeColors = new MultiStateDiscreteNode(server.getNodeMap(), nodeId, browseName, display, description, uint(0), uint(0));
        compressorTypeColors.setValue(new DataValue(new Variant(NO_READ)));
        compressorTypeColors.setMinimumSamplingInterval(1.0);
        compressorTypeColors.setEnumStrings(COLOR_READS);
        compressorTypeColors.setDataType(Identifiers.UInteger);
        compressorTypeColors.addReference(typeDefinition);
        compressorTypeColors.setAttributeDelegate(ConveyorNodeUtils.getAttributeDelegate(Conveyor::getLastProcessedColor));

        conveyor.addComponent(compressorTypeColors);
        return compressorTypeColors;
    }


}

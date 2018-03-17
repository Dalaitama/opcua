package com.bbv.sorter.opcua.server.utils;

import com.bbv.sorter.hardware.conveyor.Conveyor;
import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import com.bbv.sorter.opcua.server.SorterNamespace;
import com.bbv.sorter.opcua.server.methods.ChangeConveyorModeMethod;
import com.bbv.sorter.opcua.server.methods.PressurizeValveModeMethod;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.AnalogItemNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.MultiStateDiscreteNode;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.structured.EUInformation;
import org.eclipse.milo.opcua.stack.core.types.structured.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

import static com.bbv.sorter.opcua.server.utils.NodePredicates.isEqualVariableNode;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

/**
 * Created by lorenzodemicheli on 16.03.2018.
 */
public interface CompressorNodeUtils {
    String V1 = "V1";
    String V2 = "V2";
    String V3 = "V3";
    String[] VALVES = {V1, V2, V3};
    Logger logger = LoggerFactory.getLogger(CompressorNodeUtils.class);


    static UaObjectTypeNode createCompressorTypeNode(OpcUaServer server, UShort namespaceIndex) {
        return UaObjectTypeNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/CompressorType"))
                .setBrowseName(new QualifiedName(namespaceIndex, "CompressorType"))
                .setDisplayName(LocalizedText.english("ConveyorType"))
                .setDescription(LocalizedText.english("Pneumatic Compressor"))
                .setIsAbstract(false)
                .build();
    }


    static UaVariableNode addCompressorValveInstanceDeclaration(UaObjectTypeNode compressorTypeNode, OpcUaServer server, UShort namespaceIndex, String valveIdentifier) {
        UaVariableNode valveTypeVariableNodeMode = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/CompressorType." + valveIdentifier))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, valveIdentifier))
                .setDisplayName(LocalizedText.english(valveIdentifier + " Valve"))
                .setDescription(LocalizedText.english("The Valve , On/Off"))
                .setDataType(Identifiers.Boolean)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .setHistorizing(false)
                .build();

        valveTypeVariableNodeMode.setValue(new DataValue(new Variant(false)));
        valveTypeVariableNodeMode.setMinimumSamplingInterval(1.0);
        compressorTypeNode.addComponent(valveTypeVariableNodeMode);
        return valveTypeVariableNodeMode;
    }


    static UaObjectNode createCompressorInstance(UaObjectTypeNode conveyorTypeNode, UaVariableNode valve1, UaVariableNode valve2, UaVariableNode valve3, NodeFactory nodeFactory, UShort namespaceIndex) {
        // Use NodeFactory to create instance of ConveyorType called "Conveyor".
        // NodeFactory takes care of recursively instantiating MyObject member nodes
        // as well as adding all nodes to the address space.
        UaObjectNode conveyor = nodeFactory.createObject(
                new NodeId(namespaceIndex, "Sorter/Compressor"),
                new QualifiedName(namespaceIndex, "Compressor"),
                LocalizedText.english("Compressor"),
                conveyorTypeNode.getNodeId()
        );


        conveyor.getComponentNodes().stream()
                .filter(isEqualVariableNode(valve1, UaVariableNode.class)).map(x -> ((UaVariableNode) x))
                .forEach(variable -> variable.setAttributeDelegate(ConveyorNodeUtils.getBooleanConveyorDelegate(Conveyor::readValve1)));
        conveyor.getComponentNodes().stream()
                .filter(isEqualVariableNode(valve2, UaVariableNode.class)).map(x -> ((UaVariableNode) x))
                .forEach(variable -> variable.setAttributeDelegate(ConveyorNodeUtils.getBooleanConveyorDelegate(Conveyor::readValve2)));
        conveyor.getComponentNodes().stream()
                .filter(isEqualVariableNode(valve3, UaVariableNode.class)).map(x -> ((UaVariableNode) x))
                .forEach(variable -> variable.setAttributeDelegate(ConveyorNodeUtils.getBooleanConveyorDelegate(Conveyor::readValve3)));


        return conveyor;
    }

    static void addChangeConveyorMethodNode(UaObjectNode conveyor, OpcUaServer server, UShort namespaceIndex) {
        UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "Sorter/Compressor/PressurizeValve"))
                .setBrowseName(new QualifiedName(namespaceIndex, "PressurizeValve"))
                .setDisplayName(new LocalizedText(null, "C1 Pressurize a Valve"))
                .setDescription(LocalizedText.english("On/Off the pressure on the Valve"))
                .build();


        try {
            NodeUtils.addMethod(conveyor, server, methodNode, new PressurizeValveModeMethod());

        } catch (Exception e) {
            logger.error("Error creating PressurizeValveModeMethod() method.", e);
        }
    }


}

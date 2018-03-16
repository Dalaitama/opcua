package com.bbv.sorter.opcua.server.utils;

import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import com.bbv.sorter.opcua.server.SorterNamespace;
import com.bbv.sorter.opcua.server.methods.ChangeConveyorModeMethod;
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

import static com.bbv.sorter.opcua.server.utils.NodePredicates.isEqualVariableNode;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.ubyte;

/**
 * Created by lorenzodemicheli on 16.03.2018.
 */
// TODO convert into class.
public interface ConveyorNodeUtils {
    LocalizedText MODE_STARTED = LocalizedText.english("STARTED");
    LocalizedText MODE_STOPPED = LocalizedText.english("STOPPED");
    LocalizedText[] MODES = {MODE_STARTED, MODE_STOPPED};
    Logger logger = LoggerFactory.getLogger(ConveyorNodeUtils.class);
    String CONVEYOR_MODE_QUALIFIEDNAME = "Mode";
    String CONVEYOR_CHANGE_MODE_QUALIFIEDNAME = "Change Mode";

    static AttributeDelegate getModeAttributeDelegate() {
        return new AttributeDelegate() {
            @Override
            public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                MultiStateDiscreteNode multiStateDiscreteNode = (MultiStateDiscreteNode) node;
                LocalizedText[] enumStrings = multiStateDiscreteNode.getEnumStrings();
                if (ConveyorFactory.getInstance().getMode()) {
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
                return new DataValue(new Variant(ConveyorFactory.getInstance().getStatus()));
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


    static UaVariableNode addConveyorStatusInstanceDeclaration(UaObjectTypeNode conveyorTypeNode, OpcUaServer server, UShort namespaceIndex) {
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


    static UaVariableNode addConveyorModeInstanceDeclaration(UaObjectTypeNode conveyorTypeNode, OpcUaServer server, UShort namespaceIndex) {
        // "Mode" and "Status" are members. These nodes are what are called "instance declarations" by the spec.
        UaVariableNode conveyorTypeVariableNodeMode = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/ConveyorType.Mode"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, CONVEYOR_MODE_QUALIFIEDNAME))
                .setDisplayName(LocalizedText.english("Mode"))
                .setDescription(LocalizedText.english("The Mode , Started/Stopped"))
                .setDataType(Identifiers.ObjectNode)
                .setTypeDefinition(Identifiers.MultiStateDiscreteType)
                .setHistorizing(true)
                .build();

        conveyorTypeVariableNodeMode.setValue(new DataValue(new Variant(ConveyorNodeUtils.MODE_STOPPED)));
        conveyorTypeVariableNodeMode.setMinimumSamplingInterval(1.0);
        conveyorTypeNode.addComponent(conveyorTypeVariableNodeMode);
        return conveyorTypeVariableNodeMode;
    }


    @Deprecated
    static UaMethodNode addChangeConveyorMethodNodeInstanceDefinition(UaObjectTypeNode conveyorTypeNode, OpcUaServer server, UShort namespaceIndex) {
        UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/ConveyorType.ChangeMode"))
                .setBrowseName(new QualifiedName(namespaceIndex, CONVEYOR_CHANGE_MODE_QUALIFIEDNAME))
                .setDisplayName(new LocalizedText(null, "Change Mode"))
                .setDescription(LocalizedText.english("Start Stop the Conveyor"))
                .build();


        try {
            AnnotationBasedInvocationHandler invocationHandler =
                    AnnotationBasedInvocationHandler.fromAnnotatedObject(
                            server.getNodeMap(), new ChangeConveyorModeMethod());

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);


            conveyorTypeNode.addReference(new Reference(
                    conveyorTypeNode.getNodeId(),
                    Identifiers.HasComponent,
                    methodNode.getNodeId().expanded(),
                    methodNode.getNodeClass(),
                    true
            ));

            methodNode.addReference(new Reference(
                    methodNode.getNodeId(),
                    Identifiers.HasComponent,
                    conveyorTypeNode.getNodeId().expanded(),
                    conveyorTypeNode.getNodeClass(),
                    false
            ));
            conveyorTypeNode.addComponent(methodNode);

        } catch (Exception e) {
            logger.error("Error creating ChangeConveyorModeMethod() method.", e);
        }
        return methodNode;
    }

    static UaObjectNode createConveyorInstance(UaObjectTypeNode conveyorTypeNode, UaVariableNode conveyorTypeVariableNodeMode, UaVariableNode conveyorTypeVariableNodeStatus,UaVariableNode conveyorSpeedVariableNodeStatus, NodeFactory nodeFactory, UShort namespaceIndex) {
        // Use NodeFactory to create instance of ConveyorType called "Conveyor".
        // NodeFactory takes care of recursively instantiating MyObject member nodes
        // as well as adding all nodes to the address space.
        UaObjectNode conveyor = nodeFactory.createObject(
                new NodeId(namespaceIndex, "Sorter/Conveyor"),
                new QualifiedName(namespaceIndex, "Conveyor"),
                LocalizedText.english("Conveyor"),
                conveyorTypeNode.getNodeId()
        );


        conveyor.getComponentNodes().stream()
                .filter(isEqualVariableNode(conveyorTypeVariableNodeMode, MultiStateDiscreteNode.class)).map(x -> ((MultiStateDiscreteNode) x))
                .forEach((MultiStateDiscreteNode multiStateDiscreteNode) -> {
                    multiStateDiscreteNode.setEnumStrings(ConveyorNodeUtils.MODES);
                    multiStateDiscreteNode.setAttributeDelegate(ConveyorNodeUtils.getModeAttributeDelegate());
                });
        conveyor.getComponentNodes().stream()
                .filter(isEqualVariableNode(conveyorTypeVariableNodeStatus, UaVariableNode.class)).map(x -> ((UaVariableNode) x))
                .forEach(variable -> variable.setAttributeDelegate(ConveyorNodeUtils.getStatusAttributeDelegate()));

        conveyor.getComponentNodes().stream()
                .filter(isEqualVariableNode(conveyorSpeedVariableNodeStatus, AnalogItemNode.class)).map(x -> ((AnalogItemNode) x))
                .forEach((AnalogItemNode speedIndicatorNode) -> {
                    speedIndicatorNode.setEngineeringUnits(new EUInformation(SorterNamespace.NAMESPACE_URI,   177, LocalizedText.english("cm/s"), LocalizedText.english("cm per second as UN/CEFACT Recomendation 20")) );
                    speedIndicatorNode.setInstrumentRange(new Range(1.0,10.0));
                    speedIndicatorNode.setEURange(new Range(1.0,Double.MAX_VALUE));
                    speedIndicatorNode.setValue(new DataValue(new Variant(0.0)));
                    speedIndicatorNode.setAttributeDelegate(ConveyorSpeedUtils.getSpeedDelegate());
                });


        return conveyor;
    }


    static void addChangeConveyorMethodNode(UaObjectNode conveyor, OpcUaServer server, UShort namespaceIndex) {
        UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "Sorter/Conveyor/ChangeMode"))
                .setBrowseName(new QualifiedName(namespaceIndex, CONVEYOR_CHANGE_MODE_QUALIFIEDNAME))
                .setDisplayName(new LocalizedText(null, "Change Mode"))
                .setDescription(LocalizedText.english("Start Stop the Conveyor"))
                .build();


        try {
            AnnotationBasedInvocationHandler invocationHandler =
                    AnnotationBasedInvocationHandler.fromAnnotatedObject(
                            server.getNodeMap(), new ChangeConveyorModeMethod());

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            server.getNodeMap().addNode(methodNode);
            conveyor.addComponent(methodNode);


            conveyor.addReference(new Reference(
                    conveyor.getNodeId(),
                    Identifiers.HasComponent,
                    methodNode.getNodeId().expanded(),
                    methodNode.getNodeClass(),
                    true
            ));

            methodNode.addReference(new Reference(
                    methodNode.getNodeId(),
                    Identifiers.HasComponent,
                    conveyor.getNodeId().expanded(),
                    conveyor.getNodeClass(),
                    false
            ));

        } catch (Exception e) {
            logger.error("Error creating ChangeConveyorModeMethod() method.", e);
        }
    }

}

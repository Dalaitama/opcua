/*
 * Copyright (c) 2016 Kevin Herron
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.html.
 */

package com.bbv.sorter.opcua.server;

import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import com.bbv.sorter.opcua.server.methods.ChangeConveyorModeMethod;
import com.google.common.collect.Lists;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRank;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.*;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.AnalogItemNode;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegateChain;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.OpcUaBinaryDataTypeDictionary;
import org.eclipse.milo.opcua.stack.core.types.OpcUaDataTypeManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.Range;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.eclipse.milo.opcua.stack.core.util.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bbv.sorter.opcua.server.types.CustomDataType;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.bbv.sorter.opcua.server.utils.NodePredicates.isEqualVariableNode;
import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.*;

public class SorterNamespace implements Namespace {

    public static final String NAMESPACE_URI = "urn:bbv:fischer:color-sorter";


    private final Logger logger = LoggerFactory.getLogger(getClass());


    private final SubscriptionModel subscriptionModel;

    private final NodeFactory nodeFactory;

    private final OpcUaServer server;
    private final UShort namespaceIndex;

    public SorterNamespace(OpcUaServer server, UShort namespaceIndex) {
        this.server = server;
        this.namespaceIndex = namespaceIndex;

        subscriptionModel = new SubscriptionModel(server, this);

        nodeFactory = new NodeFactory(
                server.getNodeMap(),
                server.getObjectTypeManager(),
                server.getVariableTypeManager()
        );

        try {
            UaFolderNode sorterFolder = createSorterFolder(server, namespaceIndex);
            UaObjectNode instance = addConveyorObjectTypeAndInstance(sorterFolder);
            addMethodNode(instance);
        } catch (UaException e) {
            logger.error("Error adding nodes: {}", e.getMessage(), e);
        }
    }

    private UaObjectNode addConveyorObjectTypeAndInstance(UaFolderNode sorterFolder) throws UaException {
        // Define a new ObjectType called "ConveyorType".
        UaObjectTypeNode conveyorTypeNode = UaObjectTypeNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/ConveyorType"))
                .setBrowseName(new QualifiedName(namespaceIndex, "ConveyorType"))
                .setDisplayName(LocalizedText.english("ConveyorType"))
                .setDescription(LocalizedText.english("Depict a Conveyor of Fisher Model xxyy"))
                .setIsAbstract(false)
                .build();

        // "Mode" and "Status" are members. These nodes are what are called "instance declarations" by the spec.
        UaVariableNode conveyorTypeVariableNodeMode = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/ConveyorType.Mode"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, "Mode"))
                .setDisplayName(LocalizedText.english("Mode"))
                .setDescription(LocalizedText.english("The Mode , Started/Stopped"))
                .setDataType(Identifiers.Boolean)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        conveyorTypeVariableNodeMode.setValue(new DataValue(new Variant(false)));
        conveyorTypeVariableNodeMode.setMinimumSamplingInterval(1.0);

        conveyorTypeNode.addComponent(conveyorTypeVariableNodeMode);


        UaVariableNode conveyorTypeVariableNodeStatus = UaVariableNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "ObjectTypes/ConveyorType.Status"))
                .setAccessLevel(ubyte(AccessLevel.getMask(AccessLevel.READ_WRITE)))
                .setBrowseName(new QualifiedName(namespaceIndex, "Status"))
                .setDisplayName(LocalizedText.english("Status"))
                .setDescription(LocalizedText.english("The Status , On/Off"))
                .setDataType(Identifiers.Boolean)
                .setTypeDefinition(Identifiers.BaseDataVariableType)
                .build();

        conveyorTypeVariableNodeStatus.setValue(new DataValue(new Variant(false)));
        conveyorTypeVariableNodeStatus.setMinimumSamplingInterval(1.0);
        conveyorTypeNode.addComponent(conveyorTypeVariableNodeStatus);

        // Tell the ObjectTypeManager about our new type.
        // This let's us use NodeFactory to instantiate instances of the type.
        server.getObjectTypeManager().registerObjectType(
                conveyorTypeNode.getNodeId(),
                UaObjectNode.class,
                UaObjectNode::new
        );

        // Add our ObjectTypeNode as a subtype of BaseObjectType.
        server.getUaNamespace().addReference(
                Identifiers.BaseObjectType,
                Identifiers.HasSubtype,
                true,
                conveyorTypeNode.getNodeId().expanded(),
                NodeClass.ObjectType
        );

        // Add the inverse SubtypeOf relationship.
        conveyorTypeNode.addReference(new Reference(
                conveyorTypeNode.getNodeId(),
                Identifiers.HasSubtype,
                Identifiers.BaseObjectType.expanded(),
                NodeClass.ObjectType,
                false
        ));

        // Add it into the address space.
        server.getNodeMap().addNode(conveyorTypeNode);

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
                .filter(isEqualVariableNode(conveyorTypeVariableNodeMode))
                .forEach(variable -> ((UaVariableNode) variable).setAttributeDelegate(getModeAttributeDelegate()));
        conveyor.getComponentNodes().stream()
                .filter(isEqualVariableNode(conveyorTypeVariableNodeStatus))
                .forEach(variable -> ((UaVariableNode) variable).setAttributeDelegate(getStatusAttributeDelegate()));


        // Add forward and inverse references from the root folder.
        sorterFolder.addOrganizes(conveyor);

        conveyor.addReference(new Reference(
                conveyor.getNodeId(),
                Identifiers.Organizes,
                sorterFolder.getNodeId().expanded(),
                sorterFolder.getNodeClass(),
                false
        ));

        return conveyor;
    }


    private UaFolderNode createSorterFolder(OpcUaServer server, UShort namespaceIndex) throws UaException {
        // Create a "Sorter" folder and add it to the node manager
        NodeId folderNodeId = new NodeId(namespaceIndex, "Sorter");

        UaFolderNode folderNode = new UaFolderNode(
                server.getNodeMap(),
                folderNodeId,
                new QualifiedName(namespaceIndex, "Sorter"),
                LocalizedText.english("Sorter")
        );

        server.getNodeMap().addNode(folderNode);

        // Make sure our new folder shows up under the server's Objects folder
        server.getUaNamespace().addReference(
                Identifiers.ObjectsFolder,
                Identifiers.Organizes,
                true,
                folderNodeId.expanded(),
                NodeClass.Object
        );
        return folderNode;
    }

    private void addMethodNode(UaObjectNode objectNode) {
        UaMethodNode methodNode = UaMethodNode.builder(server.getNodeMap())
                .setNodeId(new NodeId(namespaceIndex, "Sorter/Conveyor/start"))
                .setBrowseName(new QualifiedName(namespaceIndex, "start"))
                .setDisplayName(new LocalizedText(null, "start"))
                .setDescription(
                        LocalizedText.english("Start the Conveyor"))
                .build();


        try {
            AnnotationBasedInvocationHandler invocationHandler =
                    AnnotationBasedInvocationHandler.fromAnnotatedObject(
                            server.getNodeMap(), new ChangeConveyorModeMethod());

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            server.getNodeMap().addNode(methodNode);

            objectNode.addReference(new Reference(
                    objectNode.getNodeId(),
                    Identifiers.HasComponent,
                    methodNode.getNodeId().expanded(),
                    methodNode.getNodeClass(),
                    true
            ));

            methodNode.addReference(new Reference(
                    methodNode.getNodeId(),
                    Identifiers.HasComponent,
                    objectNode.getNodeId().expanded(),
                    objectNode.getNodeClass(),
                    false
            ));
        } catch (Exception e) {
            logger.error("Error creating sqrt() method.", e);
        }
    }

    private AttributeDelegate getModeAttributeDelegate() {
        return new AttributeDelegate() {
            @Override
            public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                return new DataValue(new Variant(ConveyorFactory.createConveyor().getMode()));
            }
        };
    }

    private AttributeDelegate getStatusAttributeDelegate() {
        return new AttributeDelegate() {
            @Override
            public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                return new DataValue(new Variant(ConveyorFactory.createConveyor().getStatus()));
            }
        };
    }

    @Override
    public UShort getNamespaceIndex() {
        return namespaceIndex;
    }

    @Override
    public String getNamespaceUri() {
        return NAMESPACE_URI;
    }


    @Override
    public CompletableFuture<List<Reference>> browse(AccessContext context, NodeId nodeId) {
        ServerNode node = server.getNodeMap().get(nodeId);

        if (node != null) {
            return CompletableFuture.completedFuture(node.getReferences());
        } else {
            return FutureUtils.failedFuture(new UaException(StatusCodes.Bad_NodeIdUnknown));
        }
    }

    @Override
    public void read(
            ReadContext context,
            Double maxAge,
            TimestampsToReturn timestamps,
            List<ReadValueId> readValueIds) {

        List<DataValue> results = Lists.newArrayListWithCapacity(readValueIds.size());

        for (ReadValueId readValueId : readValueIds) {
            ServerNode node = server.getNodeMap().get(readValueId.getNodeId());

            if (node != null) {
                DataValue value = node.readAttribute(
                        new AttributeContext(context),
                        readValueId.getAttributeId(),
                        timestamps,
                        readValueId.getIndexRange(),
                        readValueId.getDataEncoding()
                );

                results.add(value);
            } else {
                results.add(new DataValue(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.complete(results);
    }

    @Override
    public void write(WriteContext context, List<WriteValue> writeValues) {
        List<StatusCode> results = Lists.newArrayListWithCapacity(writeValues.size());

        for (WriteValue writeValue : writeValues) {
            ServerNode node = server.getNodeMap().get(writeValue.getNodeId());

            if (node != null) {
                try {
                    node.writeAttribute(
                            new AttributeContext(context),
                            writeValue.getAttributeId(),
                            writeValue.getValue(),
                            writeValue.getIndexRange()
                    );

                    results.add(StatusCode.GOOD);

                    logger.info(
                            "Wrote value {} to {} attribute of {}",
                            writeValue.getValue().getValue(),
                            AttributeId.from(writeValue.getAttributeId()).map(Object::toString).orElse("unknown"),
                            node.getNodeId());
                } catch (UaException e) {
                    logger.error("Unable to write value={}", writeValue.getValue(), e);
                    results.add(e.getStatusCode());
                }
            } else {
                results.add(new StatusCode(StatusCodes.Bad_NodeIdUnknown));
            }
        }

        context.complete(results);
    }

    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

    @Override
    public Optional<MethodInvocationHandler> getInvocationHandler(NodeId methodId) {
        Optional<ServerNode> node = server.getNodeMap().getNode(methodId);

        return node.flatMap(n -> {
            if (n instanceof UaMethodNode) {
                return ((UaMethodNode) n).getInvocationHandler();
            } else {
                return Optional.empty();
            }
        });
    }

}

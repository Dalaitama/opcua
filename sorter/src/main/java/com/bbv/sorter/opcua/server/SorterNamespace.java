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

import com.bbv.sorter.opcua.server.utils.ConveyorNodeUtils;
import com.bbv.sorter.opcua.server.utils.ConveyorSpeedUtils;
import com.bbv.sorter.opcua.server.utils.LightBarrierUtils;
import com.google.common.collect.Lists;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.*;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;
import org.eclipse.milo.opcua.stack.core.types.enumerated.TimestampsToReturn;
import org.eclipse.milo.opcua.stack.core.types.structured.ReadValueId;
import org.eclipse.milo.opcua.stack.core.types.structured.WriteValue;
import org.eclipse.milo.opcua.stack.core.util.FutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.bbv.sorter.opcua.server.utils.NodePredicates.isEqualVariableNode;

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
            UaObjectNode conveyor = createConveyor(sorterFolder);

            organizeInFolder(sorterFolder, conveyor);
        } catch (UaException e) {
            logger.error("Error adding nodes: {}", e.getMessage(), e);
        }
    }

    // Add forward and inverse references from the root folder.
    private void organizeInFolder(UaFolderNode sorterFolder, UaObjectNode conveyor) {
        sorterFolder.addOrganizes(conveyor);

        conveyor.addReference(new Reference(
                conveyor.getNodeId(),
                Identifiers.Organizes,
                sorterFolder.getNodeId().expanded(),
                sorterFolder.getNodeClass(),
                false
        ));
    }

    private UaObjectNode createConveyor(UaFolderNode sorterFolder) throws UaException {
        UaObjectTypeNode conveyorTypeNode = ConveyorNodeUtils.createConveyorTypeNode(server, namespaceIndex);
        UaVariableNode conveyorTypeVariableNodeMode = ConveyorNodeUtils.addConveyorModeInstanceDeclaration(conveyorTypeNode, server, namespaceIndex);
        UaVariableNode conveyorTypeVariableNodeStatus = ConveyorNodeUtils.addConveyorStatusInstanceDeclaration(conveyorTypeNode, server, namespaceIndex);
        // Doesn't work,... ConveyorNodeUtils.addChangeConveyorMethodNodeInstanceDefinition(conveyorTypeNode, server, namespaceIndex);
        UaVariableNode speedInticatorTypeVariable = ConveyorSpeedUtils.addSpeedInticatorInstanceDeclaration(conveyorTypeNode, server, namespaceIndex);
        registerType(conveyorTypeNode, Identifiers.BaseObjectType, Identifiers.HasSubtype, NodeClass.ObjectType);

        UaObjectNode conveyor = ConveyorNodeUtils.createConveyorInstance(conveyorTypeNode, conveyorTypeVariableNodeMode, conveyorTypeVariableNodeStatus, speedInticatorTypeVariable, nodeFactory, namespaceIndex);

        ConveyorNodeUtils.addChangeConveyorMethodNode(conveyor, server, namespaceIndex);
        LightBarrierUtils.addLightBarriers(conveyor, server, namespaceIndex);
        ConveyorSpeedUtils.addConveyorSpeedIndicatorNode(conveyor, server, namespaceIndex);

        return conveyor;
    }


    private void registerType(UaObjectTypeNode conveyorTypeNode, NodeId sourceNodeId, NodeId referenceTypeId, NodeClass nodeClass) throws UaException {
        // Tell the ObjectTypeManager about our new type.
        // This let's us use NodeFactory to instantiate instances of the type.
        server.getObjectTypeManager().registerObjectType(
                conveyorTypeNode.getNodeId(),
                UaObjectNode.class,
                UaObjectNode::new
        );

        // Add our ObjectTypeNode as a subtype of BaseObjectType.
        server.getUaNamespace().addReference(
                sourceNodeId,
                referenceTypeId,
                true,
                conveyorTypeNode.getNodeId().expanded(),
                nodeClass
        );

        // Add the inverse SubtypeOf relationship.
        conveyorTypeNode.addReference(new Reference(
                conveyorTypeNode.getNodeId(),
                referenceTypeId,
                sourceNodeId.expanded(),
                nodeClass,
                false
        ));

        // Add it into the address space.
        server.getNodeMap().addNode(conveyorTypeNode);
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

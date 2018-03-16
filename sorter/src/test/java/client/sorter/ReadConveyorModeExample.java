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

package client.sorter;

import client.ClientExample;
import client.ClientExampleRunner;
import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.client.model.nodes.variables.BaseDataVariableNode;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ReadConveyorModeExample implements ClientExample {

    public static void main(String[] args) throws Exception {
        ReadConveyorModeExample example = new ReadConveyorModeExample();

        new ClientExampleRunner(example, false).run();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        client.connect().get();

        UShort index = client.getNamespaceTable().getIndex("urn:bbv:fischer:color-sorter");
        NodeId nodeId = new NodeId(index, "Sorter/Conveyor.Mode");
        printConveyorMode(client, index, nodeId);

        ConveyorFactory.getInstance().start();

        Thread.sleep(1000);

        printConveyorMode(client, index, nodeId);


        future.complete(client);
    }

    private void printConveyorMode(OpcUaClient client, UShort index, NodeId nodeId) throws InterruptedException, java.util.concurrent.ExecutionException {
        BaseDataVariableNode conveyorModeNode = client.getAddressSpace().getVariableNode(
                nodeId,
                BaseDataVariableNode.class
        ).get();

        CompletableFuture<? extends VariableNode> variableComponent = conveyorModeNode.getVariableComponent(new QualifiedName(index, "Sorter/Conveyor/Mode"));


        logger.error("ServerStatus.conveyorModeNode={}", conveyorModeNode.getValue().get());
    }

}
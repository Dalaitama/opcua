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
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.nodes.Node;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BrowseSorterFolderExample implements ClientExample {

    public static void main(String[] args) throws Exception {
        BrowseSorterFolderExample example = new BrowseSorterFolderExample();

        new ClientExampleRunner(example).run();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        client.connect().get();

        UShort index = client.getNamespaceTable().getIndex("urn:bbv:fischer:color-sorter");
        NodeId nodeId = new NodeId(index, "Sorter");

        // start browsing at root folder
        browseNode("", client, nodeId);

        future.complete(client);
    }

    private void browseNode(String indent, OpcUaClient client, NodeId browseRoot) {
        try {
            List<Node> nodes = client.getAddressSpace().browse(browseRoot).get();

            for (Node node : nodes) {
                logger.info("{} Node={}", indent, node.getBrowseName().get().getName());

                // recursively browse to children
                browseNode(indent + "  ", client, node.getNodeId().get());
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Browsing nodeId={} failed: {}", browseRoot, e.getMessage(), e);
        }
    }

}
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
import com.bbv.sorter.opcua.server.utils.ConveyorNodeUtils;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;
import org.eclipse.milo.opcua.stack.core.types.structured.CallMethodRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

import static org.eclipse.milo.opcua.stack.core.util.ConversionUtil.l;

public class MethodExample implements ClientExample {

    public static void main(String[] args) throws Exception {
        MethodExample example = new MethodExample();



        new ClientExampleRunner(example,false).run();
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception {
        // synchronous connect
        client.connect().get();

        // call the start(x) function
        boolean input = true;
        changeMode(client, future, ConveyorNodeUtils.MODE_STARTED.getText());
        Thread.sleep(1000);
        changeMode(client, future, ConveyorNodeUtils.MODE_STOPPED.getText());
        Thread.sleep(1000);
        changeMode(client, future, ConveyorNodeUtils.MODE_STARTED.getText());
        Thread.sleep(1000);
        changeMode(client, future, ConveyorNodeUtils.MODE_STOPPED.getText());
        Thread.sleep(1000);
        changeMode(client, future, ConveyorNodeUtils.MODE_STARTED.getText());
        Thread.sleep(1000);
        changeMode(client, future, ConveyorNodeUtils.MODE_STOPPED.getText());
        Thread.sleep(1000);
        changeMode(client, future, "banana");
        Thread.sleep(1000);

        future.complete(client);
    }

    private void changeMode(OpcUaClient client, CompletableFuture<OpcUaClient> future, String input) {
        start(client, input).exceptionally(ex -> {
            logger.error("error invoking start()", ex);
            return "Something went wrong";
        }).thenAccept(v -> {
            logger.info("start={}", v);


        });
    }

    private CompletableFuture<String> start(OpcUaClient client, String input) {
        UShort index = client.getNamespaceTable().getIndex("urn:bbv:fischer:color-sorter");
        NodeId objectId = new NodeId(index, "Sorter/Conveyor");
        NodeId methodId = new NodeId(index, "Sorter/Conveyor.ChangeMode");


        CallMethodRequest request = new CallMethodRequest(
                objectId, methodId, new Variant[]{new Variant(input)});
        //    objectId, methodId, null);

        return client.call(request).thenCompose(result -> {
            StatusCode statusCode = result.getStatusCode();

            if (statusCode.isGood()) {
                String value = (String) l(result.getOutputArguments()).get(0).getValue();
                return CompletableFuture.completedFuture(value);
            } else {
                CompletableFuture<String> f = new CompletableFuture<>();
                f.completeExceptionally(new UaException(statusCode));
                return f;
            }
        });
    }

}

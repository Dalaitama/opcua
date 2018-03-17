package com.bbv.sorter.opcua.server.utils;

import com.bbv.sorter.opcua.server.methods.PressurizeValveModeMethod;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.nodes.UaMethodNode;
import org.eclipse.milo.opcua.sdk.server.nodes.UaObjectNode;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by lorenzodemicheli on 16.03.2018.
 */
public interface NodeUtils {
    Logger logger = LoggerFactory.getLogger(NodeUtils.class);

    static void addMethod(UaObjectNode objectNode, OpcUaServer server, UaMethodNode methodNode, Object method) throws Exception {
        try {
            AnnotationBasedInvocationHandler invocationHandler =
                    AnnotationBasedInvocationHandler.fromAnnotatedObject(
                            server.getNodeMap(), method);

            methodNode.setProperty(UaMethodNode.InputArguments, invocationHandler.getInputArguments());
            methodNode.setProperty(UaMethodNode.OutputArguments, invocationHandler.getOutputArguments());
            methodNode.setInvocationHandler(invocationHandler);

            server.getNodeMap().addNode(methodNode);
            objectNode.addComponent(methodNode);


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
            logger.error("Error creating  method {} because of {}.", method.getClass().getSimpleName(), e);
        }

    }
}

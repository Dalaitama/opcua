package com.bbv.sorter.opcua.server.utils;

import org.eclipse.milo.opcua.sdk.server.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;

import java.util.function.Predicate;

/**
 * Created by lorenzodemicheli on 09.03.2018.
 */
public interface NodePredicates {

      static Predicate<Node> isEqualVariableNode(UaVariableNode variableNode) {
        return x -> x.getBrowseName().equals(variableNode.getBrowseName());
    }
}

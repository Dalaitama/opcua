package com.bbv.sorter.opcua.server.utils;

import com.sun.istack.internal.NotNull;
import org.eclipse.milo.opcua.sdk.server.api.nodes.Node;
import org.eclipse.milo.opcua.sdk.server.nodes.UaVariableNode;

import java.util.function.Predicate;

/**
 * Created by lorenzodemicheli on 09.03.2018.
 */
public interface NodePredicates {

    static Predicate<Node> isEqualVariableNode(@NotNull String browseName, Class<? extends Node> clazz) {
        return x -> x.getBrowseName().getName().equals(browseName) && clazz.isInstance(x);
    }
}

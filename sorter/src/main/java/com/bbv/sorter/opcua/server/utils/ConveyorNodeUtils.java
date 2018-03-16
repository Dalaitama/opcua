package com.bbv.sorter.opcua.server.utils;

import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import org.eclipse.milo.opcua.sdk.server.api.nodes.VariableNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.MultiStateDiscreteNode;
import org.eclipse.milo.opcua.sdk.server.nodes.AttributeContext;
import org.eclipse.milo.opcua.sdk.server.nodes.delegates.AttributeDelegate;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

/**
 * Created by lorenzodemicheli on 16.03.2018.
 */
public interface ConveyorNodeUtils {
    LocalizedText MODE_STARTED = LocalizedText.english("STARTED");
    LocalizedText MODE_STOPPED = LocalizedText.english("STOPPED");
    LocalizedText[] modes = {MODE_STARTED, MODE_STOPPED};

    static AttributeDelegate getModeAttributeDelegate() {
        return new AttributeDelegate() {
            @Override
            public DataValue getValue(AttributeContext context, VariableNode node) throws UaException {
                MultiStateDiscreteNode multiStateDiscreteNode = (MultiStateDiscreteNode) node;
                LocalizedText[] enumStrings = multiStateDiscreteNode.getEnumStrings();
                if (ConveyorFactory.createConveyor().getMode()){
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
                return new DataValue(new Variant(ConveyorFactory.createConveyor().getStatus()));
            }
        };
    }
}

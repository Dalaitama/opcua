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

package com.bbv.sorter.opcua.server.methods;

import com.bbv.sorter.hardware.conveyor.ConveyorFactory;
import com.bbv.sorter.opcua.server.utils.ConveyorNodeUtils;
import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.annotations.UaOutputArgument;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.Out;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ChangeConveyorModeMethod {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @UaMethod
    public void invoke(
        InvocationContext context,

        @UaInputArgument(
            name = "mode",
            description = "the wanted mode")
            String mode,

        @UaOutputArgument(
            name = "result",
            description = "True or False.")
            Out<String> result) {

        System.out.println("mode(" + mode.toString() + ")");
        logger.debug("Invoking mode() method of Object '{}'", context.getObjectNode().getBrowseName().getName());

        if (ConveyorNodeUtils.MODE_STARTED.getText().equals(mode)){
            ConveyorFactory.getInstance().start();
            result.set(mode);
        }else if (ConveyorNodeUtils.MODE_STOPPED.getText().equals(mode)){
            ConveyorFactory.getInstance().stop();
            result.set(mode);

        }else{

            result.set(String.format("Mode '%s' Unknown, possible values; %s",mode, Arrays.stream(ConveyorNodeUtils.MODES).map(LocalizedText::getText).collect(Collectors.toList())));
        }



    }

}
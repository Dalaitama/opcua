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
import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.annotations.UaOutputArgument;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.Out;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeConveyorModeMethod {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @UaMethod
    public void invoke(
        InvocationContext context,

        @UaInputArgument(
            name = "mode",
            description = "the wanted mode")
            Boolean start,

        @UaOutputArgument(
            name = "result",
            description = "True or False.")
            Out<Boolean> result) {

        System.out.println("start(" + start.toString() + ")");
        logger.debug("Invoking start() method of Object '{}'", context.getObjectNode().getBrowseName().getName());

        if (start){
            ConveyorFactory.createConveyor().start();
        }else{
            ConveyorFactory.createConveyor().stop();

        }

        result.set(start);

    }

}
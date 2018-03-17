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
import com.bbv.sorter.opcua.server.utils.CompressorNodeUtils;
import org.eclipse.milo.opcua.sdk.server.annotations.UaInputArgument;
import org.eclipse.milo.opcua.sdk.server.annotations.UaMethod;
import org.eclipse.milo.opcua.sdk.server.annotations.UaOutputArgument;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.InvocationContext;
import org.eclipse.milo.opcua.sdk.server.util.AnnotationBasedInvocationHandler.Out;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PressurizeValveModeMethod {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @UaMethod
    public void invoke(
        InvocationContext context,

        @UaInputArgument(
            name = "valveName",
            description = "the wanted valveName")
            String valveName,
        @UaInputArgument(
                name = "pressured",
                description = "Put or Release Pressure on the Valve")
                boolean pressured,

        @UaOutputArgument(
            name = "result",
            description = "True or False.")
            Out<String> result) {

        logger.info("Invoking valveName({},{}) method of Object '{}'",valveName, pressured,context.getObjectNode().getBrowseName().getName());

        result.set("OK");
        switch (valveName){
            case CompressorNodeUtils.V1:  ConveyorFactory.getInstance().setValve1(pressured);
                break;
            case CompressorNodeUtils.V2: ConveyorFactory.getInstance().setValve2(pressured);
                break;
            case CompressorNodeUtils.V3: ConveyorFactory.getInstance().setValve3(pressured);
                break;
                default:  result.set(String.format("Valve '%s' Unknown, possible values; %s",valveName, Arrays.stream(CompressorNodeUtils.VALVES).collect(Collectors.toList())));
        }





    }

}
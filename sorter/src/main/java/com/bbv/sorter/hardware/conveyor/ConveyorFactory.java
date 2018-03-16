package com.bbv.sorter.hardware.conveyor;


public final class ConveyorFactory {



    public static Conveyor getInstance() {
        return  ConveyorMock.getInstance();
    }
}

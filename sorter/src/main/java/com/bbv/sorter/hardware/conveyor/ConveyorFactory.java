package com.bbv.sorter.hardware.conveyor;


public final class ConveyorFactory {



    public static Conveyor createConveyor() {
        return  ConveyorMock.getInstance();
    }
}

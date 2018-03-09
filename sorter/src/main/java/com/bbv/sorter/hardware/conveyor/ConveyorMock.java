package com.bbv.sorter.hardware.conveyor;

/**
 * Mock Class
 */
public class ConveyorMock implements Conveyor {

    private static final Conveyor INSTANCE = new ConveyorMock();

    volatile boolean status = false;
    volatile boolean mode = false;


    private ConveyorMock() {
    }

    static Conveyor getInstance() {
        return INSTANCE;
    }

    @Override
    public boolean getStatus() {
        return status;
    }

    @Override
    public boolean getMode() {
        return mode;
    }

    @Override
    public void start() {
        mode = true;
    }

    @Override
    public void stop() {
        mode = false;
    }
}

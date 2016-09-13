/**
 * This file is part of evdev-java - Java implementation.
 *
 * evdev-java - Java implementation is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * evdev-java - Java implementation is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with evdev-java - Java implementation.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.dgis.input.evdev.devices;

import com.dgis.input.evdev.EventDevice;
import com.dgis.input.evdev.InputEvent;
import com.dgis.input.evdev.InputListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class simplifies using "joystick" type input device (read: anything generating absolute axis and button events)
 * when dealing with an evdev EventDevice.
 * Consolidates multiple events between EV_SYN events, and can tell you how many buttons and axes exist, and which changed.
 * <p/>
 * Copyright (C) 2009 Giacomo Ferrari
 *
 * @author Giacomo Ferrari
 */

public class EvdevJoystickFilter implements InputListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final EventDevice device;
    private JoystickState state;

    private final ArrayList<JoystickListener> listeners = new ArrayList<>();

    /**
     * Holds the event codes for each joystick button, in order. That is, if
     * event code 288 is button one, it is the first entry here.
     */
    private final ArrayList<Integer> buttonEventCodes = new ArrayList<>();

    /**
     * Holds the event codes for each joystick axis, in order. That is, if
     * event code 0 is axis one, it is the first entry here.
     */
    private final ArrayList<Integer> axisEventCodes = new ArrayList<>();

    private boolean[] buttonChanged, axisChanged;

    /**
     * Constructs an EvdevJoystickFilter using the provided EventDevice as input.
     */
    public EvdevJoystickFilter(EventDevice dev) {
        this.device = dev;
        setupDevice();
    }

    /**
     * Constructs an EvdevJoystickFilter using the provided event device as input.
     */
    public EvdevJoystickFilter(File device) throws IOException {
        this(new EventDevice(device));
    }

    private void setupDevice() {
        Map<Integer, List<Integer>> supportedEvents = device.getSupportedEvents();
        List<Integer> supportedAxes = supportedEvents.get((int) InputEvent.EV_ABS);
        List<Integer> supportedKeys = supportedEvents.get((int) InputEvent.EV_KEY);

        int numAxes = supportedAxes == null ? 0 : supportedAxes.size();
        int numButtons = supportedKeys == null ? 0 : supportedKeys.size();

        if (supportedKeys != null) buttonEventCodes.addAll(supportedKeys);
        if (supportedAxes != null) axisEventCodes.addAll(supportedAxes);

        System.out.println("Detected " + buttonEventCodes.size() + " buttons and " + axisEventCodes.size() + " axes.");

        buttonChanged = new boolean[numButtons];
        axisChanged = new boolean[numAxes];

        state = new JoystickState(numButtons, numAxes);
        device.addListener(this);
    }

    @Override
    public void event(InputEvent e) {
        switch (e.type) {
            case EV_KEY:
                handleButton(e.code, e.value > 0);
                break;
            case EV_ABS:
                handleAxis(e.code, e.value);
                break;
            case EV_SYN:
                dispatchEvents();
            default:
                logger.warn("Unknown event {}", e);
        }
    }

    /**
     * Broadcast events for what changed since the last dispatchEvents().
     */
    private void dispatchEvents() {
        boolean anyAxisChanged = false;
        boolean anyButtonChanged = false;
        for (boolean x : buttonChanged) anyButtonChanged |= x;
        for (boolean x : axisChanged) anyAxisChanged |= x;

        for (JoystickListener l : listeners) {
            if (anyButtonChanged)
                l.buttonChanged(buttonChanged, state, device.getDevicePath());
            if (anyAxisChanged)
                l.joystickMoved(axisChanged, state, device.getDevicePath());
        }

        Arrays.fill(axisChanged, false);
        Arrays.fill(buttonChanged, false);
    }

    private void handleAxis(short axisNumber, int value) {
        int axisNumber2 = axisEventCodes.indexOf((int) axisNumber);
        if (axisNumber2 < 0) {
            System.err.println("WARN: Couldn't find axis " + axisNumber + " in mapping! Perhaps device reported capabilities improperly!");
            return;
        }
        axisChanged[axisNumber2] = (value != state.getAxisState(axisNumber2)); //only flag as changed if _actually_ changed.
        state.setAxisState(axisNumber2, value);

    }

    private void handleButton(short buttonNumber, boolean buttonState) {
        int buttonNumber2 = buttonEventCodes.indexOf((int) buttonNumber);
        if (buttonNumber2 < 0) {
            System.err.println("WARN: Couldn't find button " + buttonNumber + " in mapping! Perhaps device reported capabilities improperly!");
            return;
        }
        buttonChanged[buttonNumber2] = (buttonState != state.getButtonState(buttonNumber2)); //only flag as changed if _actually_ changed.
        state.setButtonState(buttonNumber2, buttonState);
    }

    /**
     * Adds an event listener to this device.
     * If the listener is already on the listener list,
     * this method has no effect.
     *
     * @param list The listener to add. Must not be null.
     */
    public void addListener(JoystickListener list) {
        listeners.add(list);
    }

    /**
     * Removes an event listener to this device.
     * If the listener is not on the listener list,
     * this method has no effect.
     *
     * @param list The listener to remove. Must not be null.
     */
    public void removeListener(JoystickListener list) {
        listeners.remove(list);
    }

    public void close() {
        device.close();
    }
}

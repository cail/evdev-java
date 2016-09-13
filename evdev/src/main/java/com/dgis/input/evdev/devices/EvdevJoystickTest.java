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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Simple program that prints info and all events from a device to STDOUT.
 * <p/>
 * Copyright (C) 2009 Giacomo Ferrari
 *
 * @author Giacomo Ferrari
 */

public class EvdevJoystickTest {

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            System.out.println("Usage: EvdevJoystickTest /dev/input/event*");
            System.exit(1);
        }

        File fn = new File(args[0]);
        EvdevJoystickFilter dev = new EvdevJoystickFilter(fn);
        System.out.println("Hit enter to quit.");

        dev.addListener(new JoystickListener() {
            @Override
            public void buttonChanged(boolean[] buttonsChanged,
                                      JoystickState state, String source) {
                System.out.print("Button change: [ ");
                for (boolean b : buttonsChanged) System.out.print((b ? 1 : 0) + " ");
                System.out.println(" ]");
                System.out.println(state);
            }

            @Override
            public void joystickMoved(boolean[] axesChanged, JoystickState state, String source) {
                System.out.print("Axis change: [ ");
                for (boolean b : axesChanged) System.out.print((b ? 1 : 0) + " ");
                System.out.println(" ]");
                System.out.println(state);
            }
        });

        //Wait for newline.
        new BufferedReader(new InputStreamReader(System.in)).readLine();

        dev.close();
    }
}

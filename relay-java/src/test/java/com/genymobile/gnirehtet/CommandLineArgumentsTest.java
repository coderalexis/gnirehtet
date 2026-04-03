/*
 * Copyright (C) 2017 Genymobile
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.genymobile.gnirehtet;

import org.junit.Assert;
import org.junit.Test;

public class CommandLineArgumentsTest {

    private static final int ACCEPT_ALL = CommandLineArguments.PARAM_SERIAL | CommandLineArguments.PARAM_DNS_SERVER
            | CommandLineArguments.PARAM_ROUTES | CommandLineArguments.PARAM_PORT;

    @Test
    public void testNoArgs() {
        CommandLineArguments args = CommandLineArguments.parse(ACCEPT_ALL);
        Assert.assertNull(args.getSerial());
        Assert.assertNull(args.getDnsServers());
    }

    @Test
    public void testSerialOnly() {
        CommandLineArguments args = CommandLineArguments.parse(ACCEPT_ALL, "myserial");
        Assert.assertEquals("myserial", args.getSerial());
        Assert.assertNull(args.getDnsServers());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidParameter() {
        CommandLineArguments.parse(ACCEPT_ALL, "myserial", "other");
    }

    @Test
    public void testDnsServersOnly() {
        CommandLineArguments args = CommandLineArguments.parse(ACCEPT_ALL, "-d", "8.8.8.8");
        Assert.assertNull(args.getSerial());
        Assert.assertEquals("8.8.8.8", args.getDnsServers());
    }

    @Test
    public void testSerialAndDnsServers() {
        CommandLineArguments args = CommandLineArguments.parse(ACCEPT_ALL, "myserial", "-d", "8.8.8.8");
        Assert.assertEquals("myserial", args.getSerial());
        Assert.assertEquals("8.8.8.8", args.getDnsServers());
    }

    @Test
    public void testDnsServersAndSerial() {
        CommandLineArguments args = CommandLineArguments.parse(ACCEPT_ALL, "-d", "8.8.8.8", "myserial");
        Assert.assertEquals("myserial", args.getSerial());
        Assert.assertEquals("8.8.8.8", args.getDnsServers());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSerialWithNoDnsServersParameter() {
        CommandLineArguments.parse(ACCEPT_ALL, "myserial", "-d");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoDnsServersParameter() {
        CommandLineArguments.parse(ACCEPT_ALL, "-d");
    }

    @Test
    public void testRoutesParameter() {
        CommandLineArguments args = CommandLineArguments.parse(ACCEPT_ALL, "-r", "1.2.3.0/24");
        Assert.assertEquals("1.2.3.0/24", args.getRoutes());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoRoutesParameter() {
        CommandLineArguments.parse(ACCEPT_ALL, "-r");
    }

    @Test
    public void testPortParameter() {
        CommandLineArguments args = CommandLineArguments.parse(ACCEPT_ALL, "-p", "1234");
        Assert.assertEquals(1234, args.getPort());
    }

    @Test
    public void testDefaultPort() {
        CommandLineArguments args = CommandLineArguments.parse(ACCEPT_ALL, "myserial");
        Assert.assertEquals(CommandLineArguments.DEFAULT_PORT, args.getPort());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoPortParameter() {
        CommandLineArguments.parse(ACCEPT_ALL, "-p");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPortStringParameter() {
        CommandLineArguments.parse(ACCEPT_ALL, "-p", "abc");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPortZeroParameter() {
        CommandLineArguments.parse(ACCEPT_ALL, "-p", "0");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidPortTooLargeParameter() {
        CommandLineArguments.parse(ACCEPT_ALL, "-p", "65536");
    }
}

/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/
package org.apache.plc4x.java.s7;


import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.authentication.PlcUsernamePasswordAuthentication;
import org.apache.plc4x.java.connection.PlcConnection;
import org.apache.plc4x.java.connection.PlcReader;
import org.apache.plc4x.java.exceptions.PlcConnectionException;
import org.apache.plc4x.java.exceptions.PlcException;
import org.apache.plc4x.java.model.Address;
import org.apache.plc4x.java.model.PlcReadRequest;
import org.apache.plc4x.java.model.PlcReadResponse;
import org.apache.plc4x.java.s7.connection.S7PlcConnection;
import org.apache.plc4x.java.types.ByteType;
import org.apache.plc4x.java.types.Type;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Calendar;
import java.util.concurrent.CompletableFuture;

public class S7PlcDriverTest {

    @Test(groups = {"fast"})
    public void getConnectionTest() throws PlcException {
        S7PlcConnection s7Connection = (S7PlcConnection)
            new PlcDriverManager().getConnection("s7://localhost/1/2");
        Assert.assertEquals(s7Connection.getHostName(), "localhost");
        Assert.assertEquals(s7Connection.getRack(), 1);
        Assert.assertEquals(s7Connection.getSlot(), 2);
    }

    /**
     * In this test case the 's7' driver should report an invalid url format.
     *
     * @throws PlcException something went wrong
     */
    @Test(groups = {"fast"}, expectedExceptions = {PlcConnectionException.class})
    public void getConnectionInvalidUrlTest() throws PlcException {
        new PlcDriverManager().getConnection("s7://localhost/hurz/2");
    }

    /**
     * In this test case the 's7' driver should report an error as this protocol
     * doesn't support authentication.
     *
     * @throws PlcException something went wrong
     */
    @Test(groups = {"fast"}, expectedExceptions = {PlcConnectionException.class})
    public void getConnectionWithAuthenticationTest() throws PlcException {
        new PlcDriverManager().getConnection("s7://localhost/1/2",
            new PlcUsernamePasswordAuthentication("user", "pass"));
    }

    /**
     * Example code do demonstrate using the S7 Plc Driver.
     *
     * @param args ignored.
     * @throws Exception something went wrong.
     */
    public static void main(String[] args) throws Exception {
        try {
            // Create a connection to the S7 PLC.
            PlcConnection plcConnection = new PlcDriverManager().getConnection("s7://192.168.0.1/0/0");
            plcConnection.connect();

            // Check if this connection support reading of data.
            if (plcConnection instanceof PlcReader) {
                PlcReader plcReader = (PlcReader) plcConnection;

                // Prepare some address object for accessing fields in the PLC.
                Address inputs = plcConnection.parseAddress("INPUTS/0");
                Address outputs = plcConnection.parseAddress("OUTPUTS/0");

                //////////////////////////////////////////////////////////
                // Read synchronously ...
                PlcReadResponse<? extends Type> plcReadResponse = plcReader.read(new PlcReadRequest<>(ByteType.class, inputs)).get();
                if (plcReadResponse.getValue() instanceof ByteType) {
                    ByteType data = (ByteType) plcReadResponse.getValue();
                    System.out.println("Inputs: " + data.getValue());
                }

                //////////////////////////////////////////////////////////
                // Read asynchronously ...
                Calendar start = Calendar.getInstance();
                CompletableFuture<PlcReadResponse<? extends Type>> asyncResponse = plcReader.read(
                    new PlcReadRequest<>(ByteType.class, outputs));
                // Simulate doing something else ...
                System.out.println("Processing: ");
                while (true) {
                    Thread.sleep(1);
                    System.out.print(".");
                    if (asyncResponse.isDone()) {
                        break;
                    }
                }
                System.out.println();
                Calendar end = Calendar.getInstance();
                plcReadResponse = asyncResponse.get();
                if (plcReadResponse.getValue() instanceof ByteType) {
                    ByteType data = (ByteType) plcReadResponse.getValue();
                    System.out.println("Outputs: " + data.getValue() + " (in " + (end.getTimeInMillis() - start.getTimeInMillis()) + "ms)");
                }
            }
        }
        // Catch any exception or the application won't be able to finish if something goes wrong.
        catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

}
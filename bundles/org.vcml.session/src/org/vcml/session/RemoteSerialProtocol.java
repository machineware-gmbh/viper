/******************************************************************************
 *                                                                            *
 * Copyright 2018 Jan Henrik Weinstock                                        *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *     http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 *                                                                            *
 ******************************************************************************/

package org.vcml.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public class RemoteSerialProtocol {
	
	private int calcChecksum(String str) {
		int result = 0;
		for (int i = 0; i < str.length(); i++)
			result += (int)str.charAt(i);
		return result & 0xff;
	}
	
	private int calcChecksum(int chr1, int chr2) {
		String txt = "" + (char)chr1 + (char)chr2;
		return Integer.parseInt(txt, 16);
	}
	
	private Socket socket;

	public RemoteSerialProtocol(String host, int port) throws SessionException  {
		try {
			socket = new Socket();
			socket.setTcpNoDelay(true);
			socket.connect(new InetSocketAddress(host, port), 1000);
		} catch (IOException e) {
			throw new SessionException("Failed to connect to session", e);
		}
	}

	public static final String NONE = "n"; /* do nothing */
	public static final String CONT = "c"; /* call sc_start */
	public static final String STOP = "a"; /* call sc_pause */
	public static final String STEP = "s"; /* call sc_start with duration */
	public static final String INFO = "i"; /* retrieve info string */
	public static final String EXEC = "e"; /* execute command on module */
	public static final String TIME = "t"; /* read current time */
	public static final String RDGQ = "q"; /* read global quantum */
	public static final String WRGQ = "Q"; /* write global quantum */
	public static final String GETA = "a"; /* read attribute */
	public static final String SETA = "A"; /* write attribute */
	public static final String QUIT = "x"; /* quit session */
	
	public void send_char(int val) throws SessionException {
		try {
			socket.getOutputStream().write(val);
		} catch (IOException e) {
			throw new SessionException("Failed to contact session", e);
		}
	}
	
	public void send(String message) throws SessionException {
		try {
			// Packet format: $<message>#<8bit-checksum>
			int checksum = calcChecksum(message);
			String packet = "$" + message + "#" + String.format("%02X", checksum);	
			socket.getOutputStream().write(packet.getBytes());
			
			int response = socket.getInputStream().read();
			switch (response) {
				case '+': break; // all good
				case '-': throw new SessionException("Checksum error"); 
				case -1 : throw new SessionException("Disconnected");
				default : throw new SessionException("Invalid response (" + response + ")");	
			}
		} catch (IOException e) {
			throw new SessionException("Failed to contact session", e);
		}
	}
	
	public String recv() throws SessionException {
		try {
			InputStreamReader ireader = new InputStreamReader(socket.getInputStream());
			BufferedReader breader = new BufferedReader(ireader);
			StringBuilder  builder = new StringBuilder();
			
			boolean inside = false;
			int ch;
	
			while ((ch = breader.read()) != -1) {
			    if (ch == '$') {
			    	inside = true;
			    } else if (ch == '#') {
			    	inside = false;
			    	String message = builder.toString();
			    	
			    	int check1 = breader.read();
			    	int check2 = breader.read();
			    	
			    	// Verify checksum. This should never fail since the transport layer assures
			    	// correct transmission.			    	
			    	boolean match = (calcChecksum(check1, check2) == calcChecksum(message));
			    	
			    	String resp = match ? "+" : "-"; 
			    	socket.getOutputStream().write(resp.getBytes());
			    	
			    	if (!match) 
			    		throw new SessionException("Checksum mismatch");
			    	return message;
			    } else if (inside) {
			    	if (ch == '\\')
			    		ch = breader.read();
			    	builder.append((char)ch);
			    } else {
			    	// just drop characters until we read '$' again
			    }
			}
		} catch (IOException e) {
			throw new SessionException("Failed to contact session", e);
		}
		
		// Unexpected end of file
		throw new SessionException("Disconnected");
	}
	
	public Response command(String... args) throws SessionException {
		String command = "";
		for (String arg : args)
			command += arg + ",";

		send(command);
		return new Response(recv());
	}
	
	public void close() throws SessionException {
		try {
			socket.close();
		} catch (IOException e) {
			throw new SessionException("Failed to contact session", e);
		}
	}

}

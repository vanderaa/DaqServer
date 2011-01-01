package be.xe.DaqServer;
import com.dalsemi.system.*;
import com.dalsemi.system.I2CPort;


import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.*;

import java.io.*;
import java.net.*;
import java.util.*;

import be.xe.DaqServer.*;

//import java.text.*;

public class ADCServer extends Thread {
	I2CPort i2c=null;
	DSPortAdapter adapter=null;
	MaxADC[] adcs=null;
	Hashtable onewiredevices=new Hashtable();
	Hashtable onewiredevicestype=new Hashtable();
	ServerSocket ssocket=null;
	final static int SERVER_PORT=3000;
	ADCServer(byte adr1,byte adr2) {
		i2c=new I2CPort();
		i2c.setClockDelay((byte)2);
		adcs=new MaxADC[2];
		adcs[0]=new MaxADC(adr1,i2c);
		adcs[1]=new MaxADC(adr2,i2c);
		// specific for the 1wire bus
		try {
			adapter = OneWireAccessProvider.getDefaultAdapter();
			System.out.println();
			System.out.println("Adapter: "+adapter.getAdapterName()
					  +" Port: "  +adapter.getPortName());
		} catch (Exception e) { System.out.println(e); }
	}
	// Main Loop of the DAQ system waiting for a connection to come
	public void run() {
		try { ssocket=new ServerSocket(SERVER_PORT); }
		catch (Exception socketexc) { socketexc.printStackTrace(); return; }
		// now wait for a connection to occur
		while(true) {
			Socket thesocket=null;
			try { thesocket=ssocket.accept(); }
			catch (Exception socketexc) { System.out.println("Fatal Error With Server"); }
			//System.out.println("Got a connection from");
			HandleConnection(thesocket);
		}
	}
	// Handle the Connection by reading on it
	private void HandleConnection(Socket s) {
		InputStreamReader input=null;
		BufferedReader   binput=null;
		PrintWriter     poutput=null;
		try {
			  input=new InputStreamReader(s.getInputStream());
			 binput=new BufferedReader(input);
			poutput=new PrintWriter(s.getOutputStream(),true);
			String command;
			//System.out.println("Handling connection");
			while((command=binput.readLine()) != null) {
				command=command.toUpperCase();
				//System.out.println(command);
				if(command.startsWith("RADC")) poutput.println(readADC(command));
				else if(command.startsWith("SADC")) poutput.println(doRanges(command));
				else if(command.startsWith("A1WD")) poutput.println(doAdd1WireDevice(command));
				else if(command.startsWith("R1WD")) poutput.println(doRead1WireDevice(command));
				else if(command.startsWith("S1WD")) poutput.println(doSet1WireDevice(command));
				else poutput.println("Invalid Command");
			}
		
		} catch (Exception e) { e.printStackTrace(); }
		finally {
		 //System.out.println("Finally");
		 try { binput.close(); poutput.close(); input.close(); s.close(); }
		 catch (IOException e) { System.out.println(e); }
		}
	}

	private String doSet1WireDevice(String command) {
		return  "This command is not yet implemented";
	}

	private String doRead1WireDevice(String command) {
		StringTokenizer st=new StringTokenizer(command);
		if(st.countTokens()!=3) return "ERROR this is not a valid command, it should be R1WD [id] [channel]"; 
		st.nextToken();
		String id=st.nextToken();
		int channel;
		try { channel=Integer.parseInt(st.nextToken()); } 
		catch (Exception e) { return "ERROR: channel identification is not valid"; }
		String currenttype=(String)onewiredevicestype.get(id);
		if(currenttype.equals("DS1920")) {
			OneWireContainer10 tempsensor=(OneWireContainer10)onewiredevices.get(id);
			if (tempsensor != null) {
				double temperature;
				try {
					byte[] state;
					state=tempsensor.readDevice();
					tempsensor.doTemperatureConvert(state);
					state=tempsensor.readDevice();
					temperature=tempsensor.getTemperature(state);
				} catch (OneWireException e) { 
					System.out.println("One Wire Exception Occured"); 
					return "ERROR: Exception with one wire bus or device";
				}			
				return "R1WD "+id+" "+channel+" "+temperature;
			}
			return "ERROR: No such device present";
		}
		if(currenttype.equals("DS1921")) {
			OneWireContainer21 thermochron=(OneWireContainer21)onewiredevices.get(id);
			if(thermochron != null) {
				double temperature;
				try {
					byte[] state;
					state=thermochron.readDevice();
					thermochron.doTemperatureConvert(state);
					state=thermochron.readDevice();
					temperature=thermochron.getTemperature(state);
				} catch (OneWireException e) {
			               System.out.println("One Wire Exception Occured");
                                       return "ERROR: Exception with one wire bus or device";
				}
                 		return "R1WD "+id+" "+channel+" "+temperature;
			}
			return "ERROR: No such device present";
		}
		if(currenttype.equals("DS2423")) {
			OneWireContainer1D counter=(OneWireContainer1D)onewiredevices.get(id);
			if(counter != null) {
				long cnt;
				try {	
					cnt=counter.readCounter(channel);	
				} catch (Exception e) {
					return "ERROR: This is not a valid command";
				}
				return "R1WD "+id+" "+channel+" "+cnt;
			}
			return "ERROR: No such device present";
		}
		if (currenttype.equals("DS2405")) {
			OneWireContainer05 swit=(OneWireContainer05)onewiredevices.get(id);
			if(swit != null) {
				boolean status;
				try {
					byte[] state;
					state=swit.readDevice();
					status=swit.getLatchState(channel,state);
				} catch (Exception e) {
					return "ERROR: No such channel";
				}
				return "R1WD "+id+" "+channel+" "+status;
			}
			return "ERROR: No such device present";	
		}
	return "ERROR: No device found to correspond to the description given";
	}


	private String doAdd1WireDevice(String command) {
		// first get the address and the name to identify the device and the device type
		StringTokenizer st=new StringTokenizer(command);
		if(st.countTokens()!=4) return "ERROR this is not a valid command, it should be A1WD [address] [type] [id]";
		st.nextToken();
		String contaddress=st.nextToken();
		String conttype=st.nextToken();
		String number=st.nextToken();
		try {
			adapter.beginExclusive(true);
			OneWireContainer thecontainer=adapter.getDeviceContainer(contaddress);	
			adapter.endExclusive();
			if (thecontainer == null) { return "ERROR: No device found with this address !"; }
			if (conttype.equals("DS1921")) { 
				OneWireContainer21 tcont=(OneWireContainer21)thecontainer;
				try { tcont.disableMission(); } 
				catch(Exception e) { return "ERROR: Sorry could not add DS1921 since i cannot disable mission"; }
			}
			onewiredevices.put(number,thecontainer);	
			onewiredevicestype.put(number,conttype);
		}
		catch  (Exception e) { System.out.println("Problem occured"); return "ERROR: Error getting device"; }
		return "Added "+conttype+" with id "+number+" At address: ["+contaddress+"]";
	}


	private String doRanges(String command) {
		String errorinfo=new String("Command Invalid should be SADC [adc] [channel] [+5,+-5,+10,+-10]");
		StringTokenizer st=new StringTokenizer(command);	
		//System.out.println("Number of tokens"+st.countTokens());
		if(st.countTokens()==4) {
			int adcn;
			byte channel;
			String mode;	
			st.nextToken();
			try { adcn=Integer.parseInt(st.nextToken()); } catch(Exception e) { return errorinfo; }	
			try { channel=(byte)Integer.parseInt(st.nextToken()); } catch(Exception e) { return errorinfo; }
			mode=st.nextToken();
			//System.out.println("adc "+adcn+" channel "+channel+" mode "+mode);
			if(!(mode.equals("+5") || mode.equals("+-5") || mode.equals("+10") || mode.equals("+-10"))) return errorinfo;
			if(adcn >= 2) return errorinfo;
			if(channel >= 8) return errorinfo;
			// now do the real setting since the syntax seems ok
			adcs[adcn].setRange(mode,channel);
			return "ADC number ["+adcn+"] channel  ["+channel+"] Set in "+mode;
		}
		else { return errorinfo; }
	}
	private String readADC(String command) {
		String errorinfo=new String("Command Invalid, should be RADC [adc] or RADC to read all ADC");
		StringTokenizer st=new StringTokenizer(command);
		// test the syntax
		int ntokens=st.countTokens();
		int adcn=0;
		int chan=0;
		if(ntokens > 3) return errorinfo;
		else if(!st.nextToken().equals("RADC")) return errorinfo;
		switch (ntokens) {
			case 1: {
				String retst=new String("RADC");
				try {
				 for (byte i=0;i<8;i++) { retst=retst+" "+adcs[0].getValue(i); }
				 for (byte i=0;i<8;i++) { retst=retst+" "+adcs[1].getValue(i); }
				} catch (Throwable t) { return "ADC read ERROR"; }
				return retst;	
				}
			case 2:
				{
				String retst=new String("RADC");
				try { adcn=Integer.parseInt(st.nextToken()); } catch(Exception e) { return errorinfo; }	
				try {
				  for(byte i=0;i<8;i++) { retst=retst+" "+adcs[adcn].getValue(i); }
				} catch (Throwable t) { return "ADC read ERROR"; }
				return retst;
				}
			case 3: {
				String retst=new String("RADC");
				try { 
				  adcn=Integer.parseInt(st.nextToken()); 
				  chan=Integer.parseInt(st.nextToken());
				  if(chan > 7) return "Not a valid channel";
				} catch(Exception e) { return errorinfo; }
				try { 
				  retst=retst+" "+adcs[adcn].getValue((byte)chan);
				  return retst;
				} catch (Throwable t) { return "ADC read ERROR"; }
				}
			default: return errorinfo;
		}	
	}
}

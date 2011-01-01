package be.xe.DaqServer;

import be.xe.DaqServer.*;

public class daq { 
	daq() {  }
	public static void main(String[] args) {
		 System.out.println("Starting DaqServer v0.1");
		 try    { (new ADCServer((byte)0x28,(byte)0x2F)).start(); }
		 catch (Exception e) { System.out.println(e); }
	}
}

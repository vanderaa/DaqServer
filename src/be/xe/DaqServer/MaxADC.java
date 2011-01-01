package be.xe.DaqServer;

import com.dalsemi.system.*;
import com.dalsemi.system.I2CPort;
import be.xe.DaqServer.*;

public class MaxADC {
	public MaxADC(byte addr,I2CPort thei2c) {
		i2c=thei2c;
		address=addr;
		for (byte i=0; i<8; i++) { 
			cbyte[i]=(byte)((i*(byte)16)+(byte)(138)); // set in 10 V mode unipolar for each channel
			sign[i]=false;
		}
	}
	public void setRange(String setting,byte channel) {
		byte valuetoadd;
		if(setting.equals("+5"))  { valuetoadd=0; sign[channel]=false; }
		else if(setting.equals("+10")) { valuetoadd=8; sign[channel]=false; } 
		else if(setting.equals("+-5"))  { valuetoadd=4;  sign[channel]=true; }  
	        else if(setting.equals("+-10")) { valuetoadd=12; sign[channel]=true; }
		else { valuetoadd=8; sign[channel]=false; }
//		cbyte[channel]=(byte)((cbyte[channel] & 0xF3)+valuetoadd);	
	 	cbyte[channel]=(byte)((byte)(((byte)((channel*(byte)16)+(byte)(138))) & 0xF3)+valuetoadd);	
	}
	public short getValue(byte channel) {
		byte[] b=new byte[5];
		b[0]=cbyte[channel];
		try {
			i2c.slaveAddress=address;
			short value=0;
			if (i2c.write(b,0,1)<0) { System.out.println("Fail write"); return 0; }	
			if (i2c.read(b,0,2)<0)  { System.out.println("Fail read"); return 0; }
			value=(short)((b[0] & 0xFF)*16+((b[1] & 0xFF) >> 4));
			if (((value & (short)(2048))==2048) && sign[channel]) value=(short)(value+(short)(61440));
//			System.out.println((byte)(b[0] & 0xFF));
//			System.out.println((byte)(b[1] & 0xFF));
			return value;
		} catch (IllegalAddressException e) {
			System.out.println("Illegal Address on Memory mapped I2C");
			return 0;
		}
	}
	byte address;
	I2CPort i2c;
	byte[] cbyte=new byte[8];
	boolean[] sign=new boolean[8];
}

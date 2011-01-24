package org.exobel.routerkeygen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ThomsonKeygen extends KeygenThread {

	/*
	 * Thomson Keygen Working variables. Here to save speed as this
	 *  thread is created much sooner than needed.
	 */
	byte[] cp;
	byte[] hash;
	byte[] entry;
	byte[] table;
	int a, b, c;
	int year;
	int week;
	int sequenceNumber;
	byte[] routerESSID;

	public ThomsonKeygen(RouterKeygen par) {
		super(par);
		this.cp = new byte[12];
		this.hash = new byte[19];
		this.table= new byte[1282];
		this.entry = new byte[3000];
		this.routerESSID = new byte[3];
	}

	public void  run()
	{
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
		}
		
		if ( router == null)
			return;
		
		if ( router.getEssid().length() != 6 ) 
		{
			pwList.add(parent.getResources().getString(R.string.msg_shortessid6));
			parent.list_key =  pwList;
			parent.handler.sendEmptyMessage(1);
			return;
		}
		
		for (int i = 0; i < 6; i += 2)
			routerESSID[i / 2] = (byte) ((Character.digit(router.getEssid().charAt(i), 16) << 4)
					+ Character.digit(router.getEssid().charAt(i + 1), 16));

		
		RandomAccessFile fis;
		try {
			fis = new RandomAccessFile(parent.folderSelect + File.separator + "RouterKeygen.dic", "r");
		} catch (FileNotFoundException e2) {
			pwList.add(new String("Dictionary not found on SDCard!" ));
			parent.list_key =  pwList;
			parent.handler.sendEmptyMessage(1);
			return;
		}
		cp[0] = (byte) (char) 'C';
		cp[1] = (byte) (char) 'P';
		
		try {
			if ( fis.read(table) == -1 )
			{
				pwList.add(parent.getResources().getString(R.string.msg_errordict));
				parent.list_key =  pwList;
				parent.handler.sendEmptyMessage(1);
				return;
			}	
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		int totalOffset = 0;
		int offset = 0;
		if ( table[( 0xFF &routerESSID[0] )*5 + 2 ] == routerESSID[0] )
		{
			int i = ( 0xFF &routerESSID[0] )*5 + 2;
			offset =( (0xFF & table[i + 1]) << 24 ) | ( (0xFF & table[i + 2])  << 16 ) |
					( (0xFF & table[i + 3])  << 8 ) | (0xFF & table[i + 4]);
		}
		totalOffset += offset;
		try {
			fis.seek(totalOffset);
			if ( fis.read(table,0,1024) == -1 )
			{
				pwList.add(parent.getResources().getString(R.string.msg_errordict));
				parent.list_key =  pwList;
				parent.handler.sendEmptyMessage(1);
				return;
			}	
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		int lenght = 0;
		if ( table[( 0xFF &routerESSID[1] )*4] == routerESSID[1] )
		{
			int i = ( 0xFF &routerESSID[1] )*4;
			offset =( (0xFF & table[i + 1])  << 16 ) |
					( (0xFF & table[i + 2])  << 8 ) | (0xFF & table[i + 3]);
			lenght =  ( (0xFF & table[i + 5])  << 16 ) |
					( (0xFF & table[i + 6])  << 8 ) | (0xFF & table[i + 7]);
			
		}
		totalOffset += offset;
		try {
			fis.seek(totalOffset );
			if ( fis.read(entry,0, lenght - offset) == -1 )
			{
				pwList.add(parent.getResources().getString(R.string.msg_errordict));
				parent.list_key =  pwList;
				parent.handler.sendEmptyMessage(1);
				return;
			}	
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		lenght -= offset;
		for( offset = 0 ; offset < lenght ; offset += 4 )
		{
			if ( stopRequested )
				return;

			if ( entry[offset] != routerESSID[2])
				continue;
			
			sequenceNumber = ( (0xFF & entry[offset + 1]) << 16 ) | 
							( (0xFF & entry[offset + 2])  << 8 ) | (0xFF & entry[offset + 3]) ;
			c = sequenceNumber % 36;
			b = sequenceNumber/36 % 36;
			a = sequenceNumber/(36*36) % 36;
			year = sequenceNumber / ( 36*36*36*52 ) + 4 ;
			week = ( sequenceNumber / ( 36*36*36 ) ) % 52 + 1 ;				
			cp[2] = (byte) Character.forDigit((year / 10), 10);
			cp[3] = (byte) Character.forDigit((year % 10), 10);
			cp[4] = (byte) Character.forDigit((week / 10), 10);
			cp[5] = (byte) Character.forDigit((week % 10), 10);
			cp[6] = charectbytes0[a];
			cp[7] = charectbytes1[a];
			cp[8] = charectbytes0[b];
			cp[9] = charectbytes1[b];
			cp[10] = charectbytes0[c];
			cp[11] = charectbytes1[c];
			md.reset();
			md.update(cp);
			hash = md.digest();

			try {
				pwList.add(StringUtils.getHexString(hash).substring(0, 10).toUpperCase());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		if(pwList.toArray().length == 0)
		{
			pwList.add(parent.getResources().getString(R.string.msg_errnomatches));
			parent.list_key = pwList;
			parent.handler.sendEmptyMessage(1);
			return;
		}

		parent.list_key = pwList;
		parent.handler.sendEmptyMessage(0);
		return;
		
	}
    static byte[] charectbytes0 = {
        '3',
        '3',
        '3',
        '3',
        '3',
        '3',
        '3',
        '3',
        '3',
        '3',
    	'4',
        '4',
        '4',
        '4',
        '4',
        '4',
        '4',
        '4',
        '4',
        '4',
        '4',
        '4',
        '4',
        '4',
        '4',
        '5',
        '5',
        '5',
        '5',
        '5',
        '5',
        '5',
        '5',
        '5',
        '5',
        '5',
        };
    
    static byte[] charectbytes1 = {
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        'A',
        'B',
        'C',
        'D',
        'E',
        'F',
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        'A',
        };

}
package me.uyuyuy99.ffa;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PrintStreamDateTime extends PrintStream {
	
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("[MM-dd-yyyy hh:mm:ss a] ");
	
	public PrintStreamDateTime(OutputStream out) {
		super(out);
	}
	
	@Override
	public void println(Object x) {
		super.println(dateFormat.format(new Date()) + x);
	}
	
	@Override
	public void println(String x) {
		super.println(dateFormat.format(new Date()) + x);
	}
	
	@Override
	public void println(char[] x) {
		// TODO Auto-generated method stub
		super.println(dateFormat.format(new Date()) + String.valueOf(x));
	}
	
}

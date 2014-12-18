package denaro.nick.httpserver;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

public class HTTPClient extends Thread
{
	public HTTPClient(Socket socket) throws IOException, NoSuchAlgorithmException
	{
		System.out.println("HTTPClient created: "+socket);
		this.socket=socket;
		in=socket.getInputStream();
		//out=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		out=socket.getOutputStream();
		
		connect=true;
		
		if(!handShake())
		{
			connect=false;
		}
	}
	
	private boolean connect;
	
	public void run()
	{
		if(!connect)
			return;
		running=true;
		
		while(running)
		{
			String line;
			try
			{
				byte b;
				while((b=(byte)in.read())!=-1)
				{
					byte opcode=b;
					int len;
					byte[] lenbytes=new byte[9];
					in.read(lenbytes,0,1);
					len=lenbytes[0]&0b01111111;
					if(lenbytes[0]==126)
					{
						in.read(lenbytes,1,2);
						len=lenbytes[2]<<8+lenbytes[3];
						
					}
					else if(lenbytes[0]==127)
					{
						in.read(lenbytes,1,8);
						len=0;
						for(int i=0;i<8;i++)
						{
							len+=lenbytes[2+i]<<((7-i)*8);
						}
						//not sure what to do, if it is this, then throw shit
						throw new UnsupportedOperationException("Don't know how to handle...");
					}
					byte[] mask=new byte[4];
					in.read(mask,0,4);
					byte[] data=new byte[len];
					in.read(data,0,len);
					
					String mes=decode64(data,mask);
					
					/*if(mes.equals("Ping"))
					{
						sendMessage("Pong");
					}*/
					handleMessage(mes);
				}
			}
			catch(IOException | UnsupportedOperationException e)
			{
				e.printStackTrace();
				running=false;
			}
		}
		System.out.println("---------------\nClient stopped!\n---------------");
	}
	
	public void handleMessage(String message) throws IOException
	{
		
	}
	
	public boolean handShake() throws IOException, NoSuchAlgorithmException
	{
		System.out.println("handshake");
		BufferedReader bufin=new BufferedReader(new InputStreamReader(socket.getInputStream()));
		String line;
		String get="";
		while(!(line=bufin.readLine()).isEmpty())
		{
			System.out.println(line);
			if(line.contains("GET"))
			{
				get=line.substring(line.indexOf('/'),line.indexOf(" HT"));
				if(get.equals("/index.html"))
				{
					servePage();
					return false;
				}
			}
			if(line.contains("Sec-WebSocket-Key:"))
			{
				key=createKey(line.substring(line.indexOf(":")+2));
				System.out.println("key: "+key);
				//sendHeaders();
				//sendMessage("Pong. This is a message...");
			}
		}
		
		sendHeaders();
		
		return(true);
	}
	
	public static String createKey(String k) throws NoSuchAlgorithmException, UnsupportedEncodingException
	{
		MessageDigest md=MessageDigest.getInstance("SHA-1");
		String temp=k+"258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
		byte[] buf=md.digest(temp.getBytes("UTF-8"));
		
		String result=byteArrayTo64String(buf);
		return(result);
	}
	
	public static String byteArrayTo64String(byte[] buf)
	{
		byte[] buffer=Base64.getEncoder().encode(buf);
		return(new String(buffer));
	}
	
	public static String decode64(byte[] buf, byte[] mask)
	{
		try
		{
			byte[] decode=new byte[buf.length];
			for(int i=0;i<buf.length;i++)
			{
				decode[i]=(byte)(buf[i]^mask[i%4]);
			}
			String temp=new String(decode,"UTF-8");
			return(temp);
		}
		catch(Exception ex)
		{
			return("exception... =(");
		}
	}
	
	public static String byteArrayToString(byte[] array)
	{
		String result="";
		for(int i=0;i<array.length-1;i++)
		{
			result+=(int)array[i]+", ";
		}
		result+=array[array.length-1];
		
		return(result);
	}
	
	public static String byteArrayToChars(byte[] array)
	{
		String result="";
		for(int i=0;i<array.length;i++)
		{
			result+=(char)array[i];
		}
		return(result);
	}
	
	private void sendHeaders() throws IOException
	{
		String header="";
		header+="HTTP/1.1 101 Switching Protocols\r\n";
		header+="Upgrade: websocket\r\n";
		header+="Connection: upgrade\r\n";
		header+="Sec-WebSocket-Accept: "+key+"\r\n";
		header+="\r\n";
		//header+="\r\n";
		out.write(header.getBytes());
		out.flush();
	}
	
	private void servePage() throws IOException
	{
		String header="";
		header+="HTTP/1.1 200 OK\r\n";
		//header+="Connection: close\r\n";
		header+="Content-Type: text/html\r\n";
		header+="Date: "+new Date()+"\r\n";
		header+="Server: The Fileserver \r\n";
		
		
		File f=new File("index.html");
		ByteArrayOutputStream buf=new ByteArrayOutputStream();
		FileInputStream fin=new FileInputStream(f);
		int data;
		while((data=fin.read())!=-1)
		{
			buf.write(data);
		}
		fin.close();
		
		System.out.println("buf.size(): "+buf.size());
		
		header+="Accept-Ranges: bytes\r\n";
		header+="Content-Length: "+buf.size()+"\r\n";
		//header+="Last-Modified: \r\n";
		header+="\r\n";
		
		header+=new String(buf.toByteArray()/*,"UTF-8"*/);
		
		//header+="\r\n";
		//header+="\r\n";
		
		buf.close();
		//out.write(buf.toByteArray());
		out.write(header.getBytes());
		out.flush();
		out.close();
	}
	
	public void sendMessage(String message) throws IOException
	{
		ByteBuffer buf=ByteBuffer.allocate(1+1+message.length());
		buf.put((byte)129);
		buf.put((byte)message.length());
		buf.put(message.getBytes("UTF-8"));
		//buf.put("\r\n\r\n".getBytes());//this kills the socket
		out.write(buf.array());
		out.flush();
	}
	
	private boolean running;
	private Socket socket;
	private InputStream in;
	private OutputStream out;
	private String key;
}

package denaro.nick.httpserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class HTTPServer extends Thread
{
	public HTTPServer()
	{
		try
		{
			server=new ServerSocket();
			server.setReuseAddress(true);
			server.bind(new InetSocketAddress("localhost",9400));
		}
		catch(IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Server bound to "+server.getLocalSocketAddress());
	}
	
	public void add(Socket socket) throws NoSuchAlgorithmException, IOException
	{
		HTTPClient client=new HTTPClient(socket);
		if(!socket.isClosed())
			client.start();
	}
	
	public void run()
	{
		System.out.println("Server started");
		running=true;
		while(running)
		{
			try
			{
				Socket socket=server.accept();
				add(socket);
			}
			catch(IOException | NoSuchAlgorithmException e)
			{
				e.printStackTrace();
				running=false;
			}
		}
	}
	
	private ServerSocket server;
	private boolean running;
}

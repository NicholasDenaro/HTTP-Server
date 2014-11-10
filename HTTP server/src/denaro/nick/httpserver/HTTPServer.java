package denaro.nick.httpserver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

public class HTTPServer extends Thread
{
	public HTTPServer() throws IOException
	{
		server=new ServerSocket();
		server.setReuseAddress(true);
		server.bind(new InetSocketAddress("66.71.87.171",9400));
		//server.bind(new InetSocketAddress("localhost",9400));
		System.out.println("Server bound to "+server.getLocalSocketAddress());
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
				HTTPClient client=new HTTPClient(socket);
				if(!socket.isClosed())
					client.start();
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

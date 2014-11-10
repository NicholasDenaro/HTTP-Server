package denaro.nick.httpserver;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class Main
{
	public static void main(String[] args) throws IOException, NoSuchAlgorithmException
	{
		HTTPServer server=new HTTPServer();
		server.start();
	}
}

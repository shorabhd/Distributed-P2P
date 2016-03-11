import java.net.Socket;
import java.util.HashMap;

public class Connect extends Thread
{
	String str[] = null;
	
	//Initialize HashMap to NULLL
	HashMap<String,Socket> map = null;
	public Connect(String str[], HashMap<String, Socket> map)
	{
		this.map = map;
		this.str = str;
	}
	public void run()
	{
		Socket client = null;
		try 
		{
			Thread.sleep(60000); //To wait until all the Servers are UP!
			
			//Create Connection with the Server
			client = new Socket(str[1],Integer.parseInt(str[2]));
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		//Put Server Name,Socket in HashMap
		map.put(str[0], client);
		System.out.println(str[0]+" Connected");
	}
}

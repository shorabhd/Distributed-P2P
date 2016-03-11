import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public class DHT
{
	public final static String FILE_PATH = "E:/eclipse/workspace/DistributedP2P/files/"; // You Need to Change This
	public static ServerSocket peerServer;
	public static Socket receiver;
	public static HashMap<String,Socket> map = new HashMap<String,Socket>();
	public static String name;

	public static void main(String args[]) throws IOException
	{
		name = args[0];
		//Read Config File
		BufferedReader fr = new BufferedReader(new FileReader(FILE_PATH + "config.txt"));
		String s = null;

		while((s = fr.readLine())!= null)
		{
			String []str = s.split(";");
			if(str[0].equals(name))			//True For Self Entry in Config File
			{
				try 
				{
					peerServer = new ServerSocket(Integer.parseInt(str[2]));
					System.out.println(str[0]+" is UP!");
					Thread t1 = new Thread(new Client(map));
					t1.start();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			else
			{
				Thread t1 = new Thread(new Connect(str,map));
				t1.start();
			}
		}
		while(!peerServer.isClosed())
		{
			receiver = peerServer.accept();			//Server is ready to Accept
			Type2 t2 = new Type2(receiver);			//To Handle Multiple Client Requests
			t2.start();
		}
	}
}

//Handles User Queries from Client Requests
class Type2 extends Thread
{
	Socket peer;
	public static Multimap<String, String> myMultimap = ArrayListMultimap.create();
	public Type2(Socket peer)
	{
		this.peer = peer;
	}

	public void run()
	{
		try 
		{
			DataInputStream dis = new DataInputStream(peer.getInputStream());
			DataOutputStream ps = new DataOutputStream(peer.getOutputStream());
			String choice=null;
			boolean flag = true;
			while(flag)
			{
				//To read the choice of operation user wants to perform
				choice = dis.readUTF();
				switch(choice)
				{
				case "1":
					String keyValue = dis.readUTF();
					String keyVal[] = keyValue.split(",");
					//unPAD and Call to GET Function
					if(registry(keyVal[0].substring(0,23).replace("!", ""), keyVal[1].substring(0,1023).replace("!", "")))
						ps.writeUTF("Success");
					else
						ps.writeUTF("Failure");
					break;
				case "2":
					String key = dis.readUTF();
					//unPAD and Call to GET Function
					Collection<String> values = search(key.substring(0,23).replace("!", ""));

					//To Inform Peer about size of the List coming up.
					ps.write(values.size());

					//Sending PeerID List
					Iterator i = values.iterator();
					while (i.hasNext())
					{
						String name = (String) i.next();
						ps.writeUTF(name);
					}

					break;
				case "3":
					String file = dis.readUTF();
					obtain(file);
					break;
				case "0":
					flag=false;
					break;
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Server " + peer + " Disconnected");
		}	
	}
	
	//To Send File to the Client
	public void obtain(String fileName)
	{
		BufferedInputStream bis = null;
		DataOutputStream os = null;
		try 
		{			
			File file = new File (DHT.FILE_PATH + "Peer" +(DHT.name).substring(DHT.name.length()-1) + "/" + fileName);   //Example:"/home/shorabh/Assignment 1/22000/Send/filename.txt"
			FileInputStream fis = new FileInputStream(file);
			bis = new BufferedInputStream(new FileInputStream(file));
			
			//Sending Data in Chunks of 4096 Bytes 
			byte [] buffer  = new byte [4096];
			int count;
			int total=0;
			os = new DataOutputStream(peer.getOutputStream());

			//To Inform Peer Client about the file size for buffer allocation
			int size = (int)file.length();
			os.writeLong(size);

			//No. of Bytes read from the buffer
			while ((count = fis.read(buffer)) != -1) 
			{
				os.write(buffer,0,count);
				total += count;
			}
			System.out.println("File " + file + " of " + total + " bytes Sent.");

		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}


	//To Register a File
	public static boolean registry(String fileName, String peerId)
	{
		if(!myMultimap.containsEntry(fileName, peerId))
		{
			if(myMultimap.put(fileName, peerId))
			{
				System.out.println("Distributed Hash Table:");
				System.out.println(myMultimap);	
				return true;
			}
			else
				return false;
		}
		else
			return true;
	}

	//To Search a File
	public static Collection<String> search(String file)
	{
		Collection<String> values = myMultimap.get(file);
		System.out.println(values);
		return values;
	}
}
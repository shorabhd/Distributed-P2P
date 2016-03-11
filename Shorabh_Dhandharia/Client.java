import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;

public class Client implements Runnable 
{
	//Creating HashMap to store Name/Socket Pairs
	HashMap<String, Socket> map = new HashMap<String,Socket>();

	public Client(HashMap<String,Socket> map)
	{
		this.map = map;
	}

	public static DataInputStream dis = null;

	public void run()
	{
		try
		{
			dis = new DataInputStream(System.in);
			String choice;
			do
			{
				System.out.println("Menu: \n1.Register File(s) \n2.Search File \n3.Obtain File \n0.Exit\n");
				choice = dis.readLine();
				String val = null;
				String peer = null;
				String serverName[];
				switch(choice)
				{
				case "1": 
					System.out.println("Enter File(s) with Extension:");
					val = dis.readLine();
					String str[] = val.split(",");
					for(int i=0;i<str.length;i++)
					{
						serverName = hashFunction(keyPad(str[i]));
						//Key/Value Register & Replication
						for(int j=0; j<serverName.length;j++)
						{
							if(serverName[j].equals(DHT.name))										//Perform PUT on Self
								Type2.registry(str[i],DHT.name);
							else
							{
								msgPass(serverName[j],choice,keyPad(str[i]) + "," + valPad(DHT.name));
							}
						}
						//File Replication
						copyFiles(str[i], serverName[1]);
					}
					break;
				case "2":
					System.out.println("Enter File:");
					val = dis.readLine();
					serverName = hashFunction(keyPad(val));
					if(serverName[0].equals(DHT.name))										//Perform GET on Self
					{
						System.out.println("File is present at below Locations:");
						Iterator i = Type2.search(val).iterator();
						while (i.hasNext())
						{
							String name = (String) i.next();
							System.out.println("Peer ID: " +name);
						}		
					}
					else
						msgPass(serverName[0],choice,keyPad(val));		
					break;
				case "3":
				{
					System.out.println("Enter File Name:");
					val = dis.readLine();
					System.out.println("Enter Peer ID:");
					peer = dis.readLine();
					fileReceive(choice,val,peer);
					break;
				}
				case "0":
					break;
				}		
			}while(!choice.equals("0"));
		}
		catch(Exception e)
		{
		}
	} 
	
	//Replication Logic
	public void copyFiles(String file, String dest)
	{
		InputStream input = null;
		OutputStream output = null;
		try
		{
			File source = new File (DHT.FILE_PATH + "Peer" +(DHT.name).substring(DHT.name.length()-1)+ "/" + file);  
			File destn = new File (DHT.FILE_PATH + "Peer" +(dest).substring(DHT.name.length()-1)+ "/");   
			
			if(!destn.exists())
			{
				System.out.println("Creating New Directory");
				destn.mkdirs();
			}
			
			input = new FileInputStream(source);
			output = new FileOutputStream(destn + "/" +file);
			//Copying Files using Chunks of 4096
			byte [] buf  = new byte [4096];
			int bytesRead;
			while ((bytesRead = input.read(buf)) != -1) 
			{
				output.write(buf, 0, bytesRead);
			}
			System.out.println("File Replicated!");
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally 
		{
			try
			{
				input.close();
				output.close();
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	//File Downloading Logic
	public void fileReceive(String choice, String file, String peer)
	{
		String serverName[] = null;
		DataOutputStream os = null;
		FileOutputStream fos = null;
		DataInputStream is = null;
		Socket sendServer = null;
		try
		{
			System.out.println("Connecting to Server");
			sendServer = map.get(peer);
			System.out.println("Connected");

			//To Send Choice & File Name
			os = new DataOutputStream(sendServer.getOutputStream());
			os.writeUTF(choice);
			os.writeUTF(file);
			
			File path = new File(DHT.FILE_PATH + "Peer" +(DHT.name).substring(DHT.name.length()-1)+"/");
			
			if(!path.exists())
			{
				System.out.println("Creating New Directory");
				path.mkdirs();
			}
			
			fos = new FileOutputStream(path+"/"+file);	
			is = new DataInputStream(sendServer.getInputStream());
			
			int size = (int)is.readLong();
			
			//Receiving Files using Chunks of 4096
			byte [] buffer  = new byte[4096];
			
			int bytesRead = 0;
			int totalBytes = 0;
			
			//To Prevent Concurrent Thread Access
			synchronized (this) 
			{
				while(totalBytes < size)
				{
					bytesRead = is.read(buffer);
					fos.write(buffer, 0, bytesRead);
					totalBytes += bytesRead;
				}
			}
			System.out.println("File " + file + " Downloaded from " + peer + "(" + totalBytes + " bytes read)");
		} 
		catch(FileNotFoundException e)
		{
			System.out.println("File Not Found!");
		}
		catch (IOException e) 
		{
			//In case of failure, will go to Replicated Node
			serverName = hashFunction(keyPad(file));
			fileReceive(choice,file,serverName[1]);
		}
	}


	//To PAD Key to 24 Bytes
	public static String keyPad(String s)
	{
		for(int i=s.length();i<24;i++)
			s = s+"!";
		return s;
	}

	//To PAD Value to 1000 Bytes
	public static String valPad(String s)
	{
		for(int i=s.length();i<1024;i++)
			s = s+"!";
		return s;
	}

	//To perform I/O operations with Other Servers
	public void msgPass(String s,String choice,String val)
	{
		try
		{
			//Get the Socket from the HashMap 
			Socket socket = map.get(s);
			DataInputStream in = new DataInputStream(socket.getInputStream());
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			out.writeUTF(choice);
			out.writeUTF(val);
			if(choice.equals("1"))		//For Register, it will Print Success/Failure
				System.out.println(in.readUTF());
			if(choice.equals("2"))		//For Search, to Print result
			{
				int size = in.read();
				if(size==0)
				{
					System.out.println("File not Registered");
				}
				else
				{
					System.out.println("File is present at below Locations:");
					do
					{
						System.out.println("Peer ID: "+in.readUTF());
						--size;
					}while(size != 0);
				}
			}
		}
		catch(IOException e)
		{
			//In case of Failure, it will go to Replicated Nodes
			if(Integer.parseInt(s.substring(s.length()-1)) < 3 || Integer.parseInt(s.substring(s.length()-1)) == 7)
			{
				msgPass("Server3",choice,keyPad(val));
			}
			else 
			{
				msgPass("Server7",choice,keyPad(val));
			}
		}
	}

	//Perform Hashing and Sending the Indexing Server's & Replicating Server's name
	public String[] hashFunction(String key)
	{
		String name[] = new String[2] ;
		int serverNo = Math.abs(key.hashCode()%8);
		name[0] = "Server"+ serverNo;
		if(serverNo < 3 || serverNo == 7)
		{
			name[1] = "Server3";
		}
		else
		{
			name[1] = "Server7";
		}
		return name;
	}
}

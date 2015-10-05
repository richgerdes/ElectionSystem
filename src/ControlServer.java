import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;


public class ControlServer implements Runnable {
	
	private static int DEFAULT_PORT = 19999;
	
	private ServerSocket server;
	private Thread thread;
	private Vector<Client> clients;

	@SuppressWarnings("unused")
	private ConsoleListener console;
	
	public ControlServer() throws IOException{
		this(DEFAULT_PORT);
	}
	
	public ControlServer(int port) throws IOException{
		this.server = new ServerSocket(port);
		this.console = new ConsoleListener();
		
		this.clients = new Vector<Client>();
		
		this.thread = new Thread(this);
		this.thread.start();
	}

	public void annouceNode(Client client) {
		System.out.println("New client " + client.toString());
		this.clients.add(client);
		for(Client c : this.clients){
			if(c != client){
				c.announce(client);
				client.announce(c);
			}
		}
	}
	
	public void countMessages(Client c, int messages){
		System.out.println("Node " + c.toString() + " Passed " + messages + " messages.");
		
		int total = 0;
		boolean fault = false;
		for(Client cl : this.clients){
			int count = cl.getMessageCount();
			if(count > -1){
				total += count;
			}else{
				fault = true;
			}
		}
		
		if(!fault){
			System.out.println("Total Messags: " + total);
		}
		
	}

	public void stopElection() {
		for(Client c : this.clients){
			c.stop();
		}
	}

	public void startElection() {
		System.out.println("Starting election with " + this.clients.size() + " Nodes");
		for(Client c : this.clients){
			c.start(this.clients.size());
		}
	}

	public void dead(Client client) {
		System.out.println("Peer Left " + client.toString() + " (" + this.clients.size() + ") Nodes");
		this.clients.remove(client);
	}

	@Override
	public void run() {
		try {
			Socket s;
			while((s = this.server.accept()) != null){
				System.out.println("New Node");
				new Client(s);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class Client implements Runnable{
		private Socket socket;
		private String host = null;
		private int port = 0;
		private Thread thread;
		private PrintWriter out;
		
		private int msgCount = -1;
		
		public Client(Socket s){
			this.socket = s;
			
			try {
				this.out = new PrintWriter(this.socket.getOutputStream(), true);
				this.thread = new Thread(this);
				this.thread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
		}
		
		public void stop() {
			this.out.println("STOP");
		}

		public int getMessageCount(){
			return this.msgCount;
		}
		@Override
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				
				String portString = br.readLine();
				
				String host = this.socket.getRemoteSocketAddress().toString().substring(1);
				this.host = host.substring(0, host.indexOf(':'));
				this.port = Integer.parseInt(portString);
				annouceNode(this);
				String s;
				while((s = br.readLine())!=null){
					String[] bits = s.split(" ");
					switch(bits[0]){
					case "LEADER":
						stopElection();
						break;
					case "MESSAGE_COUNT":
						countMessages(this, Integer.parseInt(bits[1]));
						break;
					}
				}
				
				
			} catch (IOException e) {
				dead(this);
			}
			
		}
		
		public void start(int netSize){
			System.out.println(this.toString() + " starting election");
			this.out.println("START " + netSize);
		}
		
		public void announce(Client c){
			this.out.println("PEER " + c.toString());
		}
		
		public String toString(){
			return this.host + ":" + this.port;
		}
	}
	
	private class ConsoleListener implements Runnable{
		private BufferedReader br;
		private Thread thread;
		public ConsoleListener(){
			this.br = new BufferedReader(new InputStreamReader(System.in));
			this.thread = new Thread(this);
			this.thread.start();
		}
		@Override
		public void run() {
			while(true){
				try {
					String str = this.br.readLine();
					switch(str){
					case "start":
						startElection();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}

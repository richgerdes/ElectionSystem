import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Vector;

/* 
 * Protocol
 * 
 * Control:
 * Control tell peer to start election
 * - node starts with oldest peer and offers the connect
 * 		- if peer excepts
 * 			- finish and go idle
 *		- else 
 *			- go to next peer and repeat
 * - new connection
 * - stop election
 * 		- reply with message count
 * 
 * 
 * P2P
 * - ELECT pass current votes to neighbor
 * 		- if(current.electionPower > 0 && electing)
 * 			- accept
 * 		- else
 * 			- reject and pass backed peer
 * - REJECT peer can't accept votes (already passed along) 
 * 		- peer replies with who it passed votes to
 * - ACCEPT
 * 		- peer accepts votes and current stops searching
 * 
 */

public class ElectionNode implements Runnable{
	
	private static int DEFAULT_LISTEN_PORT = 20000;
	private static int DEFAULT_CONTROL_PORT = 19999;
	private static String DEFAULT_CONTROL_HOST = "localhost";
	
	private ServerSocket serverSocket;
	private ControlLink control;
	
	private int electionPower = 0;
	
	private boolean electing = false;
	
	private String backedHost = null;
	private int backedPort = -1;
	
	private Thread thread;
	
	private Vector<Peer> peers;
	private int numberPeers;
	
	private int messageCount = 0;
	
	public ElectionNode() throws IOException{
		this(DEFAULT_LISTEN_PORT);
	}

	public ElectionNode(int listenPort) throws IOException {
		this(listenPort, DEFAULT_CONTROL_PORT, DEFAULT_CONTROL_HOST);
	}
	
	public ElectionNode(int listenPort, int controlPort, String controlHost) throws IOException{
		this.serverSocket = new ServerSocket(listenPort);
		this.control = new ControlLink(this,controlHost, controlPort);
		this.thread = new Thread(this);
		this.electionPower = 0;
		this.peers = new Vector<Peer>();
		this.thread.start();
	}

	public void startElection(int count) {
		System.out.println("Starting Election...");
		this.electionPower = 1;
		this.electing = true;
		this.numberPeers = count;
		int i = 0;
		while(this.electing){
			Peer p = this.peers.get(i);
			if(this.Elect(p)){
				break;
			}
			
			i++;
			if(i >= this.peers.size())
				i %= this.peers.size();
		}
	}

	public void completeElection() {
		this.electing = false;
		this.control.annouceMessages();
	}
	
	public synchronized boolean Elect(Peer p){
		if(this.electionPower > 0){
			if(p.accept(this.electionPower)){
				this.electionPower = 0;
				this.electing = false;
				return true;
			}else{
				this.connect(p.getBackedHost(), p.getBackedPort());
			}
		}
		return false;
	}
	
	public synchronized boolean accept(int i){
		if(this.electing && this.electionPower > 0){
			this.electionPower += i;
			
			if(this.electionPower > this.numberPeers / 2){
				this.electing = false;
				this.control.passMessage("COUNT " +  this.electionPower);
			}
			return true;
		}
		return false;
	}
	
	public String getBackedHost(){
		return this.backedHost;
	}
	
	public int getBackedPort(){
		return this.backedPort;
	}

	public void connect(String host, int port) {
		for(Peer p : this.peers){
			if(p.getHost() == host && p.getPort() == port)
				return;
		}
		try {
			this.peers.add(new Peer(this, host, port));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getHost(){
		return this.serverSocket.getInetAddress().getHostAddress();
	}
	
	public int getPort(){
		return this.serverSocket.getLocalPort();
	}
	
	public String toString(){
		return this.getHost() + " " + this.getPort();
	}

	@Override
	public void run() {
		System.out.println("Node Connecting...");
		try {
			Socket c;
			while(this.serverSocket != null && (c = this.serverSocket.accept()) != null){
				System.out.println("Node: New Peer");
				this.peers.add(new Peer(this,c));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class ControlLink implements Runnable{

		private ElectionNode parent;
		private Socket socket;
		private PrintWriter out;
		private Thread thread;
		
		public ControlLink(ElectionNode p, String controlHost, int controlPort) throws UnknownHostException, IOException{
			this.parent = p;
			this.socket = new Socket(controlHost, controlPort);
			this.out = new PrintWriter(this.socket.getOutputStream(),true);
			this.thread = new Thread(this);
			this.thread.start();
		}

		public void annouceMessages() {
			this.out.println("MESSAGE_COUNT " + messageCount);
		}

		@Override
		public void run() {
			try {
				this.out.println(parent.serverSocket.getLocalPort());
				BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				String line;
				while((line = br.readLine()) != null){
					String[] words = line.split(" ");
					System.out.println("Test");
					if(words.length > 0){
						continue;
					}	
					switch (words[0]){
					case "START":
						int power = Integer.parseInt(words[1]);
						this.parent.startElection(power);
						break;
					case "PEER":
						String host = words[1];
						int port = Integer.parseInt(words[2]);
						this.parent.connect(host, port);
						break;
					case "STOP":
						this.parent.completeElection();
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void passMessage(String msg){
			if(this.out != null){
				this.out.println(msg);
			}
		}
	}
	
	private class Peer implements Runnable{
		
		private Socket socket;
		private PrintWriter out;
		private Thread thread;
		private ElectionNode parent;
		private boolean response = false;
		private boolean accepted = false;
		
		private String backedHost = null;
		private int backedPort = -1;
		
		private int port = -1;
		private String host = null;

		public Peer(ElectionNode electionNode, String host, int port) throws UnknownHostException, IOException {
			this(electionNode, new Socket(host, port));
		}

		public Peer(ElectionNode parent, Socket s) throws IOException{
			this.parent = parent;
			this.socket = s;
			
			this.out = new PrintWriter(this.socket.getOutputStream(), true);
			
			this.thread = new Thread(this);
			this.thread.start();
			
			this.passMessage(this.parent.toString());
		}
		
		public int getPort() {
			return this.port;
		}

		public String getHost() {
			return this.host;
		}

		public boolean accept(int power) {
			this.passMessage("ACCEPT " + power);
			while(!this.response){
				Thread.yield();
			}
			this.response = false;
			if(this.accepted){
				return true;
			}
			return false;
		}
		
		@Override
		public void run() {

			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));

				String portString = br.readLine();
				
				String host = this.socket.getRemoteSocketAddress().toString().substring(1);
				this.host = host.substring(0, host.indexOf(':'));
				this.port = Integer.parseInt(portString);
				
				String line;
				while((line = br.readLine()) != null){
					String[] words = line.split(" ");
					if(words.length > 0){
						continue;
					}
					switch (words[0]){
					case "ELECT":
						int power = Integer.parseInt(words[1]);
						if(this.parent.accept(power)){
							this.passMessage("ACCEPT");
						}else{
							this.passMessage("REJECT " + this.parent.getBackedHost() + " " + this.parent.getBackedPort());
						}
						break;
					case "REJECT":
						this.backedHost = words[1];
						this.backedPort = Integer.parseInt(words[2]);
						this.accepted = false;
						this.response = true;
						break;
					case "ACCEPT":
						this.accepted = true;
						this.response = true;
						break;
					}
				}
			} catch (IOException e) {
				peers.remove(this);
			}
		}
		
		public void passMessage(String msg){
			if(this.out != null){
				messageCount++;
				this.out.println(msg);
			}
		}
		
		public String getBackedHost(){
			return this.backedHost;
		}
		
		public int getBackedPort(){
			return this.backedPort;
		}
		
	}

}

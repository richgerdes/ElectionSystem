import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;


public class ControlServer implements Runnable {
	
	private static int DEFAULT_PORT = 19999;
	
	private ServerSocket server;
	private Thread thread;
	private Vector<Client> clients;
	
	public ControlServer() throws IOException{
		this(DEFAULT_PORT);
	}
	
	public ControlServer(int port) throws IOException{
		this.server = new ServerSocket(port);
		
		this.clients = new Vector<Client>();
		
		this.thread = new Thread(this);
		this.thread.start();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		
	}
	
	private class Client implements Runnable{
		private Socket socket;
		private String host = null;
		private int port = 0;
		private Thread thread;
		
		public Client(Socket s){
			this.socket = s;
			
			this.thread = new Thread(this);
			this.thread.start();
			
		}
		@Override
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
				
				String hostString = br.readLine();
				String[] hostP = hostString.split(" ");
				
				this.host = hostP[0];
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}

}

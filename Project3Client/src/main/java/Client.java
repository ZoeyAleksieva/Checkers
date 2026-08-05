import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;
import java.util.ArrayList;

public class Client extends Thread{

	Socket socketClient;
	ObjectOutputStream out;
	ObjectInputStream in;
	public String opponent;
	private String username;
	private String winner;
	GameBoard board;
	//boolean playing = true; //flip if they quit
	private Consumer<Serializable> callback;

	Client(Consumer<Serializable> call){
		callback = call;
	}

	//GETTERS AND SETTERS-----------------------------------------------------
	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername(){
		return username;
	}

	public void setOpponent(String opp) {
		this.opponent = opp;
	}

	public String getOpponent(){
		return opponent;
	}

	public void setWinner(String playerWin){
		this.winner = playerWin;
	}

	public String getWinner(){
		return winner;
	}
	//-------------------------------------------------------------------------

	public void run() {
		
		try {
		socketClient= new Socket("127.0.0.1",5555);
	    out = new ObjectOutputStream(socketClient.getOutputStream());
	    in = new ObjectInputStream(socketClient.getInputStream());
	    socketClient.setTcpNoDelay(true);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		while(true) {
			try {
			Message message = (Message ) in.readObject();
			System.out.println("I, " + username + " received message");
			callback.accept(message);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
    }

	public void send(Message data) {
		try {
			System.out.println("Sending object. I am " + username);
			out.writeObject(data);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

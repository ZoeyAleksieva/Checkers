import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.function.Consumer;

/* Biggest messes to fix.
* 	=> two loops for reading messages -> login vs game -> make it all together
*   => handleMessage() -> switch case for msg type -> refactor to handle each case externally
*   => Better yet -> create a message handler class and pass the vars it needs as params
*   => !!! HOLD ON -> handleMessage() handles messages from the Game Sesh too !!!
*   => Need to figure out better structure for server handling messages from game and client
*   => Message -> reorganize the types!
*
*   1) Figure out Message structure first
*   2) Refactor Server to handleMessage() more cleanly, possibly even message class
*   3) Refactor GameSession
*   4) Fix minor issues in GameBoard and Move
* */

public class Server{

	int count = 0;
	int clientCount = 0;
	TheServer server;
	private Consumer<Serializable> callback;

	HashMap<String, GameSession> games = new HashMap<>(); //mapped to BOTH players
	HashMap<GameSession, String > gameOrder = new HashMap<>(); //ex. [GAME #1]
	HashMap<String, String> player_opponent = new HashMap<>(); //keeps track of who plays who

	HashMap<String, ClientThread> clientThreads = new HashMap<>(); //clients who are LOGGED IN
	HashMap<String, ClientThread> pendingClientThreads = new HashMap<>(); //clients trying to log in or making an account

	HashMap<String, String> userPasswords = new HashMap<>();
	Queue<String> waitingPlayers = new LinkedList<>();

	Server(Consumer<Serializable> call){
		callback = call;
		userPasswords = loadUsers();
		server = new TheServer();
		server.start();
	}

	public HashMap<String, String> loadUsers(){
		HashMap<String, String> users = new HashMap<>();
		BufferedReader reader = null;
		File file = new File("userInfo.txt");
		if (!file.exists()) {
			return users; // return empty map
		}
		try {
			reader = new BufferedReader(new FileReader(file));
			String line;

			while ((line = reader.readLine()) != null) {
				String[] parts = line.split(":", 2);
				if (parts.length == 2) {
					users.put(parts[0], parts[1]);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return users;
	}

	private void tryMatchingPlayers(){
		//must have at least 2 players to match them
		if(waitingPlayers.size() < 2) return;
		//in java pop() is poll()
		String p1 = waitingPlayers.poll();
		String p2 = waitingPlayers.poll();
		GameSession game = new GameSession(p1, p2);
		count++;
		//put game twice in map cuz want to access for each player
		games.put(p1, game);
		games.put(p2, game);
		//same here, add both
		player_opponent.put(p1, p2);
		player_opponent.put(p2, p1);
		//number game to keep track in log
		gameOrder.put(game, "[GAME #" + count + "]");
		Message msg = new Message(MsgType.GAME_START, game.playerTypes, game.board.copyBoard());
		//both players get game obviously
		sendToPlayer(p1, msg);
		sendToPlayer(p2, msg);
		//notify GUI
		Message m = new Message(MsgType.GUI);
		m.content = "[NEW GAME]: P1: " + p1 + " versus P2: "+ p2;
		callback.accept(m);
	}

	private void sendToPlayer(String username, Message msg){
		try{
			ClientThread ct;
			if(clientThreads.containsKey(username)){
				ct = clientThreads.get(username);
			}else if(pendingClientThreads.containsKey(username)){
				ct = pendingClientThreads.get(username);
			}else{
				System.out.println("CLIENT THREAD NOT FOUND FOR --->" + username);
				return;
			}
			synchronized (ct.out){
				ct.out.writeObject(msg);
				ct.out.flush();
			}
		}catch(Exception e){
			Message m = new Message(MsgType.GUI);
			m.content = "[ERROR]: Message to " + username + " FAILED!";
			callback.accept(m);
		}
	}

	public void handleMessage(Message message) throws IOException {
		try{
			switch (message.type){
				case MOVE: {
					System.out.println("CASE MOVE");
					String user = message.username;
					System.out.println(user);
					GameSession game = games.get(user);

					//NULL CASE
					if(game == null){
						Message msg = new Message(MsgType.MOVE_FEEDBACK, user, "Error. No game.");
						sendToPlayer(user, msg);
						System.out.println("GAME NULL");
						break;
					}

					//HANDLE MOVE
					Message result = game.handleMove(user, message.move);

					//GAME SESSIONS SENDS RESULT MESSAGE AND SERVER INTERPRETS HERE
					if(result.type == MsgType.MOVE_FEEDBACK){
						//send only to user
						System.out.println("SENDING MOVE_FEEDBACK");
						sendToPlayer(user, result);
						Message m = new Message(MsgType.GUI);
						m.content = gameOrder.get(game) + " To: " + user + "Feedback: " + result.content;
						m.playerTurn = user;
						callback.accept(m);
					}else if(result.type == MsgType.MOVE_RESULT || result.type == MsgType.GAME_OVER){
						System.out.println("SENDING MOVE_RESULT//GAME_OVER");
						//send applied moves and game over to both
						String opp = player_opponent.get(user);

						Piece[][] snapshot1 = game.board.copyBoard();
						Message msg = new Message(result.type, snapshot1, result.content);
						msg.playerTurn = result.playerTurn;
						msg.winner = result.winner;
						msg.username = "ignore";
						sendToPlayer(user, msg);
						sendToPlayer(opp, msg);

						Message m = new Message(MsgType.GUI);
						m.content = gameOrder.get(game) + " " + user + " move result: " + result.content;
						callback.accept(m);
					}else{ //DRAW
						String player = result.username;
						result.winner = "Draw";
						sendToPlayer(player, result);
						sendToPlayer(player_opponent.get(player), result);
					}
					break;
				}
				case QUIT: {
					String user = message.username;
					String opp = player_opponent.get(user);
					System.out.println("Removing " + user);
					//remove from queue, threads, playeropp, games
					waitingPlayers.remove(user);
					clientThreads.remove(user);
					//player_opponent.remove(user);

					//HAVE TO REMOVE BOTH GAME ENTRIES!!
					GameSession game = games.remove(user);

					//player could have disconnected!
					if(opp != null){
						games.remove(opp);
						//player_opponent.remove(opp);
						//need to tell opponent they won
						Message m = new Message(MsgType.GAME_OVER, opp);
						m.username = null;
						sendToPlayer(opp, m);
						Message msg = new Message(MsgType.GUI);
						msg.content = gameOrder.get(game) + " " + user + " quit. " + opp + " wins the game.";
						callback.accept(msg);
					}
					break;
				}
				case CLIENT_CHAT: {
					String sendTo = message.recipient;
					ClientThread rec = clientThreads.get(sendTo);
					rec.out.writeObject(message);
					clientThreads.get(message.sender).out.writeObject(message);
					break;
				}
				case PLAY_AGAIN: {
					String player = message.username;
					GameSession game = games.get(player);
					if (player.equals(game.player1)){
						game.p1PlayAgain = true;
						if(game.p2PlayAgain){
							game.newGame();
							Message msg = new Message(MsgType.GAME_START, game.playerTypes, game.board.copyBoard());
							sendToPlayer(player, msg);
							sendToPlayer(player_opponent.get(player), msg);
						}
					}else if(player.equals(game.player2)){
						game.p2PlayAgain = true;
						if(game.p1PlayAgain){
							game.newGame();
							Message msg = new Message(MsgType.GAME_START, game.playerTypes, game.board.copyBoard());
							sendToPlayer(player, msg);
							sendToPlayer(player_opponent.get(player), msg);
						}
					}else{
						System.out.println("PLAY_AGAIN ERROR");
					}
					break;
				}
				default:{
					Message msg = new Message(MsgType.GUI);
					msg.content = "You done messed up.Can't rec. message type";
					System.out.println(msg.content);
					//callback.accept(msg);
					break;
				}
			}
		}catch (Exception e) {
			Message m = new Message(MsgType.GUI);
			m.content = "[ERROR]: handleMessage()" + e.getMessage();
			callback.accept(m);
		}
	}

	public void userQuitCleanUp(String username, Exception e){
		System.out.println("EXCEPTION WHEN READING MESSAGE OBJECT");
		System.out.println(username + " quit");

		String opp = player_opponent.get(username);
		if(opp != null){
			Message m = new Message(MsgType.GAME_OVER, opp);
			m.username = null;
			System.out.println("Sending to " + opp);
			sendToPlayer(opp, m);
		}else{
			System.out.println("NULL OPP");
		}
		//Clean up code
		waitingPlayers.remove(username);
		clientThreads.remove(username);

		System.out.println("OPP IS " + opp);
		player_opponent.remove(username);
		GameSession game = games.get(username);
		if(game != null){
			games.remove(username);
			if(opp != null){
				games.remove(opp);
			}
		}
		//need to tell opponent they won
		if(opp != null){
			player_opponent.remove(opp);
		}

		Message msg = new Message(MsgType.GUI);
		msg.content = gameOrder.get(game) + " " + username + " quit. " + opp + " wins the game.";
		callback.accept(msg);
		e.printStackTrace();
	}

	public void saveUserInfo(String username, String password){
		try {
			File file = new File("userInfo.txt");

			// create file if it doesn't exist
			if (!file.exists()) {
				file.createNewFile();
			}

			// true = append mode
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
				writer.write(username + ":" + password);
				writer.newLine();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//THE_SERVER------------------------------------------------------------------------------------------------------
	public class TheServer extends Thread{
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){
		    System.out.println("Server is waiting for a client!");

		    while(true) {
				ClientThread c = new ClientThread(mysocket.accept(), clientCount);
				Message m = new Message(MsgType.GUI);
				m.content = "A client has connected to the server." ;
				pendingClientThreads.put(String.valueOf(clientCount), c);
				clientCount++;
				callback.accept(m);
				c.start();
			    }
			}//end of try
			catch(Exception e) {
			Message m = new Message(MsgType.GUI);
			m.content = "Server socket did not launch";
			callback.accept(m);
			}
		}//end of while
	}

	//CLIENT_THREAD------------------------------------------------------------------------------------------------------
		class ClientThread extends Thread{
			Socket connection;
			ObjectInputStream in;
			ObjectOutputStream out;
			String username;
			String requestedName;
			int clientNumber; //server assigns
			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.clientNumber = count;
			}

			public void run(){
					
				try {
					out = new ObjectOutputStream(connection.getOutputStream());
					in = new ObjectInputStream(connection.getInputStream());
					connection.setTcpNoDelay(true);

					while (true) { //[LOGIN LOOP]
						Message login = (Message) in.readObject();
						if (login.type != MsgType.LOGIN1 && login.type != MsgType.LOGIN2 && login.type != MsgType.NEW_PLAYER) {
							System.out.println("Client should not be sending non-LOGIN type messages yet!");
							continue;
						}

						//LOGIN1 ----- user just sends username
						if (login.type == MsgType.LOGIN1) {
							requestedName = login.username;
							//If username exists, request password. If not, tell them they are new
							if (userPasswords.containsKey(requestedName)) {
								if (clientThreads.containsKey(requestedName)) {
									//USER IS ALREADY LOGGED IN
									String error = "Client " + requestedName + " is already logged in. Enter unique username to create an account.";
									System.out.println("Client " + requestedName + " is already logged in.");
									Message msg = new Message(MsgType.LOGIN1, error); //error is in USERNAME field
									sendToPlayer(String.valueOf(clientNumber), msg);
								} else {
									//server sends same message back == "Hey I'm ready for your password"
									System.out.println("Client " + requestedName + " found. Requesting password...");
									//here say username = requestedName? but what if wrong password and tried another?
									//well then client goes back to login1 and redoes it
									sendToPlayer(String.valueOf(clientNumber), login);
								}
							} else {
								System.out.println("Username " + requestedName + "not found. Waiting for user to make password.");
								Message msg = new Message(MsgType.NEW_PLAYER);
								sendToPlayer(String.valueOf(clientNumber), msg);
							}
						} else if(login.type == MsgType.LOGIN2){ //LOGIN2 --- username was found, now accept/deny password
							String client = login.username;
							String password = login.password;
							if (password.equals(userPasswords.get(client))) {
								System.out.println(client + "'s password accepted");
								if (!client.equals(requestedName)) {
									System.out.println("CLIENT SENDS DIFF USERNAME FOR LOGIN1 & LOGIN2");
									continue;
								}
								//OFFICIALLY ACCEPTS PLAYER
								username = requestedName;
								clientThreads.put(username, this);


								waitingPlayers.add(username);

								//server sends same message back but changes content
								login.content = "Welcome " + username + ". Waiting for opponent...";
								sendToPlayer(username, login);

								//Update GUI
								Message m = new Message(MsgType.GUI);
								m.content = "[NEW USER ONLINE]: " + login.username;
								callback.accept(m);
								tryMatchingPlayers();
								break; //CAN ONLY BREAK OUT OF LOGIN LOOP AFTER LOGGED ON SUCCESSFULLY
							} else { //PASSWORD IS WRONG
								sendToPlayer(String.valueOf(clientNumber), new Message(MsgType.LOGIN2));
							}
						}else{
							 //login type is NEW_PLAYER
							//user will only send a NEW_PLAYER message if server sends NEW_PLAYER which means username IS unique
							//^^^so what is stopping you from saying username is requested name? BCS CLIENT THREADS TIED TO USERNAME
							String client = login.username;
							String password = login.password;
							userPasswords.put(client, password);
							saveUserInfo(client,password);

							//OFFICIALLY ACCEPTS PLAYER
							username = requestedName;
							clientThreads.put(username, this);
							waitingPlayers.add(username);

							//server sends same message back but changes content and type
							login.content = "Welcome " + username + ". Waiting for opponent...";
							login.type = MsgType.LOGIN2;
							sendToPlayer(username, login);

							//Update GUI
							Message m = new Message(MsgType.GUI);
							m.content = "[NEW USER ONLINE]: " + login.username;
							callback.accept(m);
							tryMatchingPlayers();
							break; //CAN ONLY BREAK OUT OF LOGIN LOOP AFTER LOGGED ON SUCCESSFULLY
						}
					}
				}catch(Exception e) {
					System.out.println("Streams not open");
					userQuitCleanUp(username, e);
				}
				 while(true) { //[GAME LOOP]
					try {
						Message data = (Message) in.readObject();
						System.out.println("Message from " + username + " received");
						handleMessage(data);
					}
					catch(Exception e) {
						userQuitCleanUp(username, e);
						break;
					}
				 }
			}//end of run
			
		}//end of client thread
}

//username enter only
//IF recognized -> Change label to say enter password and change prompt text -> BUTTON TO GO BACK!!! maybe at top left? -> password message -> LOGIN
//IF NOT -> pop up asking if you want to create an account w/ *username* - no->back to og screen - yes --> change message to make password -> NEW_PLAYER

//class Player:
/*
* hashmap of opponents and active games
*
* */
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
//TODO: Error handle --> if client chooses a username that's just a number, it might get confused with server's clientNumber
//TODO: Stay organized --> name messages sent to client "msg" and name the ones sent to the GUI log "m"
//TODO: Separate listViews on GUI for login feedback versus game loop feedback

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
		Message msg = Message.gameStart(game.playerTypes, game.board.copyBoard());
		//both players get game obviously
		sendToPlayer(p1, msg);
		sendToPlayer(p2, msg);
		//notify GUI
		String log = "[NEW GAME]: P1: " + p1 + " versus P2: "+ p2;
		Message m = Message.updateGUIlog(log);
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
			String log = "[ERROR]: Message to " + username + " FAILED!";
			Message m =  Message.updateGUIlog(log);
			callback.accept(m);
		}
	}

	private void sendMoveFeedback(String username, Message gameSessionFeedback, GameSession game){
		sendToPlayer(username, gameSessionFeedback);
		String log = gameOrder.get(game) + " To: " + username + "Feedback: " + gameSessionFeedback.feedback;
		Message m = Message.updateGUIlog(log);
		callback.accept(m);
	}

	private void sendMoveResult(String username, Message gameSessionFeedback, GameSession game){
		System.out.println("SENDING MOVE_RESULT");

		String opp = player_opponent.get(username);
		sendToPlayer(username, gameSessionFeedback);
		sendToPlayer(opp, gameSessionFeedback);

		String log = gameOrder.get(game) + " " + username + " move result: " + gameSessionFeedback.moveConfirmMsg;
		Message m = Message.updateGUIlog(log);
		callback.accept(m);
	}

	private void sendGameOver(String username, Message gameSessionFeedback, GameSession game){
		System.out.println("SENDING GAME_OVER");

		String opp = player_opponent.get(username);
		sendToPlayer(username, gameSessionFeedback);
		sendToPlayer(opp, gameSessionFeedback);

		String log = gameOrder.get(game) + " Game Over!" + gameSessionFeedback.winner + " wins!";
		Message m = Message.updateGUIlog(log);
		callback.accept(m);
	}

	private void sendGameDraw(String username, Message gameSessionFeedback, GameSession game){
		System.out.println("SENDING GAME_OVER");

		String opp = player_opponent.get(username);
		sendToPlayer(username, gameSessionFeedback);
		sendToPlayer(opp, gameSessionFeedback);

		String log = gameOrder.get(game) + " Draw!";
		Message m = Message.updateGUIlog(log);
		callback.accept(m);
	}

	private void handleMove(Message clientMsg, String username){
		GameSession game = games.get(username);

		if(game == null){
			Message msg = Message.moveFeedback("Error. No active game.");
			sendToPlayer(username, msg);
			System.out.println("GAME IS NULL");
			return;
		}

		Message gameSessionFeedback = game.handleMove(username, clientMsg.move);

		switch(gameSessionFeedback.type){
			case MOVE_FEEDBACK:
				sendMoveFeedback(username, gameSessionFeedback, game);
				break;
			case MOVE_RESULT:
				sendMoveResult(username, gameSessionFeedback, game);
				break;
			case GAME_OVER:
				sendGameOver(username, gameSessionFeedback, game);
				break;
			case DRAW:
				sendGameDraw(username, gameSessionFeedback, game);
				break;
		}
	}

	//Player message types: MOVE, CLIENT_CHAT, PLAY_AGAIN, QUIT
	public void handleMessage(Message clientMsg, String username) throws IOException {
		try{
			switch (clientMsg.type){
				case MOVE: {
					GameSession game = games.get(username);

					if(game == null){
						Message msg = Message.moveFeedback("Error. No active game.");
						sendToPlayer(username, msg);
						System.out.println("GAME IS NULL");
						break;
					}

					handleMove(clientMsg, username);
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

			private void handleLoginUsername(Message login){
				requestedName = login.username;
				if (userPasswords.containsKey(requestedName)) {
					if (clientThreads.containsKey(requestedName)) {
						//USER IS ALREADY LOGGED IN
						String errorFeedback = "Client " + requestedName + " is already logged in. Enter unique username to create an account.";
						Message msg = Message.rejectUser(errorFeedback);
						sendToPlayer(String.valueOf(clientNumber), msg);

						System.out.println("Client " + requestedName + " is already logged in.");
					} else {
						//ASK FOR PASSWORD
						Message msg = new Message(MsgType.LOGIN2);
						sendToPlayer(String.valueOf(clientNumber), msg);

						System.out.println("Client " + requestedName + " found. Requesting password...");
					}
				} else {
					Message msg = new Message(MsgType.NEW_PLAYER);
					sendToPlayer(String.valueOf(clientNumber), msg);

					System.out.println("Username " + requestedName + "not found. Waiting for new user to create a password.");
				}
			}
			private boolean handleLoginPassword(Message login){
				String password = login.password;
				if (password.equals(userPasswords.get(requestedName))) {
					//OFFICIALLY ACCEPTS PLAYER
					username = requestedName;
					clientThreads.put(username, this);
					waitingPlayers.add(username);
					System.out.println(username + "'s password got accepted");

					Message msg = new Message(MsgType.ACCEPT_PASSWORD);
					//login.content = "Welcome " + username + ". Waiting for opponent...";
					sendToPlayer(username, msg);

					//Update GUI
					Message m = Message.updateGUIlog("[NEW USER ONLINE]: " + username);
					callback.accept(m);

					tryMatchingPlayers();
					return true; //User accepted - BREAK OUT of login loop
				} else { //PASSWORD IS WRONG
					Message msg = new Message(MsgType.REJECT_USERNAME);
					sendToPlayer(String.valueOf(clientNumber), msg);
					System.out.println(requestedName + "'s password got denied");
					return false; //User denied - DO NOT break out of login loop
				}
			}
			private void handleNewPlayer(Message login){
				username = requestedName;
				String password = login.password;

				userPasswords.put(username, password);
				saveUserInfo(username,password);

				clientThreads.put(username, this);
				waitingPlayers.add(username);

				Message msg = new Message(MsgType.ACCEPT_PASSWORD);
				sendToPlayer(username, msg);

				//Update GUI
				Message m = Message.updateGUIlog("[NEW USER ONLINE]: " + username);
				callback.accept(m);
				tryMatchingPlayers();
			}

			public void run(){
					
				try {
					out = new ObjectOutputStream(connection.getOutputStream());
					in = new ObjectInputStream(connection.getInputStream());
					connection.setTcpNoDelay(true);
					//[LOGIN LOOP]----------------------------------------------------------------------------------------------
					while (true) {
						Message login = (Message) in.readObject();
						if (login.type != MsgType.LOGIN1 && login.type != MsgType.LOGIN2 && login.type != MsgType.NEW_PLAYER) {
							System.out.println("Client should not be sending non-LOGIN type messages yet!");
							continue;
						}

						if (login.type == MsgType.LOGIN1) {
							handleLoginUsername(login);
						} else if(login.type == MsgType.LOGIN2){
							boolean userAccepted = handleLoginPassword(login);
							if(userAccepted){break;}
						}else{ //NEW_PLAYER
							handleNewPlayer(login);
							break;
						}
					}
				}catch(Exception e) {
					System.out.println("Streams not open (login loop)");
					userQuitCleanUp(username, e);
				}
				//[GAME LOOP]---------------------------------------------------------------------------------------------------
				 while(true) {
					try {
						Message clientMessage = (Message) in.readObject();
						handleMessage(clientMessage, username);
					}
					catch(Exception e) {
						System.out.println("Streams not open (game loop)");
						userQuitCleanUp(username, e);
						break;
					}
				 }
			}//end of run
			
		}//end of client thread
}
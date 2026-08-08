import java.io.Serializable;
import java.util.HashMap;

public class Message implements Serializable {

    private static final long serialVersionUID = 42L;
    public MsgType type;

    //LOGIN1
    public String username;

    //LOGIN2
    public String password;

    //REJECT_USERNAME
    public String rejectUserFeedback;

    //GAME_START -> for server to inform clients which player they are and give board
    HashMap<String, Piece> playerTypes = new HashMap<>();
    public Piece[][] board;

    //MOVE -> for client to request move
    public Move move;

    //MOVE_FEEDBACK -> feedback ONLY for player attempting to make move
    public String feedback;

    //MOVE_RESULT-> move has been applied, sends updated board to BOTH
    public String playerTurn;
    public String moveConfirmMsg;

    //GAME_OVER -> send to both the final board and who won
    public String winner; //if no one wins, winner is "Draw"

    //CLIENT_CHAT
    public String message;
    public String sender;
    public String recipient;

    //GUI
    public String guiFeedback;

    //NEW_PLAYER -> server sends if client sends LOGIN1 and user is NOT found
    //LOGIN2 -> server sends to prompt user for password
    //ACCEPT_PASSWORD, REJECT_PASSWORD
    Message(MsgType msgType){
        this.type = msgType;
    }

    //LOGIN1 - user sends username
    public static Message loginName(String username){
        Message msg = new Message(MsgType.LOGIN1);
        msg.username = username;
        return msg;
    }

    //REJECT_USERNAME - server sends if user is already logged in
    public static Message rejectUser(String feedback){
        Message msg = new Message(MsgType.REJECT_USERNAME);
        msg.rejectUserFeedback = feedback;
        return msg;
    }

   //LOGIN2 - client sends password
    public static Message loginPswrd(String password){
        Message msg = new Message(MsgType.LOGIN2);
        msg.password = password;
        return msg;
    }

    //GAME_START - server sends initial game board and player info
    public static Message gameStart(HashMap<String, Piece> playerTypes, Piece[][] board){
        Message msg = new Message(MsgType.GAME_START);
        msg.playerTypes = playerTypes;
        msg.board = board;
        return msg;
    }

    //MOVE
    public static Message move( Move move){
        Message msg = new Message(MsgType.MOVE);
        msg.move = move;
        return msg;
    }

    //MOVE_FEEDBACK
    public static Message moveFeedback(String feedback){
        Message msg = new Message(MsgType.MOVE_FEEDBACK);
        msg.feedback = feedback;
        return msg;
    }

    //NEW_PLAYER
    public static Message newPlayer(String password){
        Message msg = new Message(MsgType.NEW_PLAYER);
        msg.password = password;
        return msg;
    }

    //MOVE_RESULT
    public static Message moveResult(Piece[][] board, String moveConfirmMsg, String playerTurn) {
        Message msg = new Message(MsgType.MOVE_RESULT);
        msg.board = board;
        msg.moveConfirmMsg  = moveConfirmMsg;
        msg.playerTurn = msg.playerTurn;
        return msg;
    }

    //GAME_OVER -  server message is "WINNER" or "Draw"
    public static Message gameOver(String winner){
        Message msg = new Message(MsgType.GAME_OVER);
        msg.winner = winner;
        return msg;
    }

    //CLIENT_CHAT
    public static Message chat(String sender, String recipient, String message){
        Message msg = new Message(MsgType.CLIENT_CHAT);
        msg.sender = sender;
        msg.recipient = recipient;
        msg.message = message;
        return msg;
    }

    //GUI
    public static Message updateGUIlog(String feedback){
        Message msg = new Message(MsgType.GUI);
        msg.guiFeedback = feedback;
        return msg;
    }
}

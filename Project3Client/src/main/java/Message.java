import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

//hashmap groupName, recipients
public class Message implements Serializable {

    private static final long serialVersionUID = 42L;
    public MsgType type;

    //LOGIN1, LOGIN2, NEW_PLAYER
    public String username;
    public String password;

    //GAME_START -> for server to inform clients which player they are and give board
    HashMap<String, Piece> playerTypes = new HashMap<>();
    public Piece[][] board;

    //MOVE -> for client to request move
    public Move move;

    //MOVE_FEEDBACK -> feedback ONLY for player attempting to make move
    public String content;

    //MOVE_RESULT-> move has been applied, sends updated board
    public String playerTurn;
    //also uses "board"
    //also uses "content"

    //GAME_OVER -> send to both the final board and who won
    public String winner; //if draw, winner is "Draw"

    //CLIENT_CHAT
    public String message;
    public String sender;
    public String recipient;

    //GAME_START - server sends initial game board and player info
    Message(MsgType msgType, HashMap<String, Piece> playerTypes, Piece[][] board){
        this.type = msgType;
        this.playerTypes = playerTypes;
        this.board = board;
    }

    //LOGIN1 - client message is username ->
    //LOGIN1 - if approved, server sends back same message, if not-> message is "Error. Username already logged in."
    //GAME_OVER -  server message is WINNER / "Draw"
    Message(MsgType msgType, String message){
        this.type = msgType;
        if(msgType == MsgType.LOGIN1){
            this.username = message;
        }else if(msgType == MsgType.GAME_OVER){
            this.winner = message;
        }else if(msgType == MsgType.LOGIN2){
            this.password = message;
        }
    }

    //NEW_PLAYER -> server sends if client sends LOGIN1 and user is NOT found
    //LOGIN2 -> when password is wrong, server sends only msgType (client recognizes rejection through null fields)
    Message(MsgType msgType){
        this.type = msgType;
    }

    //MOVE
    Message(MsgType msgType, Move move){
        this.type = msgType;
        this.move = move;
    }

    //NEW_PLAYER -> user sends username and password
    //LOGIN 2 - user sends name and password, if approved, server sends back same message but content says "Waiting for opp.."
    //MOVE_FEEDBACK -- user is for server to know who to give the INDIVIDUAL feedback (content == feedback)
    Message(MsgType msgType, String user, String content){
        this.type = msgType;
        this.username = user;
        if(msgType == MsgType.MOVE_FEEDBACK){
            this.content = content;
        }else if(msgType == MsgType.LOGIN2 || msgType == MsgType.NEW_PLAYER){
            this.password = content;
        }
    }

    //MOVE_RESULT -> content is confirmation of move/jump
    Message(MsgType msgType, Piece[][] board, String content){
        //type = msgType;
        this.type = msgType;
        this.board = board;
        this.content = content;
    }

    //CLIENT_CHAT - for client to request to send message
    //TODO: THIS WAS CONTENT YOU CHANGED TO MESSAGE HERE!!!!!!!!!!!!
    Message(MsgType msgType, String sender, String recipient, String content){
        this.type = msgType;
        this.sender = sender;
        this.recipient = recipient;
        this.message = content;
    }

}

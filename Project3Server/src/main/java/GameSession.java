import java.util.ArrayList;
import java.util.HashMap;

//TODO: Refactor handleMove() but all else is perf

public class GameSession {

    String player1;
    String player2;
    GameBoard board;
    Piece currentTurn;
    boolean mustContinue;
    int mustMoveRow; //the piece at this loc has to keep moving
    int mustMoveCol;
    boolean gameOver;
    String winner;
    boolean p1PlayAgain;
    boolean p2PlayAgain;
    HashMap<String, Piece> playerTypes = new HashMap<>();
    int movesWithoutCapture;

    GameSession(String player1, String player2){
        this.player1 = player1;
        this.player2 = player2;
        this.board = new GameBoard();
        currentTurn = Piece.BLACK;
        playerTypes.put(player1, Piece.BLACK);
        playerTypes.put(player2, Piece.RED);
        movesWithoutCapture = 0;
    }

    public void newGame(){
        this.board = new GameBoard();
        this.currentTurn = Piece.BLACK;
        this.mustContinue = false;
        this.mustMoveRow = -1;
        this.mustMoveCol = -1;
        this.gameOver = false;
        this.winner = null;
        movesWithoutCapture = 0;
    }

    /* HANDLE MOVE---------------------------------------------------
    *  - Only accept if it's that player's turn
    *  - Differentiate between normal move and jump
    *  - Get the list of what all the possible moves they could have made were
    *  - Reject move if they could have jumped but didn't. Send forced?
    *  - If jump -> apply the move. Include if mustContinue jumping in message and forced locs
    *  - If jumped -> Check if game is over by calling function with CURRENT PLAYER and communicate when it is
    *  - Else if normal move -> apply and send board updates to both with switched turn
    * Draw? Stuck playing? */

    public Message handleMove(String username, Move move){
        System.out.print(username + " wants to make move: ");
        move.printMove();

        //----------------------------------------------------------------
        // if(!playerTurn(username)) {return new Message(MsgType.MOVE_FEEDBACK, username, "Wait for your turn.");}
        Piece player = playerTypes.get(username);
        if(player != currentTurn){
            return new Message(MsgType.MOVE_FEEDBACK, username, "Wait for your turn.");
        }
        //Before assessing move, check if it's a mustContinue
        //technically can't this be if ((mustContinue) && (move.fromRow != mustMoveRow || move.fromCol != mustMoveCol))
        if(mustContinue){
            if(move.fromRow != mustMoveRow || move.fromCol != mustMoveCol){
                return new Message(MsgType.MOVE_FEEDBACK, username,"Must continue jumping with same piece.");
            }
        }
        //-----------------------------------------------------------------
        //if the move is not in the possible moves or didn't jump
        //TODO: Make these OBJECTS so they can be changed inside the function
        boolean valid = false;
        boolean canJump = false;
        //CheckMoveValidity(valid, canJump);
        ArrayList<Move> possibleMoves = board.getPlayerMoves(player);
        System.out.println("POSSIBLE MOVES FOR " + player);
        printPossibleMoves(possibleMoves);

        for(Move m : possibleMoves){
            if (m.isJump){
                canJump = true;
            }
            if(m.equals(move)){
                valid = true;
            }
        }
        //------------------------------------------------------------------
        if(canJump && !move.isJump){
            return new Message(MsgType.MOVE_FEEDBACK, username, "You must jump.");
        }
        if(!valid){return new Message(MsgType.MOVE_FEEDBACK, username, "Invalid Move.");}

        // function applyMove()-----------------------------------------
        // return applyMove(username, move)
        if(Math.abs(move.fromRow - move.toRow) == 2){move.isJump = true;}
        if(move.isJump){
            //return applyJump(username, move)
            movesWithoutCapture = 0;
            board.movePiece(move);
            boolean canContinue = board.canJumpAgain(move.toRow, move.toCol);
            String jumpInfo = username + " captured a piece!";
            Message msg = new Message(MsgType.MOVE_RESULT, board.copyBoard(), jumpInfo);
            //add extra info to message if they can continue jump
            if(canContinue){
                mustContinue = true;
                mustMoveRow = move.toRow;
                mustMoveCol = move.toCol;
                msg.playerTurn = username;
                msg.content = username + " should keep jumping.";
                return msg;
            }else{
                mustContinue = false;
            }
            //if jump happened and can't continue, check for game over
            Message gameOverMsg = checkGameOver(player, username);
            if(gameOverMsg != null) {return gameOverMsg;}
            //if they can't continue jumping, switch turns
            currentTurn = (currentTurn == Piece.BLACK) ? Piece.RED : Piece.BLACK;
            msg.playerTurn = (currentTurn == Piece.BLACK) ? player1 : player2;
            return msg;
        }
        //------------------------------------------------------------------
        //if NOT jump, it's just a normal move
        movesWithoutCapture++;
        if(movesWithoutCapture >= 40){
            Message draw = new Message(MsgType.DRAW);
            draw.username = username;
            draw.winner = "Draw";
            return draw;
        }
        //------------------------------------------------------------------
        //return applyNormalMove()
        board.movePiece(move);
        Message gameOverMsg = checkGameOver(player, username);
        if(gameOverMsg != null) {return gameOverMsg;}
        currentTurn = (currentTurn == Piece.BLACK) ? Piece.RED : Piece.BLACK;
        Message msg = new Message(MsgType.MOVE_RESULT, board.copyBoard(), "Move Applied.");
        msg.playerTurn = (currentTurn == Piece.BLACK) ? player1 : player2;
        mustContinue = false;
        board.printBoard();
        System.out.print(username + " MADE move: ");
        move.printMove();
        return msg;
    }

    private Message checkGameOver(Piece player, String username){
        System.out.println("CHECKING game over");
        if(board.isGameOver(player)){
            System.out.println("GAME OVER");
            gameOver = true;
            winner = username;
            return new Message(MsgType.GAME_OVER, winner);
        }
        return null;
    }

    private void printPossibleMoves(ArrayList<Move> possibleMoves){
        for(Move move : possibleMoves){
            move.printMove();
        }
    }
}

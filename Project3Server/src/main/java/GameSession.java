import java.util.ArrayList;
import java.util.HashMap;


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
    *  - If jump -> apply the move. Include if mustContinue jumping in message and include forced location
    *  - If jumped -> Check if game is over by calling function with CURRENT PLAYER and communicate when it is
    *  - Else if normal move -> apply and send board updates to both with switched turn
    * ---------------------------------------------------------------*/

    private boolean isPlayerTurn(Piece playerColor){
        return playerColor == currentTurn;
    }

    private boolean[] getMoveValidity(String username, Piece playerColor, Move move){
        boolean valid = false;
        boolean canJump = false;
        ArrayList<Move> possibleMoves = board.getPlayerMoves(playerColor);
        System.out.println("POSSIBLE MOVES FOR " + playerColor);
        printPossibleMoves(possibleMoves);

        for(Move m : possibleMoves){
            if (m.isJump){
                canJump = true;
            }
            if(m.equals(move)){
                valid = true;
            }
        }
        return new boolean[]{valid, canJump};
    }

    public Message handleMove(String username, Move move){
        System.out.print(username + " wants to make move: ");
        move.printMove();
        Piece playerColor = playerTypes.get(username);
        //----------------------------------------------------------------
        if(!isPlayerTurn(playerColor)) {return Message.moveFeedback("Wait for your turn.");}

        if (mustContinue && (move.fromRow != mustMoveRow || move.fromCol != mustMoveCol)){
            return Message.moveFeedback("Must continue jumping with same piece.");
        }
        //-----------------------------------------------------------------
        boolean[] validity = getMoveValidity(username, playerColor, move);
        boolean valid = validity[0];
        boolean canJump = validity[1];

        if(canJump && !move.isJump){
            return Message.moveFeedback("You must jump.");
        }
        if(!valid){return Message.moveFeedback("Invalid Move.");}
        //------------------------------------------------------------------
        //JUMP
        if(Math.abs(move.fromRow - move.toRow) == 2){move.isJump = true;}
        if(move.isJump){
            return applyJump(username, playerColor, move);
        }
        //------------------------------------------------------------------
        //NOT jump
        movesWithoutCapture++;
        if(movesWithoutCapture >= 40){
            return new Message(MsgType.DRAW);
        }
        return applyMove(username, playerColor, move);
    }

    private Message applyJump(String username, Piece playerColor, Move move){
        movesWithoutCapture = 0;
        board.movePiece(move);
        boolean canContinue = board.canJumpAgain(move.toRow, move.toCol);

        if(canContinue){
            mustContinue = true;
            mustMoveRow = move.toRow;
            mustMoveCol = move.toCol;
            String feedback = username + " should keep jumping.";
            return Message.moveResult(board.copyBoard(), feedback, username);
        }else{
            mustContinue = false;
        }

        Message gameOverMsg = checkGameOver(playerColor, username);
        if(gameOverMsg != null) {return gameOverMsg;}

        currentTurn = (currentTurn == Piece.BLACK) ? Piece.RED : Piece.BLACK;
        String playerTurn = (currentTurn == Piece.BLACK) ? player1 : player2;
        String jumpFeedback = username + " captured a piece!";
        return Message.moveResult(board.copyBoard(), jumpFeedback, playerTurn);
    }

    private Message applyMove(String username, Piece playerColor, Move move){
        board.movePiece(move);
        Message gameOverMsg = checkGameOver(playerColor, username);
        if(gameOverMsg != null) {return gameOverMsg;}

        currentTurn = (currentTurn == Piece.BLACK) ? Piece.RED : Piece.BLACK;
        String playerTurn = (currentTurn == Piece.BLACK) ? player1 : player2;
        mustContinue = false;

        board.printBoard();
        System.out.print(username + " MADE move: ");
        move.printMove();

        String moveConfirm = username + "'s move was applied";
        return Message.moveResult(board.copyBoard(), moveConfirm, playerTurn);
    }

    private Message checkGameOver(Piece player, String username){
        System.out.println("CHECKING game over");
        if(board.isGameOver(player)){
            System.out.println("GAME OVER");
            gameOver = true;
            winner = username;
            return Message.gameOver(winner);
        }
        return null;
    }

    private void printPossibleMoves(ArrayList<Move> possibleMoves){
        for(Move move : possibleMoves){
            move.printMove();
        }
    }
}

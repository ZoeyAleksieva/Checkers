import java.util.ArrayList;
/* GAME FLOW
* Server accepts message with Move from client
* Server finds client board
* Server gets all moves for client
* Server approves or denies move
* */
public class GameBoard {
    private Piece[][] board;

    GameBoard(){
        board = new Piece[8][8];
        initBoard();
        printBoard();
    }

    private void initBoard(){
        //black on the bottom goes first
        for(int row = 0; row < 8; row++){
            for(int col = 0; col < 8; col++){
                if((row + col) % 2 != 0){
                    if(row < 3){
                        board[row][col] = Piece.RED;
                    }else if(row > 4){
                        board[row][col] = Piece.BLACK;
                    }else{
                        board[row][col] = Piece.EMPTY;
                    }
                } else { board[row][col] = Piece.EMPTY;}
            }
        }
    }

    // b - black, r - red, "   " - empty, B - black king, R - red king
    public void printBoard(){
        for(Piece[] row : board){
            for(Piece cell : row){
                String piece = "";
                switch(cell){
                    case EMPTY:
                        piece = " ";
                        break;
                    case RED:
                        piece = "r";
                        break;
                    case BLACK:
                        piece = "b";
                        break;
                    case RED_KING:
                        piece = "R";
                        break;
                    case BLACK_KING:
                        piece = "B";
                        break;
                }
                System.out.print("| " + piece + " |");
            }
            System.out.println();
        }
    }

    //returns list of VALID moves for ONE piece
    public ArrayList<Move> getPossibleMoves(int r, int c){
        ArrayList<Move> moves = new ArrayList<>();
        Piece p = board[r][c];

        //GET ALL POSSIBLE DIRECTIONS BASED ON TYPE OF PIECE
        //Black at bottom moves UP so -1 for row, Red + 1 for col
        int[][] directions; //basically pairs of movements
        if(p == Piece.BLACK){
            directions = new int[][]{{-1,-1},{-1, 1}}; //up-left, up-right
        }else if(p == Piece.RED){
            directions = new int[][]{{1,-1},{1, 1}}; //down-left, down-right
        }else{
            //KINGS - can move in all directions
            directions = new int[][]{{1,-1},{1, 1}, {-1,-1},{-1, 1}};
        }

        //go through all directions and add possible moves
        for(int[] d : directions){
            //NORMAL MOVE
            int row = r + d[0];
            int col = c + d[1];

            //only add if it's not out of bounds and the place is empty
            if(!outOfBounds(row, col) &&board[row][col] == Piece.EMPTY){
                Move newMove = new Move(r, c, row, col, false);
                moves.add(newMove);
            }

            //JUMPING - same logic, just translate double the amount
            int jumpRow = r + 2*d[0];
            int jumpCol = c + 2*d[1];

            //CONDITIONS TO COUNT AS JUMP:
            //      - empty cell
            //      - now out of bounds
            //      - enemy in between
            if( !outOfBounds(row, col) && !outOfBounds(jumpRow, jumpCol) && board[row][col] == Piece.EMPTY && isEnemy(p, board[row][col])){
                Move newMove = new Move(r, c, jumpRow, jumpCol, true);
                moves.add(newMove);
            }
        }
        return moves;
    }

    public boolean canJumpAgain(int row, int col){
        Piece p = board[row][col];
        int[][] directions; //basically pairs of movements
        if(p == Piece.BLACK){
            directions = new int[][]{{-1,-1},{-1, 1}}; //up-left, up-right
        }else if(p == Piece.RED){
            directions = new int[][]{{1,-1},{1, 1}}; //down-left, down-right
        }else{
            //KINGS - can move in all directions
            directions = new int[][]{{1,-1},{1, 1}, {-1,-1},{-1, 1}};
        }

        for(int[] d : directions) {
            int middleRow = row + d[0];
            int middleCol = col + d[1];
            int jumpRow = row + 2*d[0];
            int jumpCol = col + 2*d[1];

            if(!outOfBounds(jumpRow, jumpCol) && !outOfBounds(middleRow, middleCol) && board[jumpRow][jumpCol] == Piece.EMPTY && isEnemy(p, board[middleRow][middleCol])){
                return true;
            }
        }
        return false;
    }

    public ArrayList<Move> getPlayerMoves(Piece player){
        //Call this and compare to requested move and reject if not inside list
        //If there is a jump, only return jump array
        ArrayList<Move> moves = new ArrayList<>();
        ArrayList<Move> jumps = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = board[r][c];
                if (p == Piece.EMPTY) continue;
                if(!isPlayer(player, p)) continue;;

                for (Move m : getPossibleMoves(r,c)){
                    if(m.isJump){
                        jumps.add(m);
                    }else{
                        moves.add(m);
                    }
                }
            }
        }
        //IMPLEMENT FORCE JUMP!
        if(jumps.isEmpty()){
            return moves;
        }else{
            return jumps;
        }
    }

    private Piece getPieceAt(int row, int col){
        return board[row][col];
    }

    //returns true if same player should continue moving
    public void movePiece(Move m){
        Piece player = board[m.fromRow][m.fromCol];

        board[m.fromRow][m.fromCol] = Piece.EMPTY;
        board[m.toRow][m.toCol] = player;

        if(m.isJump){
            //if (2,3) to (4,5) then middle is (3, 4)
            //if (5,0) to (3, 2) then middle is (4, 1)
            //add and divide by 2
            int enemyRow = (m.fromRow + m.toRow) / 2;
            int enemyCol = (m.fromCol + m.toCol) / 2;
            board[enemyRow][enemyCol] = Piece.EMPTY;
        }

        //if it's at the end make it king, BLACK - 0, RED - 7
        if(player == Piece.RED && m.toRow == 7){
            board[m.toRow][m.toCol] = Piece.RED_KING;
        } else if(player == Piece.BLACK && m.toRow == 0){
            board[m.toRow][m.toCol] = Piece.BLACK_KING;
        }

        /* HANDLE IN GAME SESSION
        if(m.isJump && canJumpAgain(m.toRow, m.toCol)){
            return true;
        }else{
            return false;
        } */
    }

    private boolean outOfBounds(int row, int col){
        return !(row >= 0 && row < 8 && col >= 0 && col < 8);
    }

    private boolean isKing(Piece piece){
        return piece == Piece.RED_KING || piece == Piece.BLACK_KING;
    }

    private boolean isEnemy(Piece p1, Piece p2){
        if(p1 == Piece.EMPTY || p2 == Piece.EMPTY){
            return false;
        }
        //bruh gotta go thru EVERY COMBO
        //Case 1: p1 is red, p2 is black
        //Case 2: p1 is black, p2 is red
        if((p1 == Piece.RED || p1 == Piece.RED_KING) && (p2 == Piece.BLACK || p2 == Piece.BLACK_KING)){
            return true;
        }
        if((p2 == Piece.RED || p2 == Piece.RED_KING) && (p1 == Piece.BLACK || p1 == Piece.BLACK_KING)){
            return true;
        }
        return false;
    }

    private boolean isPlayer(Piece player, Piece piece){
        if(player == Piece.RED){
            return piece == Piece.RED || piece == Piece.RED_KING;
        }else{
            return piece == Piece.BLACK || piece == Piece.BLACK_KING;
        }
    }

    //GAME OVER RELATED
    public boolean hasMoves(Piece player){ return !getPlayerMoves(player).isEmpty();}

    public boolean hasPieces(Piece player){
        for(Piece[] row : board){
            for(Piece p : row){
                if(isPlayer(player, p)){
                    return true;
                }
            }
        }
        return false;
    }

    // if red played -> check if black lost and vice versa
    public boolean isGameOver(Piece currPlayer){
        Piece enemy;
        if(currPlayer == Piece.RED){
            enemy = Piece.BLACK;
        }else{
            enemy = Piece.RED;
        }
        return !hasPieces(enemy) || !hasMoves(enemy);
    }

    public Piece[][] getBoard(){
        return board;
    }
}

import java.io.Serializable;

//TODO: See if you can delete first constructor

public class Move implements Serializable {
    int fromRow, fromCol;
    int toRow, toCol;
    boolean isJump;

    private static final long serialVersionUID = 1L;

    public Move(int fr, int fc, int tr, int tc){
        fromRow = fr;
        fromCol = fc;
        toRow = tr;
        toCol = tc;
    }

    public Move(int fr, int fc, int tr, int tc, boolean isJump){
        fromRow = fr;
        fromCol = fc;
        toRow = tr;
        toCol = tc;
        this.isJump = isJump;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Move)) return false;

        Move m = (Move) obj;

        return this.fromRow == m.fromRow &&
                this.fromCol == m.fromCol &&
                this.toRow == m.toRow &&
                this.toCol == m.toCol;
    }

    public void printMove(){
        System.out.println("|(" + fromRow + ", " + fromCol + ")" + "->" + "(" + toRow + ", " + toCol + ")|");
    }

    @Override
    public int hashCode(){
        int result = Integer.hashCode(fromRow);
        result = 31 * result + Integer.hashCode(fromCol);
        result = 31 * result + Integer.hashCode(toRow);
        result = 31 * result + Integer.hashCode(toCol);
        result = 31 * result + Boolean.hashCode(isJump);
        return result;
    }
}

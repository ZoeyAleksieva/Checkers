import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.util.Pair;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import java.util.Scanner;


public class GameController implements Initializable {
    Client clientConnection;
    GuiClient main;

    @FXML
    private Parent chatView;

    @FXML
    public ChatController chatViewController; //embedded fxml

    @FXML
    private GridPane boardGrid;

    @FXML
    private Label lblTurn, lblServer, lblUser;



    private StackPane selectedCell;

    private Move selectedMove;

    public Piece playerColor;

    public String opponent;

    public Piece[][] board;

    private boolean isPlayer2;

    public void setMain(GuiClient main){
        this.main = main;
        initChatController();
    }

    public void setClientConnection(Client client){
        clientConnection = client;
        initChatController();
    }

    private void initChatController(){
        if(chatViewController != null && clientConnection != null && main != null){
            chatViewController.setClientConnection(clientConnection);
            chatViewController.setMain(main);
        }
    }

    public void startGame(Message m){
        main.setTheScene("GameScene");
        lblServer.setText("Game Start");
        HashMap<String, Piece> playerTypes = m.playerTypes;
        playerColor = playerTypes.get(clientConnection.getUsername());
        playerTypes.forEach((name, color) -> {
            if(!clientConnection.getUsername().equals(name)){
               opponent = name;
               clientConnection.setOpponent(opponent);
            }
        });
        String color;
        if(playerColor == Piece.BLACK){
            color = "White";
        }else{
            color = "Pink";
            isPlayer2 = true;
        }
        lblUser.setText("You: " + clientConnection.getUsername() +  "\nOpponent: " + clientConnection.getOpponent() + "\nPlayer Color: " + color);
        if(playerColor == Piece.BLACK){
            lblTurn.setText("Turn:" + clientConnection.getUsername());
        }else{
            lblTurn.setText("Turn:" + clientConnection.getOpponent());
        }
        updateBoardFromArray(m.board);
    }

    public void updateBoard(Message m){
        lblServer.setText(m.content);
        // null: lblServer.setText("TRYING TO SET LABEL TEXT TO: " + m.playerTurn);
        lblTurn.setText("Turn:" + m.playerTurn);
        updateBoardFromArray(m.board);
    }

    public void showError(Message m){
        lblServer.setText(m.content);
    }

    public void openGameOver(Message m){
        boolean again;
        clientConnection.setWinner(m.winner);
        //if server sends GAME_OVER with null username, it means the other player quit!!!
        if(m.username == null){
            again = false;
            if(m.winner != null){
                main.endController.showResult(m.winner, again);
            }
        }else{
            again = true;
            if(m.winner != null){
                main.endController.showResult(m.winner, again);
            }
        }
        System.out.println("Inside open game over.SETTING SCENE");
        main.setTheScene("EndScene");
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        //need to "listen" to the 64 stack panes to get clicks
        if(boardGrid == null){
            System.out.println("NULL GRID");
            return;
        }
        for(Node node : boardGrid.getChildren()){
            if(node instanceof StackPane){
                StackPane cell = (StackPane) node;
                int r = getRow(cell);
                int c = getCol(cell);
                if ((r + c) % 2 == 0) {
                    cell.setStyle("-fx-background-color: #ebcad4;");
                } else {
                    cell.setStyle("-fx-background-color: #87173a;"); //pink
                }

                cell.setOnMouseEntered(e -> {
                    cell.setStyle(cell.getStyle() + "-fx-border-color: yellow; -fx-border-width: 2;");
                });

                cell.setOnMouseExited(e -> {
                    // reset color
                    if((r + c) % 2 == 0){
                        cell.setStyle("-fx-background-color: #ebcad4;"); //#87173a
                    } else {
                        cell.setStyle("-fx-background-color: #87173a;");
                    }
                });

                cell.setOnMouseClicked(e->{handleCellClick(cell);});
            }
        }

    }

    private void handleCellClick(StackPane cell){
        int row = getRow(cell);
        int col = getCol(cell);

        int rr = fromRelativeRow(row);
        int rc = fromRelativeCol(col);


        lblServer.setText("Selected: (" + (row + 1) + ", " + (col + 1) + ")");


        //first cell selected
        if(selectedCell == null){
            selectedCell = cell;
            if(board[rr][rc] == Piece.EMPTY){
                selectedCell = null;
                return;
            }
            return;
        }

        int sr = fromRelativeRow(getRow(selectedCell));
        int sc = fromRelativeCol(getCol(selectedCell));

        if(board[rr][rc] != Piece.EMPTY){
            selectedCell = null;
            return;
        }


        Move move;
        if(Math.abs(sr - rr) > 1){
            move = new Move(sr, sc, rr, rc, true);
        }else{
            move = new Move(sr, sc, rr, rc, false);
        }

        Message msg = new Message(MsgType.MOVE, move);
        msg.username = clientConnection.getUsername();

        System.out.println("handleClick calling send() of client");
        clientConnection.send(msg);
        selectedCell = null;
    }

    private int getRow(Node n){
        Integer r = GridPane.getRowIndex(n);
        if(r == null) return 0;
        return r;
    }

    private int getCol(Node n){
        Integer c = GridPane.getColumnIndex(n);
        if(c == null) return 0;
        return c;
    }
    public void printBoard(Piece[][] board){
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
    private void updateBoardFromArray(Piece[][] board){
        this.board = board;
        printBoard(board);
        for(Node node : boardGrid.getChildren()){
            if(!(node instanceof StackPane)) continue;
            StackPane cell = (StackPane) node;
            int r = getRow(cell);
            int c = getCol(cell);

            int rr = fromRelativeRow(r);
            int rc = fromRelativeCol(c);

            cell.getChildren().clear();
            Piece p = board[rr][rc];
            if(p == Piece.EMPTY) continue;

            ImageView piece = new ImageView();
            piece.setFitWidth(35);
            piece.setFitHeight(35);
            piece.setPreserveRatio(true);

            switch (p) {
                case RED:
                    piece.setImage(new Image(getClass().getResource("/images/pink.png").toExternalForm()));
                    break;
                case BLACK:
                    piece.setImage(new Image(getClass().getResource("/images/white.png").toExternalForm()));
                    break;
                case RED_KING:
                    piece.setImage(new Image(getClass().getResource("/images/pink_king.png").toExternalForm()));
                    break;
                case BLACK_KING:
                    piece.setImage(new Image(getClass().getResource("/images/white_king.png").toExternalForm()));
                    break;
            }
            cell.getChildren().add(piece);
        }
    }

    public void quitGame(){
        Message m = new Message(MsgType.QUIT);
        m.username = clientConnection.getUsername();
        clientConnection.send(m);
        Platform.exit();
    }


    //(0,0) is (7,7) so if it's player 2 and it's flipped just do 7 - r/c
    private int fromRelativeRow(int r){
        return isPlayer2 ? 7 - r : r;
    }

    private int fromRelativeCol(int c){
        return isPlayer2 ? 7 - c : c;
    }
}

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class EndController implements Initializable {
    Client clientConnection;

    @FXML
    Stage stage;

    @FXML
    Label lblResult;

    @FXML
    private Button btnAgain;

    GuiClient main;

    String requestedName;

    public void setMain(GuiClient main){
        this.main = main;
    }

    public void setClientConnection(Client client){
        clientConnection = client;
    }

    public void playAgain(){
        lblResult.setText("Waiting for " + clientConnection.getOpponent() + " to accept.");
        Message m = new Message(MsgType.PLAY_AGAIN);
        m.username = clientConnection.getUsername();
        clientConnection.send(m);
    }

    public void quit(){
        Message m = new Message(MsgType.QUIT);
        m.username = clientConnection.getUsername();
        clientConnection.send(m);
        Platform.exit();
    }

    public void showResult(String winner, boolean again){
        if(winner == null){
            System.out.println("Null in endcontroller");
        }
        if(!again){
            btnAgain.setDisable(true);
            lblResult.setText("Your opponent quit!");
            return;
        }

        if(winner.equals(clientConnection.getUsername())){
            lblResult.setText("You Won!!! Play Again?");
        }else if(winner.equals("Draw")){
            lblResult.setText("You're tied! Rematch?");
        }else{
            lblResult.setText("You Lost...Rematch?");
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }




}

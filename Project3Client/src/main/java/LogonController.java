import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.net.URL;
import java.util.ResourceBundle;


public class LogonController implements Initializable {
    Client clientConnection;

    @FXML
    Stage stage;

    @FXML
    TextField tfUsername;

    @FXML
    Label lblError;

    @FXML
    Button btnBack, btnSubmit;

    GuiClient main;

    String requestedName;
    String password;

    boolean passwordScreen;
    boolean newPlayer;

    public void setMain(GuiClient main){
        this.main = main;
    }

    public void setClientConnection(Client client){
        clientConnection = client;
    }

    public void submitUsername(){
        //SUBMIT BUTTON ON LOGIN SCREEN SUBMITS BOTH USERNAME AND PASSWORD DEPENDING ON passwordScreen being true/false
        String content = tfUsername.getText();
        if(passwordScreen){ //means username was approved and user is submitting password
            if(newPlayer){
                Message msg = new Message(MsgType.NEW_PLAYER, requestedName, content);
                clientConnection.send(msg);
            }else{
                Message msg = new Message(MsgType.LOGIN2, requestedName, content);
                clientConnection.send(msg);
            }
        }else{ //requesting to log in with specified username
            requestedName = tfUsername.getText();
            Message msg = new Message(MsgType.LOGIN1, requestedName);
            clientConnection.send(msg);
        }
    }

    public void handleLOGIN1(Message msg){
        if(requestedName.equals(msg.username)){
            //proceed with logging in
            lblError.setText("Hi " + requestedName + "! Please enter your password.");
            tfUsername.clear();
            tfUsername.setPromptText("Enter password...");
            passwordScreen = true;
            btnBack.setVisible(true);
        }else{
            lblError.setText(msg.username); //msg.username is NOT a username -> server says username is logged in already
            passwordScreen = false;
        }
    }
    

    public void handleLOGIN2(Message msg){
        tfUsername.clear();
        if(msg.username == null){
            //PASSWORD WAS DENIED
            lblError.setText("Incorrect password. Try again or go back to enter a different username.");
        }else{
            lblError.setText(msg.content); //server says "Waiting for opponent"
            clientConnection.setUsername(requestedName);
            btnSubmit.setVisible(false);
            tfUsername.setVisible(false);
            btnBack.setVisible(false);
        }
    }

    public void handleNEWPLAYER(Message msg){
        newPlayer = true;
        passwordScreen = true;
        lblError.setText("User not found. Create password to register or go back to enter a different username.");
        tfUsername.clear();
        tfUsername.setPromptText("Enter password...");
        btnBack.setVisible(true);
    }

    public void backToUsernameScreen(){
        passwordScreen = false;
        newPlayer = false;
        tfUsername.setPromptText("Enter username...");
        tfUsername.clear();
        lblError.setText("Welcome. Enter username to continue.");
        btnBack.setVisible(false);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        passwordScreen = false;
        newPlayer = false;
        lblError.setText("Welcome. Enter username to continue.");
        btnBack.setVisible(false);
    }
}

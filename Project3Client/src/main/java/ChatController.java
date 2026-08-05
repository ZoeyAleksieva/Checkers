import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.*;


public class ChatController  implements Initializable{
    Client clientConnection;
    GuiClient main;

    @FXML
    ListView<String> lvChat;

    @FXML
    TextField tfMessage;

    @FXML
    Button btnSend;

    public void setMain(GuiClient main){
        this.main = main;
    }

    public void setClientConnection(Client client){
        clientConnection = client;
    }

    public void receiveMessage(Message message){
        lvChat.getItems().add(message.sender + ": " + message.message);
    }

    public void sendMessage(){
        if(tfMessage.getText().isEmpty()){return;}
        Message m = new Message(MsgType.CLIENT_CHAT, clientConnection.getUsername(),clientConnection.getOpponent(), tfMessage.getText());
        clientConnection.send(m);
        tfMessage.clear();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }
}

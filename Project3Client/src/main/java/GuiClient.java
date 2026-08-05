
import java.util.HashMap;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiClient extends Application{

	Stage stage;
	HashMap<String, Scene> sceneMap;

	VBox clientBox;
	Client clientConnection;
	LogonController logonController;
	GameController gameController;
	public EndController endController;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		stage = primaryStage;
		stage.setResizable(false);
		clientConnection = new Client(data->{
				Platform.runLater(()->{updateGUI((Message) data);
			});
		});
							
		clientConnection.start();
		sceneMap = new HashMap<String, Scene>();

		//load with fxml
		FXMLLoader welcomeLoader = new FXMLLoader(getClass().getResource("/logon.fxml"));
		Parent root = welcomeLoader.load();
		logonController = welcomeLoader.getController();
		logonController.setClientConnection(clientConnection);
		logonController.setMain(this);
		Scene welcomeScene = new Scene(root, 600, 600);
		sceneMap.put("LogonScene", welcomeScene) ;
		welcomeScene.getStylesheets().add("/logonStyle.css");

		FXMLLoader gameLoader = new FXMLLoader(getClass().getResource("/checkersGame.fxml"));
		Parent root3 = gameLoader.load();
		gameController = gameLoader.getController();
		gameController.setClientConnection(clientConnection);
		gameController.setMain(this);
		Scene gameScene = new Scene(root3, 600, 600);
		sceneMap.put("GameScene", gameScene);
		gameScene.getStylesheets().add("/gameStyle.css");

		FXMLLoader endLoader = new FXMLLoader(getClass().getResource("/endscreen.fxml"));
		Parent root4 = endLoader.load();
		endController = endLoader.getController();
		endController.setClientConnection(clientConnection);
		endController.setMain(this);
		Scene endScene = new Scene(root4, 600, 600);
		sceneMap.put("EndScene", endScene);
		endScene.getStylesheets().add("/logonStyle.css");

		stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                Platform.exit();
                System.exit(0);
            }
        });
		stage.setScene(sceneMap.get("LogonScene"));
		stage.setTitle("Checkers Game");
		stage.show();
	}

	public void setTheScene(String scene) {
		stage.setResizable(true);
		stage.setScene(sceneMap.get(scene));
		stage.sizeToScene();
		stage.setResizable(false);
	}

	//DISPLAY ALL MESSAGES JUST FILTER SO YOU DON'T SEE
	public void updateGUI(Message message) {
		switch (message.type){
			case LOGIN1:
				logonController.handleLOGIN1(message);
				break;
			case LOGIN2:
				logonController.handleLOGIN2(message);
				break;
			case NEW_PLAYER:
				logonController.handleNEWPLAYER(message);
				break;
			case GAME_START:
				gameController.startGame(message);
				break;
			case MOVE_RESULT:
				gameController.updateBoard(message);
				break;
			case MOVE_FEEDBACK:
				gameController.showError(message);
				break;
			case GAME_OVER:
            case DRAW:
				System.out.println("Game Over or Draw");
                gameController.openGameOver(message);
				break;
			case CLIENT_CHAT:
				gameController.chatViewController.receiveMessage(message);
				break;
            default:
				System.out.println("[ERROR] DID NOT UNDERSTAND MESSAGE TYPE");
				break;
		}
	}
}

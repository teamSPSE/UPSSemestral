import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Random;


/**
 * hlavni trida aplikace
 */
public class MainWindow {
	private Connection conn;
	private Stage primaryStage = null;
	public Client client = null;
	public Boolean play_processed = false;
	public boolean connected = false;

	//login
	public Label nameOfGame = new Label(Constants.gameTitle);
	public Label loginLabel = new Label(Constants.loginName);
	public Button login = new Button(Constants.buttonLogin);
	public TextField enterName = new TextField();


	//lobby
	public Label nameOfPlayer = new Label();
	public Label name = new Label("Your name: ");
	public Button play = new Button("Play");

	//images
	private static Image arena_background = null;

	//game
	public int enemyHealth = 100;
	public static Random r = new Random();

	public MainWindow(Stage stage, List<String> args){
		this.conn = new Connection(args, this);
		connected = this.conn.connect(this.conn.addr, this.conn.port);
		this.primaryStage = createLoginStage(stage);
		this.client = new Client("");
	}

	/**
	 * zobrazi okno
	 */
	public void show() {
		if(connected)
			this.primaryStage.show();
	}

	/**
	 * vytvori stage pro prihlaseni
	 * @param stage	aktualni stage
	 * @return	login stage
	 */
	public Stage createLoginStage(Stage stage) {
		stage = onCloseEvent(stage);
		nameOfGame.setFont(new Font(20));

		login.setMaxWidth(75);
		enterName.setOnKeyPressed(new EventHandler<KeyEvent>()
		{
			@Override
			public void handle(KeyEvent ke)
			{
				if (ke.getCode().equals(KeyCode.ENTER))
				{
					conn.login(enterName.getText());
				}
			}
		});
		login.setOnAction(event -> {
			conn.login(enterName.getText());
		});

		GridPane loginPane = new GridPane();
		loginPane.setHgap(5);
		loginPane.setVgap(5);
		loginPane.add(nameOfGame, 6, 1, 1, 1);
		loginPane.add(loginLabel, 5, 6, 1, 1);
		loginPane.add(enterName, 6, 6, 1, 1);
		loginPane.add(login, 6, 7, 1, 1);

		BorderPane root = new BorderPane();
		root.setCenter(loginPane);

		Scene scene = new Scene(root, Constants.stageWidthLogin, Constants.stageHeightLogin);

		stage.setMinWidth(Constants.stageWidthLogin);
		stage.setMinHeight(Constants.stageHeightLogin);
		stage.setScene(scene);
		stage.setTitle(Constants.gameTitle);
		return stage;
	}

	/**
	 * vyhodnoti odpoved serveru na pozadavek loginu
	 * @param msg	odpoved servetu na login
	 */
	public void processLogin(String msg) {
		String[] p = msg.split("-");
		Alert alert = null;

		if (p[1].equals("ack")) {		//ok
			primaryStage = createLobbyStage(primaryStage);
			alert = new Alert(Alert.AlertType.INFORMATION);
			alert.setHeaderText("Login was successful");
			alert.setContentText("Login was successful.");
			alert.setResizable(true);
			alert.show();
			return;
		}else if(p[1].equals("nackfull")){	//plno hracu
			alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Login failed");
			alert.setContentText("List of users is full.");
			alert.show();
		}else if(p[1].equals("nackname")){	//zabrane jmeno
			alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Login failed");
			alert.setContentText("List of users is full.");
			alert.show();
		}else{								//jina chyba
			alert = new Alert(Alert.AlertType.ERROR);
			alert.setHeaderText("Login failed");
			alert.setContentText("Something went wrong");
			alert.show();
		}
	}

	/**
	 * vytvori stage pro pridani do lobby
	 * @param stage	aktualni stage
	 * @return	lobby stage
	 */
	public Stage createLobbyStage(Stage stage) {
		stage = onCloseEvent(stage);
		play.setMaxWidth(75);
		nameOfGame.setFont(new Font(20));
		nameOfPlayer.setFont(new Font(20));
		nameOfPlayer.setText(client.getUserName());
		nameOfPlayer.setTextFill(javafx.scene.paint.Color.web("#4c02f9"));

		//uzivatel klikl na play
		if (play_processed) {
			play.setDisable(true);
			play.setText("Queued");
		}
		else {
			play.setDisable(false);
			play.setText("Play");
			play.setOnAction(event -> {
				play();
			});
		}

		GridPane lobbyPane = new GridPane();
		lobbyPane.setHgap(5);
		lobbyPane.setVgap(5);

		lobbyPane.add(nameOfGame, 6, 1, 1, 1);
		lobbyPane.add(name, 5, 6, 1, 1);
		lobbyPane.add(nameOfPlayer, 6, 6, 1, 1);
		lobbyPane.add(play, 6, 7, 1, 1);

		BorderPane root = new BorderPane();
		root.setCenter(lobbyPane);

		Scene scene = new Scene(root, Constants.stageWidthLobby, Constants.stageHeightLobby);
		stage.setScene(scene);
		stage.setTitle(Constants.gameTitle);

		return stage;
	}

	/**
	 * nastavi potrebne atributy a zavola funkci joinLobby, ktera odesle pozadavek na pridani hrace do lobby
	 */
	public void play() {
		conn.joinLobby();
		this.play_processed = true;
		client.setState(States.IN_QUEUE);
		play.setDisable(true);
		play.setText("Queued");
	}

	/**
	 * instrukce, ktere se maji provest po zavreni aplikace
	 * @param stage	 aktialni stage
	 * @return	zavrena stage
	 */
	public Stage onCloseEvent(Stage stage) {
		stage.setOnCloseRequest(event -> {
			Alert alert = new Alert(Alert.AlertType.WARNING);
			alert.setTitle(null);
			alert.setHeaderText(null);
			alert.setContentText("Application will be closed");
			alert.getButtonTypes().add(ButtonType.CANCEL);
			alert.showAndWait();
			if(alert.getResult() == ButtonType.OK) {
				if(client.getState() != States.LOGGING){
					System.out.println("odhlásit");
					conn.logout();
				}else {
					System.out.println("zavřív app");
					Platform.exit();
					System.exit(0);
				}
			}
			else {
				event.consume();
			}
			return;
		});
		return stage;
	}

	/**
	 * spravuje hru
	 * @param msg zprava ze serveru
	 */
	public void processGame(String msg) {
		String[] p = msg.split("-");
		Alert alert = new Alert(Alert.AlertType.ERROR);

		if (p[1].equals("started")){							//hra zacala
			if(p[2].equals("1")){									//zacina hrac
				client.setState(States.YOU_PLAYING);
			}else if(p[2].equals("0")){								//zacina oponent
				client.setState(States.OPPONENT_PLAYING);
			}
			primaryStage = createArena(primaryStage);				//vytovri stage areny
			conn.gameStartedResponse();								//odesle odpoved, ze byla hra zapnuta
			return;
		}else if(p[1].equals("update")){							//hra se vyvinula, jeden z hracu odehral svuj tah
			client.sethealth(Integer.parseInt(p[2]));
			enemyHealth = Integer.parseInt(p[3]);

			if (client.getState() == States.YOU_PLAYING) {
				client.setState(States.OPPONENT_PLAYING);
			}else{
				client.setState(States.YOU_PLAYING);
			}

			primaryStage = createArena(primaryStage);
			return;
		}else if(p[1].equals("finish")){								// hra skoncila
			Alert finish = new Alert(Alert.AlertType.CONFIRMATION);
			System.out.println("Game finished!");
			finish.setHeaderText("Game finished!");
			if(p[2].equals("1"))		{								//vyherce
				System.out.println("You are the WINNER!!!\nDo you want to play another match?");
				finish.setContentText("You are the WINNER!!!\nDo you want to play another match?");
			}else if(p[2].equals("0"))	{								//porazeny{
				finish.setContentText("You have lost :(\nDo you want to play another match?");
				System.out.println("You have lost :(\nDo you want to play another match?");
			}else{														//chyba
				finish.setContentText("Something went wrong! We have no winner!\nDo you want to play another match?");
				System.out.println("Something went wrong! We have no winner!");
		}
		//rozhodnoti uzivatele, zda chce hrat dal
			Optional<ButtonType> result = finish.showAndWait();
			if (result.get() == ButtonType.OK){
				play_processed = false;
				client.setState(States.LOGGED);
				client.sethealth(100);
				enemyHealth = 100;
				primaryStage = createLobbyStage(primaryStage);
			} else {
				System.out.println("player cancel:"+client.getUserName());
				conn.logout();
			}

			return;
		} else if(p[1].equals("userdsc")){							//opponent se odpojil
			alert.setHeaderText("Opponent is disconnected!");
			alert.setContentText("We could not send your dmg because opponenct is disconnected. Try again in a moment.");
			alert.show();
		} else if(p[1].equals("reconnected")){						//pripojeni uzivatele zpet do hry
			client.sethealth(Integer.parseInt(p[2]));
			enemyHealth = Integer.parseInt(p[3]);
			if(Integer.parseInt(p[4])==1){
				client.setState(States.YOU_PLAYING);
			}else{
				client.setState(States.OPPONENT_PLAYING);
			}
			primaryStage = createArena(primaryStage);
			conn.gameReconResponse();
		} else {													//prisela spatna zprava ze serveru
			alert.setHeaderText("Game failed!");
			alert.setContentText("Something went wrong");
			alert.show();
		}
	}

	/**
	 * nacte obrazky
	 */
	public static void load_images(){
		if(arena_background == null){
			try {
				FileInputStream stream = new FileInputStream("img/arena_background.png");
				arena_background = new Image(stream);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * vytvori stage areny
	 * @param stage aktualni stage
	 * @return	stage areny
	 */
	private Stage createArena(Stage stage) {
		stage = onCloseEvent(stage);
		String currentPlayer = "";

		load_images();

		GridPane info = new GridPane();
		info.setHgap(60);
		info.setVgap(20);


		Label playerHealth = new Label("Your HP: " + client.health);
		Label oponentHealth = new Label("Enemy HP: " + enemyHealth);
		Button instakill = new Button("instakill");						//instakill pro testovani
		Button fastAttack = new Button("fast attack");
		Button normalAttack = new Button("normall attack");
		Button hardAttack = new Button("hard attack");

		if (client.getState() == States.YOU_PLAYING) {
			currentPlayer = Constants.playerYou;
			instakill.setDisable(false);
			fastAttack.setDisable(false);
			normalAttack.setDisable(false);
			hardAttack.setDisable(false);
		} else {
			currentPlayer = Constants.playerOpponent;
			instakill.setDisable(true);
			fastAttack.setDisable(true);
			normalAttack.setDisable(true);
			hardAttack.setDisable(true);
		}

		Label nowPlaying = new Label("Now playing: " + currentPlayer);

		info.add(nowPlaying, 1, 2);
		info.add(playerHealth, 1, 5);
		info.add(oponentHealth, 1, 6);
		info.add(fastAttack, 1, 8);
		info.add(normalAttack, 1, 9);
		info.add(hardAttack, 1, 10);
		info.add(instakill, 1, 11);

		fastAttack.setOnAction(event -> {
			countAttack(1);
		});
		normalAttack.setOnAction(event -> {
			countAttack(2);
		});
		hardAttack.setOnAction(event -> {
			countAttack(3);
		});
		instakill.setOnAction(event -> {
			countAttack(4);
		});

		ImageView ImageView_arena_background = new ImageView(arena_background);

		BorderPane root = new BorderPane();
		root.setCenter(info);
		root.setLeft(ImageView_arena_background);

		Scene scene = new Scene(root, Constants.stageWidthArena, Constants.stageHeightArena);
		stage.setScene(scene);
		stage.setTitle(Constants.gameTitle);

		return stage;
	}

	/**
	 * spocita velikost poskozeni
	 * @param i id utoku
	 */
	public void countAttack(int i) {
		/*int dmg = 0;
		int chance = r.nextInt(10);
		switch (i){
			case 1: dmg = (chance <= 7 ? 10 : 0);break;
			case 2: dmg = (chance <= 5 ? 30 : 0);break;
			case 3: dmg = (chance <= 2 ? 50 : 0);break;
			case 4: dmg = 100;break;		//testovaci ucel
			default: dmg=0;
		}*/
		conn.sendDMG(i);
	}
}

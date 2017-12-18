package com.BlightStudios.SliceItServer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;

public class WorkerRunnable implements Runnable {

	private Socket clientSocket = null;
	private BufferedReader reader;
	private PrintWriter writer;
	private String rawRequest = null;
	private boolean isHost;

	private static Gson gson = new Gson(); // Love you <3

	public String connectedUser;
	public Game currentGame;

	public WorkerRunnable(Socket clientSocket) {
		this.clientSocket = clientSocket;
		try {
			// Setup all the in/out streams
			InputStream input = clientSocket.getInputStream();
			OutputStream output = clientSocket.getOutputStream();
			reader = new BufferedReader(new InputStreamReader(input));
			writer = new PrintWriter(output, true);
			clientSocket.setKeepAlive(true);
		} catch (IOException e) {
			System.out.println("Fatal error while connecting to client");
		}
	}

	public void run() {
		try {
			while ((rawRequest = reader.readLine()) != null) {
				System.out.println(rawRequest);
				executeRequest();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				MultiThreadedServer.workerRunnables.remove(this);
				clientSocket.close();
				System.out.println(clientSocket.getInetAddress() + " disconnected");
				User user = DatabaseManager.getUserByName(connectedUser);
				if (MultiThreadedServer.QuickMatchQueue.containsKey(user.Name)) {
					MultiThreadedServer.QuickMatchQueue.remove(user.Name);
				}
			} catch (Exception e) {
			}
		}
	}

	private void executeRequest() {
		MpRequest.Action actionTag = getActionTag(rawRequest);
		if (containsToken(rawRequest)) {

			// Check if token is valid
			if (!isTokenValid()) {
				errorResponse("InvalidToken");
				return;
			}

			switch (actionTag) {
			case Logout:
				logout();
				break;
			case QuickMatch:
				quickMatch();
				break;
			case CancelQuickMatch:
				cancelQuickMatch();
				break;
			case CreatePrivateGame:
				createPrivateGame();
				break;
			case JoinPrivateGame:
				joinPrivateGame();
				break;
			case GetAvaibleGames:
				getAvaibleGames();
				break;
			case FinishMove:
				finishMove();
				break;
			default:
				System.err.println("Unknown ActionTag: " + actionTag.toString());
				break;
			}
		} else {
			switch (actionTag) {
			case Register:
				register();
				break;
			case Login:
				login();
				break;
			default:
				System.err.println("Unknown ActionTag: " + actionTag.toString());
				break;
			}
		}
	}

	private boolean containsToken(String jsonRequest) {
		if (jsonRequest.contains("ClientToken")) {
			return true;
		}
		return false;
	}

	private MpRequest.Action getActionTag(String jsonRequest) {
		MpRequest.BaseRequest baseRequest = gson.fromJson(jsonRequest, MpRequest.BaseRequest.class);
		return MpRequest.Action.valueOf(baseRequest.ActionType);
	}

	private void register() {
		MpRequest.Register request = gson.fromJson(rawRequest, MpRequest.Register.class);

		// Is the name already taken?
		if (DatabaseManager.isUserRegisteredByName(request.ClientName)) {
			errorResponse("UsernameAlreadyTaken");
			return;
		}

		// Generate new User
		User user = new User();
		user.Name = request.ClientName;
		user.PasswordHash = SHA256(request.ClientPassword);
		generateToken(user);

		DatabaseManager.createUser(user); // Insert new User in database

		connectedUser = request.ClientName;

		// Respond with new Token
		MpResponse.Token tokenResponse = new MpResponse.Token();
		tokenResponse.ClientToken = user.Token;
		writer.println(gson.toJson(tokenResponse));
	}

	private void login() {
		MpRequest.Login request = gson.fromJson(rawRequest, MpRequest.Login.class);

		// Is the login data correct?
		if (!DatabaseManager.isCredentialValid(request.ClientName, request.ClientPassword)) {
			errorResponse("InvalidLogin");
			return;
		}

		User user = DatabaseManager.getUserByName(request.ClientName);
		generateToken(user);

		DatabaseManager.updateUser(user); // Insert new User in database

		connectedUser = request.ClientName;

		// Respond with new Token
		MpResponse.Token tokenResponse = new MpResponse.Token();
		tokenResponse.ClientToken = user.Token;
		writer.println(gson.toJson(tokenResponse));
	}

	private void logout() {
		MpRequest.Logout request = gson.fromJson(rawRequest, MpRequest.Logout.class);

		User user = DatabaseManager.getUserByToken(request.ClientToken);
		user.TokenExpire = 0;

		DatabaseManager.updateUser(user); // Insert new User in database

		errorResponse("");
	}

	private void quickMatch() {
		MpRequest.QuickMatch request = gson.fromJson(rawRequest, MpRequest.QuickMatch.class);

		User user = DatabaseManager.getUserByToken(request.ClientToken);

		if (MultiThreadedServer.QuickMatchQueue.containsKey(user.Name)) {
			errorResponse("AlreadyInQueue");
			return;
		}

		if (MultiThreadedServer.QuickMatchQueue.size() >= 1) {
			Player host = new Player();
			host.name = MultiThreadedServer.QuickMatchQueue.keySet().iterator().next();

			Player client = new Player();
			client.name = user.Name;
			client.workerRunnable = this;

			Game game = new Game();
			game.host = host;
			game.client = client;

			// Very ugly but the only solution
			for (WorkerRunnable workerRunnable : MultiThreadedServer.workerRunnables) {
				if (workerRunnable.connectedUser.equals(host.name)) {
					workerRunnable.currentGame = game;
					host.workerRunnable = workerRunnable;
					host.workerRunnable.isHost = true;
					workerRunnable.joinedMatch(client.name);
				}
			}

			currentGame = game;
			isHost = false;
			MultiThreadedServer.Games.add(game);

			// Remove other player from queue
			MultiThreadedServer.QuickMatchQueue.keySet().removeIf((String u) -> {
				return u == host.name;
			});

			joinedMatch(host.name);
		} else {
			long expireTimestamp = System.currentTimeMillis() + 60 * 10 * 1000;
			MultiThreadedServer.QuickMatchQueue.put(user.Name, expireTimestamp);
		}
	}

	private void cancelQuickMatch() {
		MpRequest.CancelQuickMatch request = gson.fromJson(rawRequest, MpRequest.CancelQuickMatch.class);

		User user = DatabaseManager.getUserByToken(request.ClientToken);

		if (MultiThreadedServer.QuickMatchQueue.containsKey(user.Name)) {
			MultiThreadedServer.QuickMatchQueue.remove(user.Name);
		}

		errorResponse("");
	}

	private void createPrivateGame() {

	}

	private void joinPrivateGame() {

	}

	private void getAvaibleGames() {

	}

	private void finishMove() {
		if (currentGame == null) {
			errorResponse("NotInGame");
			return;
		}

		MpRequest.FinishMove request = gson.fromJson(rawRequest, MpRequest.FinishMove.class);
		Player player;
		if (isHost) {
			player = currentGame.host;
		} else {
			player = currentGame.client;
		}

		if (!currentGame.move(player, request.Mass)) {
			errorResponse("InvalidMass");
			return;
		}
	}
	
	public void opponentFinishedMove(){
		MpResponse.Move response = new MpResponse.Move();
		
		Player opponent;
		if (isHost) {
			opponent = currentGame.client;
		} else {
			opponent = currentGame.host;
		}
		response.Mass = opponent.lastMove;
		
		writer.println(gson.toJson(response));
	}

	private boolean isTokenValid() {
		MpRequest.TokenAuth request = gson.fromJson(rawRequest, MpRequest.TokenAuth.class);
		User user = DatabaseManager.getUserByToken(request.ClientToken);
		boolean isCorrectToken = request.ClientToken.equals(user.Token);
		boolean isExpired = System.currentTimeMillis() > user.TokenExpire;
		return isCorrectToken && !isExpired;
	}

	private void errorResponse(String errorLevel) {
		MpResponse.Status statusResponse = new MpResponse.Status();
		if (errorLevel == "") {
			statusResponse.Success = true;
		} else {
			statusResponse.Success = false;
		}
		statusResponse.ErrorLevel = errorLevel;
		writer.println(gson.toJson(statusResponse));
	}

	private static User generateToken(User user) {
		return generateToken(user, 60 * 60 * 24);
	}

	private static User generateToken(User user, int lifetime) {
		user.Token = generateRandomHexToken(16);
		user.TokenExpire = System.currentTimeMillis() + lifetime * 1000;
		return user;
	}

	public void joinedMatch(String opponent) {
		MpResponse.Player response = new MpResponse.Player();
		response.PlayerName = opponent;
		writer.println(gson.toJson(response));
	}

	public static String SHA256(String input) {
		input += "orV8i4A7"; // Lets put some salt into the mix
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Couldn't hash password");
			return null;
		}
	}

	public static String generateRandomHexToken(int byteLength) {
		SecureRandom secureRandom = new SecureRandom();
		byte[] token = new byte[byteLength];
		secureRandom.nextBytes(token);
		return new BigInteger(1, token).toString(16);
	}
}
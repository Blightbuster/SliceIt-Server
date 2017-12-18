package com.BlightStudios.SliceItServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.io.IOException;

public class MultiThreadedServer {

	protected static int serverPort = 1531;
	protected static ServerSocket serverSocket;
	protected static boolean isStopped = false;

	public static ArrayList<Game> Games = new ArrayList<Game>();
	public static HashMap<String, Long> QuickMatchQueue = new HashMap<String, Long>();
	public static ArrayList<WorkerRunnable> workerRunnables = new ArrayList<WorkerRunnable>();

	public static void main(String[] args) {
		DatabaseManager.main(null);
		openServerSocket();

		// Check every 5s for expired Games/QuickMatchRequests
		Timer timer = new Timer();
		timer.schedule(new ExpirationController(), 0, 5000);

		System.out.println("Server started");

		while (!isStopped) {
			try {
				Socket client = serverSocket.accept();
				WorkerRunnable workerThread = new WorkerRunnable(client);
				workerRunnables.add(workerThread);
				new Thread(workerThread).start();
				System.out.println(client.getInetAddress().toString() + " connected");
			} catch (IOException e) {
				System.err.println("Error while creating connection");
			}
		}
		System.out.println("Server Stopped.");
	}

	public static void stop() {
		isStopped = true;
		try {
			serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}

	private static void openServerSocket() {
		try {
			serverSocket = new ServerSocket(serverPort);
		} catch (IOException e) {
			throw new RuntimeException("Cannot open port " + serverPort, e);
		}
	}
}

class ExpirationController extends TimerTask {
	public void run() {
		MultiThreadedServer.Games.removeIf((Game i) -> {
			return i.expireTime < System.currentTimeMillis();
		});
		MultiThreadedServer.QuickMatchQueue.values().removeIf((Long i) -> {
			return i < System.currentTimeMillis();
		});
	}
}
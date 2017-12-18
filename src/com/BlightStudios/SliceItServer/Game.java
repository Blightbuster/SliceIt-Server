package com.BlightStudios.SliceItServer;

public class Game {
	public Player host;
	public Player client;
	public long expireTime = System.currentTimeMillis() + 60 * 60 * 1000;
	public String id;

	public boolean isGameOver() {
		if (host.points >= 5 || client.points >= 5) {
			return true;
		}
		return false;
	}

	public boolean move(Player player, float mass) {
		if (player.massLeft - mass + 5 < 0 || mass < 0) {
			return false;
		}
		player.lastMove = mass;
		player.massLeft -= mass;
		player.finishedMove = true;
		if (bothPlayersFinishedMove()) {
			host.workerRunnable.opponentFinishedMove();
			client.workerRunnable.opponentFinishedMove();
			nextRound();
		}
		return true;
	}

	public boolean bothPlayersFinishedMove() {
		return host.finishedMove && client.finishedMove;
	}

	public void nextRound() {
		host.finishedMove = false;
		client.finishedMove = false;
		if(isGameOver()){
			host.workerRunnable.currentGame = null;
			client.workerRunnable.currentGame = null;
			host = null;
			client = null;
		}
	}
}

package com.BlightStudios.SliceItServer;

import java.util.List;

public class MpResponse {
	public static abstract class BaseResponse {
	}

	public static class Status extends BaseResponse {
		public boolean Success;
		public String ErrorLevel;
	}

	public static class Token extends BaseResponse {
		public String ClientToken;
	}

	public static class AvaibleGames extends BaseResponse {
		public List<String> GameNames;
	}

	public static class Move extends BaseResponse {
		public float Mass;
	}

	public static class Player extends BaseResponse {
		public String PlayerName;
	}
}
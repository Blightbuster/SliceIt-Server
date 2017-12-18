package com.BlightStudios.SliceItServer;

public class MpRequest {
	// All Actions which are used in the requests
	public static enum Action {
		Register, Login, Logout, QuickMatch, CancelQuickMatch, CreatePrivateGame, JoinPrivateGame, GetAvaibleGames, FinishMove
	}

	// --- Classes for inheritance ---

	public static class BaseRequest {
		public String ActionType;
	}

	public static class PasswordAuth extends BaseRequest {
		public String ClientName;
		public String ClientPassword;
	}

	public static class TokenAuth extends BaseRequest {
		public String ClientToken;
	}

	// --- All Request below are send to the Server ---

	public static class Register extends PasswordAuth {
		public Register() {
			ActionType = Action.Register.toString();
		}
	}

	public static class Login extends PasswordAuth {
		public Login() {
			ActionType = Action.Login.toString();
		}
	}

	public static class Logout extends TokenAuth {
		public Logout() {
			ActionType = Action.Logout.toString();
		}
	}

	public static class QuickMatch extends TokenAuth {
		public QuickMatch() {
			ActionType = Action.QuickMatch.toString();
		}
	}

	public static class CancelQuickMatch extends TokenAuth {
		public CancelQuickMatch() {
			ActionType = Action.CancelQuickMatch.toString();
		}
	}

	public static class CreatePrivateGame extends TokenAuth {
		public String GamePassword = "";

		public CreatePrivateGame() {
			ActionType = Action.CreatePrivateGame.toString();
		}
	}

	public static class JoinPrivateGame extends TokenAuth {
		public String GameName = "";
		public String GamePassword = "";

		public JoinPrivateGame() {
			ActionType = Action.JoinPrivateGame.toString();
		}
	}

	public static class GetAvaibleGames extends TokenAuth {
		public GetAvaibleGames() {
			ActionType = Action.GetAvaibleGames.toString();
		}
	}

	public static class FinishMove extends TokenAuth {
		public float Mass = 0;

		public FinishMove() {
			ActionType = Action.FinishMove.toString();
		}
	}
}
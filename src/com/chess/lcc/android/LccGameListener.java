package com.chess.lcc.android;

import com.chess.lcc.android.interfaces.LccEventListener;
import com.chess.live.client.Game;
import com.chess.live.client.GameListener;
import com.chess.live.client.User;
import com.chess.statics.AppConstants;
import com.chess.utilities.LogMe;

import java.util.Collection;

public class LccGameListener implements GameListener {

	private static final String TAG = "LccLog-GAME";

	private LccHelper lccHelper;
	private Long latestGameId = 0L;

	public LccGameListener(LccHelper lccHelper) {
		this.lccHelper = lccHelper;
	}

	@Override
	public void onGameListReceived(Collection<? extends Game> games) {
//		LogMe.dl(TAG, "Game list received, total size = " + games.size());

		Long previousGameId = latestGameId;
		latestGameId = 0L;

		Long gameId;
		for (Game game : games) {
			gameId = game.getId();
			if (lccHelper.isObservedGame(game)) {
				lccHelper.addGameToUnobserve(game); // ignore previously subscribed observed games
				/*LogMe.dl(TAG, "unobserve game " + gameId);
				games.remove(game);*/
				//lccHelper.getClient().unobserveGame(gameId);
			}
			else if (gameId > latestGameId) {
				latestGameId = gameId;
			}
		}

//		LogMe.dl(TAG, "latestGameId=" + latestGameId);

		if (!latestGameId.equals(previousGameId) && lccHelper.getLccEventListener() != null) {
//			LogMe.dl(TAG, "onGameListReceived: game is expired");
			lccHelper.getLccEventListener().expireGame();
		}
	}

	@Override
	public void onGameArchiveReceived(User user, Collection<? extends Game> games) {
	}

	@Override
	public void onGameReset(Game game) {
//		LogMe.dl(TAG, "GAME LISTENER: onGameReset id=" + game.getId() + ", game=" + game);

		if (isActualMyGame(game)) {
			lccHelper.unObserveCurrentObservingGame();

		} else if (lccHelper.isObservedGame(game)) {

			if (lccHelper.isGameToUnobserve(game)) {
				lccHelper.unobserveGame(game.getId());
				if (lccHelper.getLccObserveEventListener() != null) {
					lccHelper.getLccObserveEventListener().expireGame();
				}
				return;
			} else {
				lccHelper.setCurrentObservedGameId(game.getId());
			}

		} else {
			return; // ignore old game
		}

		lccHelper.putGame(game);

		doResetGame(game);
		doUpdateGame(false, game);
	}

	@Override
	public void onGameUpdated(Game game) {
//		LogMe.dl(TAG, "GAME LISTENER: onGameUpdated id=" + game.getId() + ", game=" + game);

		if (!lccHelper.isConnected()) {
//			LogMe.dl(TAG, "ignore onGameUpdated before onConnectionRestored"); // remove after cometd/lcc fix
			return;
		}

		if (isActualMyGame(game)) {
			lccHelper.unObserveCurrentObservingGame();

		} else if (lccHelper.isObservedGame(game)) {

			if (lccHelper.isGameToUnobserve(game)) {
				return;
			}

		} else {
			return; // ignore old game
		}

		lccHelper.putGame(game);
		doUpdateGame(true, game);
	}

	@Override
	public void onGameOver(Game game) {
//		LogMe.dl(TAG, "GAME LISTENER: onGameOver " + game);
		lccHelper.putGame(game);

		Long gameId = game.getId();

		if (isOldGame(gameId)) {
			LogMe.dl(TAG, AppConstants.GAME_LISTENER_IGNORE_OLD_GAME_ID + gameId);
			return;
		}

        /*lccHelper.getClient().subscribeToSeekList(LiveChessClient.SeekListOrderBy.Default, 1,
                                                        lccHelper.getSeekListListener());*/

		// Long lastGameId = lccHelper.getCurrentGameId() != null ? lccHelper.getCurrentGameId() : gameId; // vm: looks redundant
		lccHelper.setLastGameId(gameId);

		lccHelper.checkAndProcessEndGame(game);
	}

	@Override
	public void onGameClockAdjusted(Game game, User player, Integer newClockValue, Integer clockAdjustment, Integer resultClock) {
//		LogMe.dl(TAG, "Game Clock adjusted: gameId=" + game.getId() + ", player=" + player.getUsername() +
//				", newClockValue=" + newClockValue + ", clockAdjustment=" + clockAdjustment);
	}

	@Override
	public void onGameComputerAnalysisRequested(Long aLong, boolean b, String s) {
	}

	private boolean isOldGame(Long gameId) {
		return gameId < latestGameId;
	}

	private boolean isActualMyGame(Game game) {
		Long gameId = game.getId();

		if (!lccHelper.isMyGame(game)) {
			return false;

		} else if (lccHelper.isUserPlayingAnotherGame(gameId)) {
//			LogMe.dl(TAG, "GAME LISTENER: abort and exit second game");
			lccHelper.getClient().abortGame(game, "abort second game");
			lccHelper.getClient().exitGame(game);
			return false;

		} else if (isOldGame(gameId)) { // TODO: check case
//			LogMe.dl(TAG, "GAME LISTENER: exit old game");
			lccHelper.getClient().exitGame(game);
			return false;

		} else {
			lccHelper.clearOwnChallenges();
			lccHelper.clearChallenges();
			lccHelper.clearSeeks();
			//lccHelper.getClient().unsubscribeFromSeekList(lccHelper.getSeekListSubscriptionId());
			lccHelper.setCurrentGameId(gameId);
			if (gameId > latestGameId) {
				latestGameId = gameId;
//				LogMe.dl(TAG, "GAME LISTENER: latestGameId=" + gameId);
			}
			return true;
		}
	}

	private void doResetGame(Game game) {
		synchronized (LccHelper.LOCK) {
			lccHelper.setCurrentGameId(game.getId());
			if (game.isGameOver()) {
				lccHelper.putGame(game);
				return;
			}
			lccHelper.processFullGame();
		}
	}

	private void doUpdateGame(boolean checkMoves, Game game) {

		if (checkMoves && (game.getMoveCount() == 1 || game.getMoveCount() - 1 > lccHelper.getLatestMoveNumber())) { // do not check moves if it was
			User moveMaker = game.getLastMoveMaker();
			String move = game.getLastMove();
			LogMe.dl(TAG, "GAME LISTENER: The move #" + game.getMoveCount() + " received by user: " + lccHelper.getUser().getUsername() +
					", game.id=" + game.getId() + ", mover=" + moveMaker.getUsername() + ", move=" + move + ", allMoves=" + game.getMoves());
			synchronized (LccHelper.LOCK) {
				lccHelper.doMoveMade(game, game.getMoveCount() - 1);
			}
		}

		if (!lccHelper.isObservedGame(game)) {
			lccHelper.checkAndProcessDrawOffer(game);
		}

		if (lccHelper.isMyGame(game)) {
			User opponent = game.getOpponentForPlayer(lccHelper.getUsername());
			User.Status opponentStatus = opponent.getStatus();
			LogMe.dl(TAG, "opponent status: " + opponent.getUsername() + " is " + opponentStatus);

			LccEventListener lccEventListener = lccHelper.getLccEventListener();
			if (lccEventListener != null) {

				boolean online;
				switch (opponentStatus) {
					case PLAYING:
					case ONLINE:
						online = true;
						break;
					case OFFLINE:
					case UNKNOWN:
					case IDLE:
					default:
						online = false;
				}

				lccEventListener.updateOpponentOnlineStatus(online);
			}
		}
	}

    /*public void onDrawRejected(Game game, User rejector) {
        final String rejectorUsername = (rejector != null ? rejector.getUsername() : null);
        LogMe.dl(TAG, "GAME LISTENER: Draw rejected at the move #" + game.getMoveCount() +
                        ", game.id=" + game.getId() + ", rejector=" + rejectorUsername + ", game=" + game);
        if (!rejectorUsername.equals(lccHelper.getUser().getUsername())) {
			lccHelper.getLccEventListener().onInform(context.getString(R.string.draw_declined),
					rejectorUsername + StaticData.SPACE + context.getString(R.string.has_declined_draw));
        }
    }*/
}

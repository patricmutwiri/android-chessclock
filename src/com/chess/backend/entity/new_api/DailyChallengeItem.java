package com.chess.backend.entity.new_api;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 24.12.12
 * Time: 10:37
 */
public class DailyChallengeItem extends BaseResponseItem<List<DailyChallengeItem.Data>>{
	/*
    "status": "success",
    "count": 1,
    "data": [
        {
            "id": 361,
            "opponent_username": "erik",
            "opponent_rating": 1201,
            "opponent_win_count": 9,
            "opponent_loss_count": 11,
            "opponent_draw_count": 2,
            "color": 3,
            "days_per_move": 3,
            "game_type_id": 1,
            "is_rated": false,
            "initial_setup_fen": null,
            "url": "view_game_seek.html?id=361"
        }
    ]
	*/


	public static class Data {
		private long id;
		private String opponent_username;
		private int opponent_rating;
		private int opponent_win_count;
		private int opponent_loss_count;
		private int opponent_draw_count;
		private int color;
		private int days_per_move;
		private int game_type_id;
		private boolean is_rated;
		private String initial_setup_fen;
		private String url;

		public long getGameId() {
			return id;
		}

		public void setGameId(long id) {
			this.id = id;
		}

		public String getOpponentUsername() {
			return opponent_username;
		}

		public void getOpponentUsername(String opponent_username) {
			this.opponent_username = opponent_username;
		}

		public int getOpponentRating() {
			return opponent_rating;
		}

		public void setOpponentRating(int opponent_rating) {
			this.opponent_rating = opponent_rating;
		}

		public int getOpponentWinCount() {
			return opponent_win_count;
		}

		public void setOpponentWinCount(int opponent_win_count) {
			this.opponent_win_count = opponent_win_count;
		}

		public int getOpponentLossCount() {
			return opponent_loss_count;
		}

		public void setOpponentLossCount(int opponent_loss_count) {
			this.opponent_loss_count = opponent_loss_count;
		}

		public int getOpponentDrawCount() {
			return opponent_draw_count;
		}

		public void setOpponentDrawCount(int opponent_draw_count) {
			this.opponent_draw_count = opponent_draw_count;
		}

		public int getColor() {
			return color;
		}

		public void setColor(int color) {
			this.color = color;
		}

		public int getDaysPerMove() {
			return days_per_move;
		}

		public void setDays_per_move(int days_per_move) {
			this.days_per_move = days_per_move;
		}

		public int getGameType() {
			return game_type_id;
		}

		public void setGame_type(int game_type) {
			this.game_type_id = game_type;
		}

		public boolean isRated() {
			return is_rated;
		}

		public void setRated(boolean is_rated) {
			this.is_rated = is_rated;
		}

		public String getInitial_setup_fen() {
			return initial_setup_fen;
		}

		public void setInitial_setup_fen(String initial_setup_fen) {
			this.initial_setup_fen = initial_setup_fen;
		}

		public String getUrl() {
			return url;
		}
	}
}

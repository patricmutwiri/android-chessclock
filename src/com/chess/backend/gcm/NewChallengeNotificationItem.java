package com.chess.backend.gcm;

import com.chess.backend.statics.StaticData;

/**
 * Created with IntelliJ IDEA.
 * User: roger sent2roger@gmail.com
 * Date: 12.08.13
 * Time: 5:57
 */
public class NewChallengeNotificationItem {
/*
	'sender' => $params['fromUsername'],
	'challenge_id' => $params['challengeId'],
*/
	private String username;
	private long challengeId;
	private String avatar;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public long getChallengeId() {
		return challengeId;
	}

	public void setChallengeId(long challengeId) {
		this.challengeId = challengeId;
	}

	public String getAvatar() {
		return avatar == null ? StaticData.SYMBOL_EMPTY : avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}
}
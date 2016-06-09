package com.aionemu.gameserver.model.event;

/**
 * Created on 28.05.2016
 * 
 * @author Estrayl
 * @since AION 4.8
 */
public class ArcadeProgress {

	private final int playerObjId;
	private int frenzyPoints;
	private int currentLevel = 1;
	private boolean isFrenzyActive;
	private boolean isResumeAllowed = true;

	public ArcadeProgress(int playerObjId) {
		this.playerObjId = playerObjId;
	}

	public int getPlayerObjId() {
		return playerObjId;
	}

	public int getFrenzyPoints() {
		return frenzyPoints;
	}

	public void setFrenzyPoints(int frenzyPoints) {
		this.frenzyPoints = frenzyPoints;
	}

	public int getCurrentLevel() {
		return currentLevel;
	}

	/**
	 * @return currentLevel
	 *         - the new value
	 */
	public int setCurrentLevel(int currentLevel) {
		return this.currentLevel = currentLevel;
	}

	public boolean isFrenzyActive() {
		return isFrenzyActive;
	}

	public void setFrenzyActive(boolean isFrenzyActive) {
		this.isFrenzyActive = isFrenzyActive;
	}

	public boolean isResumeAllowed() {
		return currentLevel == 7 && isResumeAllowed;
	}

	public void setResumeAllowed(boolean isResumeAllowed) {
		this.isResumeAllowed = isResumeAllowed;
	}
}
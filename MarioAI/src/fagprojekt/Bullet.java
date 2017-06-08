package fagprojekt;

import fagprojekt.AStarAgent.State;

public class Bullet extends Enemy {

	public Bullet(float x, float y, byte kind, float ya, int facing, boolean dead) {
		super(x, y, kind, ya, facing, dead);
		this.height = 12;
		noFireballDeath = false;
	}

	@Override
	public void move(byte[][] map) {
		float sideWaysSpeed = 4f;

		xa = facing * sideWaysSpeed;
		move(xa, 0);
	}
	private boolean move(float xa, float ya) {
		x += xa;
		return true;
	}

}

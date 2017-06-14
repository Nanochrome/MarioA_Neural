package fagprojekt;

import java.util.ArrayList;

import ch.idsia.benchmark.mario.engine.GlobalOptions;

import fagprojekt.AStarAgent.State;
import fagprojekt.Enemy;
import ch.idsia.benchmark.mario.engine.sprites.Mario;

public class CustomEngine {

	// Gravity and friction
	public final float marioGravity = 1.0f;
	public final float GROUND_INERTIA = 0.89f;
	public final float AIR_INERTIA = 0.89f;
	// Mario dimensions
	public final int marioWidth = 4;

	// General dimensions
	public final int cellSize = 16;
	// Map
	private byte[][] map = new byte[19][600];
	private int mapX = 0;
	private float highestX = 0;
	// UTILITIES
	public boolean debug = true;

	private byte[][] levelScene;
	private int currY;

	/**
	 * <h1>Copy enemies</h1> Takes a list of enemies and copies it, returning an
	 * identical list
	 * 
	 * @param enemies
	 * @return ArrayList<Enemy> copy
	 */

	public ArrayList<Enemy> copyEnemies(ArrayList<Enemy> enemies) {
		ArrayList<Enemy> copy = new ArrayList<>();
		for (Enemy e : enemies) {
			if (e.kind == 98) {
				copy.add(new BlueGoomba(e.x, e.y, e.kind, e.ya, e.facing, e.dead));
			} else if (e.kind == 84) {
				copy.add(new Bullet(e.x, e.y, e.kind, e.ya, e.facing, e.dead));
			} else if (e.kind == 91) {
				copy.add(new Flower(e.x, e.y, e.kind, e.ya, e.facing, e.dead));
			} else if (e.kind == 13) {
				copy.add(new Shell(e.x, e.y, e.kind, 0, 0, e.carried, e.dead));
			} else if (e.kind == 82) {
				copy.add(new NormalEnemy(e.x, e.y, e.kind, e.ya, e.facing, e.dead, e.onGround));
			} else {
				copy.add(new NormalEnemy(e.x, e.y, e.kind, e.ya, e.facing, e.dead));
			}
		}
		return copy;
	}

	/**
	 * <h1>Predict enemies in next state</h1> Takes a state, copies the enemies
	 * into a new list, and predict the movement of the enemies in the new list
	 * 
	 * @param state
	 * @return ArrayList<Enemy> predictedEnemies
	 */

	public ArrayList<Enemy> predictEnemies(State state) {
		ArrayList<Enemy> predictedEnemies = copyEnemies(state.enemyList);

		if (!GlobalOptions.areFrozenCreatures) {
			for (int i = 0; i < predictedEnemies.size(); i++) {
				Enemy e = predictedEnemies.get(i);
				if (e != null) {
					if (!e.dead) {
						if (e.kind == 13) {
							if (e.carried) {
								state.carried = (Shell) e;
							}
							e.move(state, map);
						} else {
							e.move(map);
						}
					}
				}
			}
		}
		return predictedEnemies;
	}

	/**
	 * The current state is sent into this method where a tick is simulated,
	 * changing the state's values as needed. The logic here closely resembles
	 * the logic in the original engine.
	 */
	public void predictFuture(State state) {
		state.fireballsOnScreen = state.fireballs.size();
		// Remove dead fireballs
		ArrayList<Fireball> fireballsToRemove = new ArrayList<Fireball>();
		for (Fireball f : state.fireballs) {
			if (f.dead)
				fireballsToRemove.add(f);
		}
		state.fireballs.removeAll(fireballsToRemove);
		// Check if fireballs are out of the screen
		float xCam = state.x - 160f;
		if (xCam < 0)
			xCam = 0;
		for (Fireball f : state.fireballs) {
			float xd = f.x - xCam;
			if (xd < -64 || xd > GlobalOptions.VISUAL_COMPONENT_WIDTH + 64 || f.y < -64
					|| f.y > GlobalOptions.VISUAL_COMPONENT_HEIGHT + 64) {
				f.dead = true;
			}
		}
		// Fireball movement
		for (Fireball f : state.fireballs) {
			f.move(state, map);
		}

		if (state.invulnerable > 0)
			state.invulnerable--;
		state.wasOnGround = state.onGround;
		float sideWaysSpeed = state.action[Mario.KEY_SPEED] ? 1.2f : 0.6f;
		if (state.xa > 2) {
			state.facing = 1;
		}
		if (state.xa < -2) {
			state.facing = -1;
		}
		if (state.action[Mario.KEY_JUMP] || (state.jumpTime < 0 && !state.onGround && !state.sliding)) {

			if (state.jumpTime < 0) {
				state.xa = state.xJumpSpeed;
				state.ya = -state.jumpTime * state.yJumpSpeed;
				state.jumpTime++;
			} else if (state.onGround && state.mayJump) {
				state.xJumpSpeed = 0;
				state.yJumpSpeed = -1.9f;
				state.jumpTime = 7;
				state.ya = state.jumpTime * state.yJumpSpeed;
				state.onGround = false;
				state.sliding = false;

			} else if (state.sliding && state.mayJump) {
				state.xJumpSpeed = -state.facing * 6.0f;
				state.yJumpSpeed = -2.0f;
				state.jumpTime = -6;
				state.xa = state.xJumpSpeed;
				state.ya = -state.jumpTime * state.yJumpSpeed;
				state.onGround = false;
				state.sliding = false;
				state.facing = -state.facing;
			} else if (state.jumpTime > 0) {
				state.xa += state.xJumpSpeed;
				state.ya = state.jumpTime * state.yJumpSpeed;
				state.jumpTime--;

			} else if (state.jumpTime == 0) {
				state.action[Mario.KEY_JUMP] = false;
			}
		} else {
			state.jumpTime = 0;

		}

		if (state.action[Mario.KEY_LEFT]) {
			if (state.facing == 1)
				state.sliding = false;
			state.xa -= sideWaysSpeed;
			if (state.jumpTime >= 0)
				state.facing = -1;
		}

		if (state.action[Mario.KEY_RIGHT]) {
			if (state.facing == -1)
				state.sliding = false;
			state.xa += sideWaysSpeed;
			if (state.jumpTime >= 0)
				state.facing = 1;
		}
		if ((!state.action[Mario.KEY_LEFT] && !state.action[Mario.KEY_RIGHT]) || state.ya < 0 || state.onGround) {
			state.sliding = false;
		}
		if (state.action[Mario.KEY_SPEED] && state.ableToShoot && Mario.fire && state.fireballs.size() < 2) {
			Fireball f = new Fireball(state.x + state.facing * 6, state.y - 20, (byte) 25, 4.0f, state.facing, false);
			f.move(state, map);
			state.fireballs.add(f);
		}

		state.ableToShoot = !state.action[Mario.KEY_SPEED];
		state.mayJump = (state.onGround || state.sliding) && !state.action[Mario.KEY_JUMP];
		if (state.sliding)
			state.ya *= 0.5f;
		state.onGround = false;
		move(state, state.xa, 0); // marioMove
		move(state, 0, state.ya); // marioMove
		// gap
		if (state.y >= 15 * cellSize + cellSize) {
			State current = state;
			current.penalty(Values.penaltyDie);
		}

		if (state.x < 0) {
			state.x = 0;
			state.xa = 0;
		}
		state.ya *= 0.85f;
		if (state.onGround) {
			state.xa *= (GROUND_INERTIA);
		} else {
			state.xa *= (AIR_INERTIA);
		}

		if (!state.onGround) {
			state.ya += 3;
		}
		// When mario is carrying a shell
		if (state.carried != null) {
			state.carried.x = state.x + state.facing * 8;
			state.carried.y = state.y - 2;
			if (!state.action[Mario.KEY_SPEED]) {
				state.carried.release(state);
				state.carried = null;
			}
		}
		// Enemy collision
		for (int i = 0; i < state.enemyList.size(); i++) {
			Enemy e = state.enemyList.get(i);
			if (!e.dead) {
				int stomp = e.collideCheck(state, this);
				if (stomp == 1) {
					state.enemyList = copyEnemies(state.enemyList);
					e = state.enemyList.get(i);

					if (e.winged) {
						e.winged = false;
						e.ya = 0;
					} else {
						e.dead = true;
						e.winged = false;
					}
					state.stomp = true;
					stomp(state, e);
				} else if (stomp == 2) { // Shell stomp
					state.enemyList = copyEnemies(state.enemyList);
					Shell s = (Shell) state.enemyList.get(i);
					if (s.facing != 0) {
						s.xa = 0;
						s.facing = 0;
					} else {
						s.facing = state.facing;
					}
					stompShell(state, s);
				} else if (stomp == 3) { // Shell kick
					state.enemyList = copyEnemies(state.enemyList);
					Shell s = (Shell) state.enemyList.get(i);
					kick(state, s);
					s.facing = state.facing;
				}

			}
		}
		// Check shell collision
		for (Shell shell : state.shellsToCheck) {
			int index = state.enemyList.indexOf(shell);
			for (int i = 0; i < state.enemyList.size(); i++) {
				Enemy e = state.enemyList.get(i);
				if (e.kind != 13 && !shell.dead) {
					if (e.shellCollideCheck(state, shell)) {
						state.enemyList = copyEnemies(state.enemyList);
						Shell s = (Shell) state.enemyList.get(index);
						e = state.enemyList.get(i);
						s.die();
						e.dead = true;
						if (state.carried == shell && !shell.dead) {
							state.carried = null;
							shell.die();
						}
					}
				}
			}
		}
		state.shellsToCheck.clear();

		// Check fireball collision
		for (Fireball fireball : state.fireballsToCheck) {
			for (int i = 0; i < state.enemyList.size(); i++) {
				Enemy e = state.enemyList.get(i);
				if (!fireball.dead) {
					int result = e.fireballCollideCheck(fireball);
					if (result == 1) {
						state.enemyList = copyEnemies(state.enemyList);
						e = state.enemyList.get(i);
						e.dead = true;
						fireball.dead = true;
					} else if (result == 2) {
						fireball.dead = true;
					} else if (result == 3) {
						state.enemyList = copyEnemies(state.enemyList);
						e = state.enemyList.get(i);
						e.xa = fireball.facing * 2;
						e.ya = -5;
						fireball.dead = true;
					}
				}
			}
		}
		state.fireballsToCheck.clear();
	}

	/**
	 * main move-method for checking collision with map
	 */
	private boolean move(State state, float xa, float ya) {

		while (xa > 8) {
			if (!move(state, 8, 0))
				return false;
			xa -= 8;
		}
		while (xa < -8) {
			if (!move(state, -8, 0))
				return false;
			xa += 8;
		}
		while (ya > 8) {
			if (!move(state, 0, 8))
				return false;
			ya -= 8;
		}
		while (ya < -8) {
			if (!move(state, 0, -8))
				return false;
			ya += 8;
		}
		boolean collide = false;
		if (ya > 0) {
			if (isBlocking(state, state.x + xa - marioWidth, state.y + ya, xa, 0))
				collide = true;
			else if (isBlocking(state, state.x + xa + marioWidth, state.y + ya, xa, 0))
				collide = true;
			else if (isBlocking(state, state.x + xa - marioWidth, state.y + ya + 1, xa, ya))
				collide = true;
			else if (isBlocking(state, state.x + xa + marioWidth, state.y + ya + 1, xa, ya))
				collide = true;
		}
		if (ya < 0) {
			if (isBlocking(state, state.x + xa, state.y + ya - state.height, xa, ya))
				collide = true;
			else if (collide || isBlocking(state, state.x + xa - marioWidth, state.y + ya - state.height, xa, ya))
				collide = true;
			else if (collide || isBlocking(state, state.x + xa + marioWidth, state.y + ya - state.height, xa, ya))
				collide = true;
		}
		if (xa > 0) {
			if (isBlocking(state, state.x + xa + marioWidth, state.y + ya - state.height, xa, ya))
				collide = true;
			if (isBlocking(state, state.x + xa + marioWidth, state.y + ya - state.height / 2, xa, ya))
				collide = true;
			if (isBlocking(state, state.x + xa + marioWidth, state.y + ya, xa, ya))
				collide = true;
		}
		if (xa < 0) {
			if (isBlocking(state, state.x + xa - marioWidth, state.y + ya - state.height, xa, ya))
				collide = true;
			if (isBlocking(state, state.x + xa - marioWidth, state.y + ya - state.height / 2, xa, ya))
				collide = true;
			if (isBlocking(state, state.x + xa - marioWidth, state.y + ya, xa, ya))
				collide = true;
		}

		if (collide) {
			if (xa < 0) {
				state.x = (int) ((state.x - marioWidth) / 16) * 16 + marioWidth;
				state.xa = 0;
			}
			if (xa > 0) {
				state.x = (int) ((state.x + marioWidth) / 16 + 1) * 16 - marioWidth - 1;
				state.xa = 0;
			}
			if (ya < 0) {

				state.y = (int) ((state.y - state.height) / 16) * 16 + state.height;
				state.jumpTime = 0;
				state.ya = 0;
			}
			if (ya > 0) {
				state.y = (int) ((state.y - 1) / 16 + 1) * 16 - 1;
				state.onGround = true;
			}
			return false;
		} else {
			state.x += xa;
			state.y += ya;
			return true;
		}
	}

	/**
	 * <h1>Is blocking</h1> Returning whether or not the movement would be
	 * blocked.
	 * 
	 * @param state
	 * @param _x
	 * @param _y
	 * @param xa
	 * @param ya
	 * @return boolean
	 */

	private boolean isBlocking(State state, final float _x, final float _y, final float xa, final float ya) {
		int x = (int) (_x / 16);
		int y = (int) (_y / 16);

		if (x == (int) (state.x / 16) && y == (int) (state.y / 16)) {
			return false;
		}
		if (state.x >= 0 && state.x < 600 * 16 && y >= 0 && y < 16) {
			byte block = map[y][x];
			boolean blocking = block < 0;
			if (ya <= 0) {
				if (block == -62) {
					return false;
				}
			}
			return blocking;
		} else {
			return false;
		}
	}

	public void stomp(State state, final Enemy enemy) {
		float targetY = enemy.y - enemy.height / 2;
		move(state, 0, targetY - state.y);
		state.xJumpSpeed = 0;
		state.yJumpSpeed = -1.9f;
		state.jumpTime = 8;
		state.ya = state.jumpTime * state.yJumpSpeed;
		state.invulnerable = 1;
		state.onGround = false;
		state.sliding = false;
		state.stomp = false;
	}

	public void stompShell(State state, final Shell shell) {
		if (state.action[Mario.KEY_SPEED] && shell.facing == 0) {
			state.carried = shell;
			shell.carried = true;
		} else {
			float targetY = shell.y - shell.height / 2;
			move(state, 0, targetY - state.y);
			state.xJumpSpeed = 0;
			state.yJumpSpeed = -1.9f;
			state.jumpTime = 8;
			state.ya = state.jumpTime * state.yJumpSpeed;
			state.onGround = false;
			state.sliding = false;
			state.invulnerable = 1;
		}

	}

	public void kick(State state, final Shell shell) {
		if (state.action[Mario.KEY_SPEED]) {
			state.carried = shell;
			shell.carried = true;

		} else {
			state.invulnerable = 1;
		}

	}

	/**
	 * <h1>Printing ongoing</h1> Printing the current grid
	 * 
	 * @param x
	 * @param y
	 */
	public void printOnGoing(float x, float y) {
		if (debug) {
			int __x = (int) x / 16;
			int __y = (int) y / 16;

			for (int i = 0; i < 19; i++) {
				for (int j = mapX - 18; j < mapX + 1; j++) {
					if (i == __y && j == __x) {
						System.out.print("M" + "\t");
					} else {
						System.out.print(map[i][j] + "\t");
					}
				}
				System.out.println();
			}
			System.out.println();
		}
	}

	public void setLevelScene(byte[][] levelScene) {
		this.levelScene = levelScene;
	}

	public void setScene(byte[][] levelScene) {
		for (int i = 7; i < 19; i++) {
			for (int j = 7; j < 19; j++) {
				this.map[i - 7][j - 7] = levelScene[i][j];
			}
		}
		mapX = 18 - 7;
		currY = 2;
	}

	public void toScene(float x, float y) {
		if (x > highestX) {
			highestX = x;
			if ((int) ((highestX) / 16) > mapX - 9) {
				mapX++;
				for (int i = 0; i < 19; i++) {
					if ((int) (y / 16) + i - 9 >= 0 && ((int) (y / 16) + i - 9) < 19) {
						map[(int) (y / 16) + i - 9][mapX] = levelScene[i][18];
					}
				}
			}
		}

		if ((int) y / 16 > currY) {
			currY = (int) y / 16;
			if ((int) y / 16 + 9 < 19) {

				for (int i = 0; i < 19; i++) {
					if ((int) (x / 16) - (9 - i) >= 0) {
						map[(int) (y / 16) + 9][i + (int) (x / 16) - 9] = levelScene[18][i];
					}
				}
			}

		} else if ((int) y / 16 < currY) {
			currY = (int) y / 16;
			if ((int) (y / 16) - 9 >= 0) {

				for (int i = 0; i < 19; i++) {
					if ((int) (x / 16) - (9 - i) >= 0) {
						map[(int) (y / 16) - 9][i + (int) (x / 16) - 9] = levelScene[0][i];

					}
				}
			}
		}
	}

	/**
	 * <h1>Set cheatlevelscene</h1> Setting the initial state of the map, adding
	 * the first couple of rows
	 * 
	 * @param levelScene
	 */
	public void setCheatLevelScene(byte[][] levelScene) {
		this.levelScene = levelScene;
	}

	public void setCheatScene(byte[][] levelScene) {
		for (int i = 0; i < 19; i++) {
			for (int j = 0; j < 19; j++) {
				this.map[i][j] = levelScene[i][j];
			}
		}
		mapX = 18;
	}

	/**
	 * <h1>To cheatscene</h1> Adding the next column to our map
	 * 
	 * @param x
	 */

	public void toCheatScene(float x) {
		if (x > highestX) {
			highestX = x;
			if ((int) ((highestX) / 16) > mapX - 15) {
				mapX++;
				for (int i = 0; i < 19; i++) {
					map[i][mapX] = levelScene[i][17];
				}
			}
		}
	}

	void print() {
		for (int i = 0; i < 19; i++) {
			for (int j = 0; j < mapX; j++) {

				System.out.print(map[i][j] + "\t");

			}
			System.out.println();
		}
		System.out.println();
	}
}

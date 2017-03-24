/*
 * Copyright (c) 2009-2010, Sergey Karakovskiy and Julian Togelius
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Mario AI nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package fagprojekt;

import fagprojekt.AStarAgent.State;

public class Enemy {
	public static final int KIND_NONE = 0;
	public static final int KIND_GOOMBA = 80;
	public static final int KIND_GOOMBA_WINGED = 95;
	public static final int KIND_RED_KOOPA = 82;
	public static final int KIND_RED_KOOPA_WINGED = 97;
	public static final int KIND_GREEN_KOOPA = 81;
	public static final int KIND_GREEN_KOOPA_WINGED = 96;
	public static final int KIND_BULLET_BILL = 84;
	public static final int KIND_SPIKY = 93;
	public static final int KIND_SPIKY_WINGED = 99;
	public static final int KIND_ENEMY_FLOWER = 91;
	public static final int KIND_WAVE_GOOMBA = 98;
	public static final int KIND_SHELL = 13;
	public static final int KIND_MUSHROOM = 2;
	public static final int KIND_GREEN_MUSHROOM = 9;
	public static final int KIND_PRINCESS = 49;
	public static final int KIND_FIRE_FLOWER = 3;
	public static final int KIND_PARTICLE = 21;
	public static final int KIND_SPARCLE = 22;
	public static final int KIND_COIN_ANIM = 1;
	public static final int KIND_FIREBALL = 25;

	public static final int KIND_UNDEF = -42;
	
	protected static float GROUND_INERTIA = 0.89f;
	protected static float AIR_INERTIA = 0.89f;
	
	
	public byte kind = KIND_UNDEF;
	public float creaturesGravity = 1;
	public float runTime;
	public boolean onGround = false;

	public int width = 4;
	public int height = 24;
	public float yaa = 1;
	public float x;
	public float y;
	public float ya;
	public float xa;

	public int facing;
	public int deadTime = 0;
	public boolean flyDeath = false;

	public boolean avoidCliffs = true;

	public boolean winged = true;
	public int wingTime = 0;

	public float yaw = 1;
	public int debugPos;
	public boolean noFireballDeath;
	public Enemy() {}
	public Enemy(int x, int y,
			int type /* ,boolean winged, int mapX, int mapY */) {
		debugPos = 0;
		kind = (byte) type;
		this.winged = winged; // What to do here?
		if(kind == KIND_GOOMBA || kind == KIND_GOOMBA_WINGED) height = 12;
		this.x = x;
		this.y = y;
		
		yaa = creaturesGravity * 2;
		yaw = creaturesGravity == 1 ? 1 : 0.3f * creaturesGravity;

		avoidCliffs = kind == KIND_RED_KOOPA;
		
		noFireballDeath = (kind == KIND_SPIKY || kind == KIND_SPIKY_WINGED);
		//facing = dir; HOW TO MAKE DIR?
		if (facing == 0) facing = -1;
	}

	public void collideCheck(CustomEngine ce, State state) {
		if (deadTime != 0) {
			return;
		}
		float xMarioD = state.x - x;
		float yMarioD = state.y - y;
		// float w = 16;
		if (xMarioD > -width * 2 - 4 && xMarioD < width * 2 + 4) {
			
			if (yMarioD > -height && yMarioD < 24) { // CHANGE 24 TO MARIO HEIGHT
				if ((kind != KIND_SPIKY && kind != KIND_SPIKY_WINGED && kind != KIND_ENEMY_FLOWER)
						&& state.ya > 0 && yMarioD <= 0 && (!state.onGround)) {
					System.out.println("STOMPED");
					ce.stomp(state, this);
					
					if (winged) {
						winged = false;
						ya = 0;
					} else {
						deadTime = 10;
						winged = false;
					}
				} else {
					//levelScene.mario.getHurt(this.kind); TODO - Mario Damage!
					state.penalty(500);
					//state.devolve (mario got worse)
				}
			}
		}
	}

	public void move(byte[][] map) {
		wingTime++;
		if (deadTime > 0) {
			deadTime--;

			if (deadTime == 0) {
				deadTime = 1;
			}

			if (flyDeath) {
				x += xa;
				y += ya;
				ya *= 0.95;
				ya += 1;
			}
			return;
		}

		float sideWaysSpeed = 1.75f;

		if (xa > 2)
			facing = 1;
		else if (xa < -2)
			facing = -1;

		xa = facing * sideWaysSpeed;
		

		

		if (!move(map, xa, 0))
			facing = -facing;
		onGround = false;
		move(map, 0, ya);

		ya *= winged ? 0.95f : 0.85f;
		if (onGround) {
			xa *= (GROUND_INERTIA);
		} else {
			xa *= (AIR_INERTIA);
		}

		if (!onGround) {
			if (winged) {
				ya += 0.6f * yaw;
			} else {
				ya += yaa;
			}
		} else if (winged) {
			ya = -10;
		}

	}

	public boolean move(byte[][] map, float xa, float ya) {

		while (xa > 8) {
			if (!move(map, 8, 0))
				return false;
			xa -= 8;
		}
		while (xa < -8) {
			if (!move(map, -8, 0))
				return false;
			xa += 8;
		}
		while (ya > 8) {
			if (!move(map, 0, 8))
				return false;
			ya -= 8;
		}
		while (ya < -8) {
			if (!move(map, 0, -8))
				return false;
			ya += 8;
		}

		boolean collide = false;
		if (ya > 0) {
			if (isBlocking(map, x + xa - width, y + ya, xa, 0))
				collide = true;
			else if (isBlocking(map, x + xa + width, y + ya, xa, 0))
				collide = true;
			else if (isBlocking(map, x + xa - width, y + ya + 1, xa, ya))
				collide = true;
			else if (isBlocking(map, x + xa + width, y + ya + 1, xa, ya))
				collide = true;
		}
		if (ya < 0) {
			if (isBlocking(map, x + xa, y + ya - height, xa, ya))
				collide = true;
			else if (collide || isBlocking(map, x + xa - width, y + ya - height, xa, ya))
				collide = true;
			else if (collide || isBlocking(map, x + xa + width, y + ya - height, xa, ya))
				collide = true;
		}
		if (xa > 0) {
			if (isBlocking(map, x + xa + width, y + ya - height, xa, ya))
				collide = true;
			if (isBlocking(map, x + xa + width, y + ya - height / 2, xa, ya))
				collide = true;
			if (isBlocking(map, x + xa + width, y + ya, xa, ya))
				collide = true;

			if (avoidCliffs && onGround
					&& !(map[(int) ((y) / 16 + 1)][(int) ((x + xa + width) / 16)] < 0))
				collide = true;
		}
		if (xa < 0) {
			if (isBlocking(map, x + xa - width, y + ya - height, xa, ya))
				collide = true;
			if (isBlocking(map, x + xa - width, y + ya - height / 2, xa, ya))
				collide = true;
			if (isBlocking(map, x + xa - width, y + ya, xa, ya))
				collide = true;

			if (avoidCliffs && onGround
					&& map[(int) ((y) / 16 + 1)][(int) ((x + xa - width) / 16)] < 0)
				collide = true;
		}

		if (collide) {
			if (xa < 0) {
				x = (int) ((x - width) / 16) * 16 + width;
				this.xa = 0;
			}
			if (xa > 0) {
				x = (int) ((x + width) / 16 + 1) * 16 - width - 1;
				this.xa = 0;
			}
			if (ya < 0) {
				y = (int) ((y - height) / 16) * 16 + height;
				this.ya = 0;
			}
			if (ya > 0) {
				y = (int) (y / 16 + 1) * 16 - 1;
				onGround = true;
			}
			return false;
		} else {
			x += xa;
			y += ya;
			return true;
		}
	}

	public boolean isBlocking(byte[][] map, final float _x, final float _y, final float xa, final float ya) {
		int x = (int) (_x / 16);
		int y = (int) (_y / 16);

		if (x == (int) (this.x / 16) && y == (int) (this.y / 16)){
			return false;
		}
		if (this.x >= 0 && this.x < 600 * 16 && y >= 0 && y < 16) {
			byte block = map[y][x];
			if (ya <= 0) {
				if (block == -62) {
					return false;
				}
			}
			return block < 0;
		} else {
			return false;
		}
		
	}
/* TODO - SHELLS
	public boolean shellCollideCheck(Shell shell) {
		if (deadTime != 0)
			return false;

		float xD = shell.x - x;
		float yD = shell.y - y;

		if (xD > -16 && xD < 16) {
			if (yD > -height && yD < shell.height) {
				xa = shell.facing * 2;
				ya = -5;
				flyDeath = true;
				if (spriteTemplate != null)
					spriteTemplate.isDead = true;
				deadTime = 100;
				winged = false;
				hPic = -hPic;
				yPicO = -yPicO + 16;
				// System.out.println("shellCollideCheck");
				++LevelSmapne.killedCreaturesTotal;
				++LevelSmapne.killedCreaturesByShell;
				return true;
			}
		}
		return false;
	}
*/
/*// TODO - FIREBALLS
	public boolean fireballCollideCheck(Fireball fireball) {
		if (deadTime != 0)
			return false;

		float xD = fireball.x - x;
		float yD = fireball.y - y;

		if (xD > -16 && xD < 16) {
			if (yD > -height && yD < fireball.height) {
				if (noFireballDeath)
					return true;

				xa = fireball.facing * 2;
				ya = -5;
				flyDeath = true;
				if (spriteTemplate != null)
					spriteTemplate.isDead = true;
				deadTime = 100;
				winged = false;
				hPic = -hPic;
				yPicO = -yPicO + 16;
				// System.out.println("fireballCollideCheck");
				++LevelScene.killedCreaturesTotal;
				++LevelScene.killedCreaturesByFireBall;
				return true;
			}
		}
		return false;
	}
	*/
/* TODO - BUMPCHECK?!
	public void bumpCheck(int xTile, int yTile) {
		if (deadTime != 0)
			return;

		if (x + width > xTile * 16 && x - width < xTile * 16 + 16 && yTile == (int) ((y - 1) / 16)) {
			xa = -levelScene.mario.facing * 2;
			ya = -5;
			flyDeath = true;
			if (spriteTemplate != null)
				spriteTemplate.isDead = true;
			deadTime = 100;
			winged = false;
			hPic = -hPic;
			yPicO = -yPicO + 16;
			// System.out.println("bumpCheck: mostelikely shell killed other
			// creature");
		}
	}
	*/
}
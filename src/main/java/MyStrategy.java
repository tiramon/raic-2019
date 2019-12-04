import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import model.ColorFloat;
import model.CustomData;
import model.Game;
import model.Item;
import model.LootBox;
import model.Tile;
import model.Unit;
import model.UnitAction;
import model.Vec2Double;
import model.Vec2Float;

public class MyStrategy {
	static double distanceSqr(Vec2Double a, Vec2Double b) {
		return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
	}

	static Vec2Double add(Vec2Double a, Vec2Double b) {
		return new Vec2Double(a.getX() + b.getX(), a.getY() + b.getY());
	}

	static Vec2Double addIntern(Vec2Double a, Vec2Double b) {
		a.setX(a.getX() + b.getX());
		a.setY(a.getY() + b.getY());
		return a;
	}

	static Vec2Double addIntern(Vec2Double a, Vec2Double b, double factor) {
		a.setX(a.getX() + (b.getX() / factor));
		a.setY(a.getY() + (b.getY() / factor));
		return a;
	}

	// walk only as close as needed
	// DONE - check LOS
	// walk most effective way (path finding, ladders, platforms, jumppads)
	// swap weapons?
	// reloading
	// retreat while reloading
	// don't hit own unit (LOS, expl.)
	// dodge bullets
	// dodge mines
	// gather health packs
	// gather mines
	// steal weapons from other player (lootbox) (only better than his current)
	// weapon params in properties, calc best dps (incl reload)
	// only charge with better weapon
	// shot with bazooka wihtout los when out of epl range

	Map<Integer, Vec2Double> lastPosition = new HashMap<>();

	public UnitAction getAction(Unit unit, Game game, Debug debug) {
		Bresenham.init(game.getLevel().getTiles().length, game.getLevel().getTiles()[0].length, game.getUnits(), game.getLevel().getTiles());
		Unit nearestEnemy = getNearestEnemy(unit, game);
		Vec2Double lastVel = new Vec2Double();
		if (nearestEnemy != null) {
			Vec2Double lastPosUnit = lastPosition.get(nearestEnemy.getId());
			if (lastPosUnit != null) {
				lastVel.setX(nearestEnemy.getPosition().getX() - lastPosUnit.getX());
				lastVel.setY(nearestEnemy.getPosition().getY() - lastPosUnit.getY());
			}
		}
		Arrays.stream(game.getUnits()).forEach(u -> lastPosition.put(u.getId(), u.getPosition()));
		LootBox nearestWeapon = getNearestWeaponLootBox(unit, game);
		LootBox nearestHealth = getNearestHealthLootBox(unit, game);

		// **************************
		// Choose target to move to
		// **************************
		Vec2Double targetPos = unit.getPosition();
		// if no current weapon, get nearestWeapon
		if (unit.getWeapon() == null && nearestWeapon != null) {

			targetPos = nearestWeapon.getPosition();
		}
		// if we have a weapon get nearest enemy
		else if (nearestEnemy != null) {
			debug.draw(new CustomData.Log("Weapon: BS - " + unit.getWeapon().getParams().getBullet().getSpeed() + " Spread " + unit.getWeapon().getParams().getMinSpread() + "/" + unit.getWeapon().getParams().getMaxSpread() + " R " + unit.getWeapon().getParams().getRecoil()));
			targetPos = nearestEnemy.getPosition();
		}

		debug.draw(new CustomData.Log("own pos: " + unit.getPosition().getX() + "/" + unit.getPosition().getY()));
		debug.draw(new CustomData.Log("Target pos: " + targetPos.getX() + "/" + targetPos.getY()));
		debug.draw(new CustomData.Log("Target vel: " + lastVel.getX() + "/" + lastVel.getY()));

		// **************************
		// Choose target to aim at
		// **************************
		Vec2Double aim = new Vec2Double(0, 0);
		// if there is an enemy aim for him

		if (nearestEnemy != null && unit.getWeapon() != null) {
			double ticks = game.getProperties().getTicksPerSecond();
			// get location of target when bullet reaches him with his current speed
			// double sqrDistance = distanceSqr(add(nearestEnemy.getPosition(), lastVel),
			// unit.getPosition());
			double bulletSpeed = unit.getWeapon().getParams().getBullet().getSpeed();
			double bulletSpeedPerTick = bulletSpeed / ticks;
			// double sqrBulletSpeed = bulletSpeed * bulletSpeed;
			double sqrTickBulletSpeed = bulletSpeedPerTick * bulletSpeedPerTick;
			double roundsTilBulletReachesEnemy;

			// Vec2Double posBullet = new Vec2Double(unit.getPosition().getX(),
			// unit.getPosition().getY());
			Vec2Double posEnemy = new Vec2Double(nearestEnemy.getPosition().getX(), nearestEnemy.getPosition().getY());

			int aimTicksAhead = 0;
			for (int i = 1; i < 120; i++) {
				// enemy moves
				addIntern(posEnemy, lastVel);
				double sqrDistance = distanceSqr(posEnemy, unit.getPosition());
				double sqrRound = sqrDistance / sqrTickBulletSpeed;
				if (sqrRound < i * i) {
					System.err.println(i);
					aimTicksAhead = i;
					break;
				}
			}

			aim = new Vec2Double(posEnemy.getX() - unit.getPosition().getX(), posEnemy.getY() - unit.getPosition().getY());

			debug.draw(new CustomData.Log("Aim: " + aim.getX() + "/" + aim.getY() + " ticks " + aimTicksAhead + "/" + ticks + " " + posEnemy.getX() + "/" + posEnemy.getY()));
			// aim = new Vec2Double(nearestEnemy.getPosition().getX() - unit.getPosition().getX(),
			// nearestEnemy.getPosition().getY() - unit.getPosition().getY());
		} else {
			aim = new Vec2Double(nearestEnemy.getPosition().getX() - unit.getPosition().getX(), nearestEnemy.getPosition().getY() - unit.getPosition().getY());
			;
		}
		// move upward?
		boolean jump = targetPos.getY() > unit.getPosition().getY();
		if (targetPos.getX() > unit.getPosition().getX() && game.getLevel().getTiles()[(int) (unit.getPosition().getX() + 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
			jump = true;
		}
		// move downward
		if (targetPos.getX() < unit.getPosition().getX() && game.getLevel().getTiles()[(int) (unit.getPosition().getX() - 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
			jump = true;
		}

		// compare current weapon with lootbox weapon
		boolean swap = false;
		// if (unit != null && unit.getWeapon() != null && nearestWeapon != null && unit.getWeapon().getTyp().discriminant < ((Weapon) nearestWeapon.getItem()).getWeaponType().discriminant) {
		// swap = true;
		// }
		boolean shoot = true;
		model.Weapon currentWeapon = unit.getWeapon();
		if (nearestEnemy != null && currentWeapon != null) {
			double distanceEnemy = distanceSqr(nearestEnemy.getPosition(), unit.getPosition());

			boolean hasLos = lineOfSight(unit.getPosition(), nearestEnemy.getPosition());
			boolean outOfExplosionRange = !isExplosionWeapon(currentWeapon) || currentWeapon.getParams().getExplosion().getRadius() > distanceEnemy;
			shoot = hasLos && outOfExplosionRange;
			if (shoot) {
				targetPos = unit.getPosition();
			}
		}

		// if (unit.getWeapon().getTyp().discriminant < ((Weapon) nearestWeapon.getItem()).getWeaponType().discriminant) {
		// swap = true;
		// }

		Vec2Double aimTarget = add(unit.getPosition(), aim);
		debug.draw(new CustomData.Line(new Vec2Float((float) unit.getPosition().getX(), (float) unit.getPosition().getY()), new Vec2Float((float) aimTarget.getX(), (float) aimTarget.getY()), 0.05f, new ColorFloat(1, 0, 0, 1)));

		UnitAction action = new UnitAction();
		action.setVelocity(targetPos.getX() - unit.getPosition().getX());
		action.setJump(jump);
		action.setJumpDown(!jump);
		action.setAim(aim);
		action.setShoot(shoot);
		action.setSwapWeapon(swap);
		action.setPlantMine(false);
		return action;
	}

	public boolean lineOfSight(Vec2Double pos1, Vec2Double pos2) {
		if (pos1.getY() < pos2.getY())
			return Bresenham.calculateLine(pos1, pos2);
		else
			return Bresenham.calculateLine(pos2, pos1);
	}

	public boolean isExplosionWeapon(model.Weapon weapon) {
		return weapon != null && weapon.getParams() != null && weapon.getParams().getExplosion() != null && weapon.getParams().getExplosion().getRadius() > 0d;
	}

	public LootBox getNearestWeaponLootBox(Unit unit, Game game) {
		LootBox nearestWeapon = null;
		for (LootBox lootBox : game.getLootBoxes()) {
			if (lootBox.getItem() instanceof Item.Weapon) {
				if (nearestWeapon == null || distanceSqr(unit.getPosition(), lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestWeapon.getPosition())) {
					nearestWeapon = lootBox;
				}
			}
		}
		return nearestWeapon;
	}

	public LootBox getNearestHealthLootBox(Unit unit, Game game) {
		LootBox nearestHealth = null;
		for (LootBox lootBox : game.getLootBoxes()) {
			if (lootBox.getItem() instanceof Item.HealthPack) {
				if (nearestHealth == null || distanceSqr(unit.getPosition(), lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestHealth.getPosition())) {
					nearestHealth = lootBox;
				}
			}
		}
		return nearestHealth;
	}

	public Unit getNearestEnemy(Unit unit, Game game) {
		Unit nearestEnemy = null;
		for (Unit other : game.getUnits()) {
			if (other.getPlayerId() != unit.getPlayerId()) {
				if (nearestEnemy == null || distanceSqr(unit.getPosition(), other.getPosition()) < distanceSqr(unit.getPosition(), nearestEnemy.getPosition())) {
					nearestEnemy = other;
				}
			}
		}

		return nearestEnemy;
	}
}

class Bresenham {
	static boolean[][] blocked;

	public static void init(int width, int height, Unit[] units, Tile[][] tiles) {
		blocked = new boolean[width][height];
		Arrays.stream(units).forEach(u -> blocked[(int) u.getPosition().getX()][(int) u.getPosition().getY()] = true);
		// cells.stream().filter(u -> u.empty == false).forEach(c -> blocked[c.x][c.getPosition().getY()] = true);
		for (int i = 0; i < tiles.length; i++) {
			for (int j = 0; j < tiles[i].length; j++) {
				blocked[i][j] = tiles[i][j].equals(Tile.WALL);
			}
		}
	}

	public static boolean calculateLine(model.Vec2Double pos1, model.Vec2Double pos2) {
		int p1x = (int) pos1.getX();
		int p1y = (int) pos1.getY();
		int p2x = (int) pos2.getX();
		int p2y = (int) pos2.getY();
		// Console.Error.WriteLine("los "+ first.x+"/"+first.y + " -> " + last.x+"/"+last.y);
		int deltaX = Math.abs(p2x - p1x);
		int deltaY = Math.abs(p2y - p1y);

		int signalX = p1x < p2x ? 1 : -1;
		int signalY = p1y < p2y ? 1 : -1;

		int error = deltaX - deltaY;

		int doubleError;
		int newPosX = p1x;
		int newPosY = p1y;

		while (true) {
			doubleError = 2 * error;

			if (doubleError > -1 * deltaY) {
				error -= deltaY;
				newPosX = newPosX + signalX;
			}

			if (doubleError < deltaX) {
				error += deltaX;
				newPosY = newPosY + signalY;
			}

			if (newPosX == p2x && newPosY == p2y) {
				return true;
			}
			if (blocked[newPosX][newPosY]) {
				return false;
			}
		}
	}
}
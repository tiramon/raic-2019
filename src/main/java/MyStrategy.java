import model.CustomData;
import model.Game;
import model.Item;
import model.Item.Weapon;
import model.LootBox;
import model.Tile;
import model.Unit;
import model.UnitAction;
import model.Vec2Double;

public class MyStrategy {
	static double distanceSqr(Vec2Double a, Vec2Double b) {
		return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
	}

	// walk only as close as needed
	// check LOS
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

	public UnitAction getAction(Unit unit, Game game, Debug debug) {

		Unit nearestEnemy = getNearestEnemy(unit, game);
		LootBox nearestWeapon = getNearestLootBox(unit, game);

		Vec2Double targetPos = unit.getPosition();
		// if no current weapon, get nearestWeapon
		if (unit.getWeapon() == null && nearestWeapon != null) {
			targetPos = nearestWeapon.getPosition();

		}
		// if we have a weapon get nearest enemy
		else if (nearestEnemy != null) {

			targetPos = nearestEnemy.getPosition();
		}

		debug.draw(new CustomData.Log("Target pos: " + targetPos));
		Vec2Double aim = new Vec2Double(0, 0);
		// if there is an enemy aim for him
		if (nearestEnemy != null) {
			aim = new Vec2Double(nearestEnemy.getPosition().getX() - unit.getPosition().getX(),
					nearestEnemy.getPosition().getY() - unit.getPosition().getY());
		}
		// move upward?
		boolean jump = targetPos.getY() > unit.getPosition().getY();
		if (targetPos.getX() > unit.getPosition().getX() && game.getLevel()
				.getTiles()[(int) (unit.getPosition().getX() + 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
			jump = true;
		}
		// move downward
		if (targetPos.getX() < unit.getPosition().getX() && game.getLevel()
				.getTiles()[(int) (unit.getPosition().getX() - 1)][(int) (unit.getPosition().getY())] == Tile.WALL) {
			jump = true;
		}

		// compare current weapon with lootbox weapon
		boolean swap = false;
		if (unit.getWeapon().getTyp().discriminant < ((Weapon) nearestWeapon.getItem()).getWeaponType().discriminant) {
			swap = true;
		}

		UnitAction action = new UnitAction();
		action.setVelocity(targetPos.getX() - unit.getPosition().getX());
		action.setJump(jump);
		action.setJumpDown(!jump);
		action.setAim(aim);
		action.setShoot(true);
		action.setSwapWeapon(swap);
		action.setPlantMine(false);
		return action;
	}

	public LootBox getNearestLootBox(Unit unit, Game game) {
		LootBox nearestWeapon = null;
		for (LootBox lootBox : game.getLootBoxes()) {
			if (lootBox.getItem() instanceof Item.Weapon) {
				if (nearestWeapon == null || distanceSqr(unit.getPosition(),
						lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestWeapon.getPosition())) {
					nearestWeapon = lootBox;
				}

			}
		}
		return nearestWeapon;
	}

	public Unit getNearestEnemy(Unit unit, Game game) {
		Unit nearestEnemy = null;
		for (Unit other : game.getUnits()) {
			if (other.getPlayerId() != unit.getPlayerId()) {
				if (nearestEnemy == null || distanceSqr(unit.getPosition(),
						other.getPosition()) < distanceSqr(unit.getPosition(), nearestEnemy.getPosition())) {
					nearestEnemy = other;
				}
			}
		}
		return nearestEnemy;
	}
}
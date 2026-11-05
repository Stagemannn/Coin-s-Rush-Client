package com.coinrush.camera.client;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CameraMathTest {
	@Test
	void wrapStaysInRange() {
		assertEquals(0, CameraMath.wrap(360), 1e-4);
		assertEquals(-170, CameraMath.wrap(190), 1e-4);
		assertEquals(170, CameraMath.wrap(-190), 1e-4);
		assertEquals(-180, CameraMath.wrap(180), 1e-4);
		assertEquals(10, CameraMath.wrap(730), 1e-4);
	}

	@Test
	void speedScale() {
		assertEquals(1f, CameraMath.fraction(100), 1e-6);
		assertEquals(1f, CameraMath.fraction(250), 1e-6);
		assertEquals(0.5f, CameraMath.fraction(50), 1e-6);
		assertEquals(0.05f, CameraMath.fraction(0), 1e-6); // jamais bloqué
	}

	@Test
	void stepTowardTakesShortestWayAndConverges() {
		assertEquals(370, CameraMath.stepToward(350, 10, 100), 1e-3); // 20° par la droite, pas 340° par la gauche
		float y = 350;
		for (int i = 0; i < 40; i++) y = CameraMath.stepToward(y, 10, 50);
		assertEquals(0, CameraMath.wrap(y - 10), 0.06);
		assertEquals(90, CameraMath.stepToward(0, 90, 100), 1e-4); // instantané
		assertEquals(45, CameraMath.stepToward(0, 90, 50), 1e-4);
		assertEquals(-80, CameraMath.stepToward(10, 190, 50), 1e-3); // exactement à l'opposé : sens défini, sans exception
	}

	@Test
	void stepPitchConverges() {
		float p = 80;
		for (int i = 0; i < 60; i++) p = CameraMath.stepPitch(p, -30, 65);
		assertEquals(-30, p, 1e-3);
		assertEquals(0, CameraMath.stepPitch(10, 0, 100), 1e-6);
	}

	@Test
	void moveYawFollowsKeysRelativeToCamera() {
		assertEquals(30, CameraMath.moveYaw(30, 0, 1), 1e-3); // avant
		assertEquals(120, CameraMath.moveYaw(30, -1, 0), 1e-3); // droite
		assertEquals(-60, CameraMath.moveYaw(30, 1, 0), 1e-3); // gauche
		assertEquals(-150, CameraMath.wrap(CameraMath.moveYaw(30, 0, -1)), 1e-3); // arrière
		assertEquals(75, CameraMath.moveYaw(30, -1, 1), 1e-3); // avant + droite
	}

	@Test
	void idleFollowsOnlyBeyondTheLimit() {
		assertEquals(10, CameraMath.idleTarget(10, 40, 45), 1e-4); // dans l'angle : immobile
		assertEquals(10, CameraMath.idleTarget(10, -30, 45), 1e-4);
		assertEquals(55, CameraMath.idleTarget(10, 100, 45), 1e-4); // plus loin : suit en gardant 45°
		assertEquals(-35, CameraMath.idleTarget(10, -80, 45), 1e-4);
		assertEquals(0, CameraMath.wrap(CameraMath.idleTarget(170, -170, 45) - 170), 1e-4); // 20° à travers ±180 : immobile
		assertEquals(10, CameraMath.idleTarget(10, 100, 180), 1e-4);
	}

	@Test
	void pitchClamp() {
		assertEquals(90, CameraMath.clampPitch(120));
		assertEquals(-90, CameraMath.clampPitch(-120));
		assertEquals(12, CameraMath.clampPitch(12));
	}
}

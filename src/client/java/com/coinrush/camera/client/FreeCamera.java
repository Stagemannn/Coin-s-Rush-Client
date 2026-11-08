package com.coinrush.camera.client;

import com.coinrush.camera.client.mixin.ClientInputAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;

/**
 * Caméra 3e personne libre, en vue de dos : la caméra tourne autour du personnage sans le faire tourner ; le personnage se tourne vers sa
 * direction de marche (touches relatives à la caméra). Seule la rotation envoyée avec les mouvements normaux change : rien côté serveur.
 * Hors vue de dos, avec un autre objet comme caméra (cinématique), en véhicule, en spectateur ou si l'option est coupée : comportement vanilla.
 */
public final class FreeCamera {
	private static boolean wasActive;
	private static float yaw, pitch;
	private static int align;

	private FreeCamera() { }

	/** Vrai si la caméra libre pilote la vue ; gère le passage entre les deux modes. */
	public static boolean active() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		boolean on = p != null && CameraOptions.enabled && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK && mc.getCameraEntity() == p
				&& !p.isPassenger() && !p.isSpectator() && !p.isSleeping() && (CameraOptions.elytra || !p.isFallFlying());
		if (on && !wasActive) {
			yaw = p.getYRot();
			pitch = p.getXRot();
			align = 0;
		} else if (!on && wasActive && p != null && mc.getCameraEntity() == p) {
			snapPlayer(p); // première personne, élytres... : on regarde là où la caméra regardait
		}
		wasActive = on;
		return on;
	}

	/** Vrai si {@code e} est le joueur local piloté par la caméra libre. */
	public static boolean useCamera(Entity e) {
		return e == Minecraft.getInstance().player && active();
	}

	public static float yaw() {
		return yaw;
	}

	public static float pitch() {
		return pitch;
	}

	/** Mouvement de souris : il tourne la caméra, pas le personnage. */
	public static void turn(double dx, double dy) {
		yaw += (float) dx * 0.15f;
		pitch = CameraMath.clampPitch(pitch + (float) dy * 0.15f);
	}

	private static void snapPlayer(LocalPlayer p) {
		p.setYRot(yaw);
		p.setXRot(pitch);
		p.yRotO = yaw;
		p.xRotO = pitch;
	}

	/** Début de tick : un clic aligne aussitôt le personnage sur la caméra (ce qu'on vise est ce que la caméra regarde) et arme le minuteur. */
	public static void startTick(Minecraft mc) {
		if (!active() || mc.screen != null) return;
		if (CameraOptions.alignOnClick && (mc.options.keyAttack.isDown() || mc.options.keyUse.isDown())) {
			snapPlayer(mc.player);
			align = CameraOptions.alignTicks;
		} else if (align > 0) {
			align--;
		}
	}

	/** Fin du calcul des touches, avant le déplacement du tick : tourne le personnage et convertit les touches. */
	public static void input(ClientInput input) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer p = mc.player;
		if (p == null || mc.screen != null || !active()) return;
		Vec2 move = input.getMoveVector();
		boolean moving = move.x != 0 || move.y != 0;
		boolean follow = align > 0 || p.isFallFlying(); // alignement après un clic, ou vol en élytres : le personnage suit la caméra
		if (!follow && !moving) return; // à l'arrêt, le personnage ne bouge pas : seule la caméra tourne (comme Better Third Person)
		float target = follow ? yaw : CameraMath.moveYaw(yaw, move.x, move.y);
		p.setYRot(CameraMath.stepToward(p.getYRot(), target, CameraOptions.rotationSpeed));
		if (follow) p.setXRot(CameraMath.stepPitch(p.getXRot(), pitch, CameraOptions.pitchSpeed));
		if (moving && !follow) {
			// Toute touche de marche devient « avancer » : le personnage va dans la direction où il se tourne (et peut courir dans toutes).
			Input keys = input.keyPresses;
			input.keyPresses = new Input(true, false, false, false, keys.jump(), keys.shift(), keys.sprint());
			((ClientInputAccessor) input).coinrush$setMoveVector(new Vec2(0, Math.min(1f, move.length())));
		}
	}
}

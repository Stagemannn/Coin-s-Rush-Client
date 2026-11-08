package com.coinrush.cinematique.client.mixin;

import com.coinrush.cinematique.client.LecteurClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cinématique : la caméra se place d'elle-même et filme le joueur, qui reste visible (comme l'intro de FK). */
// Priorité haute : appliqué après les mods de caméra (Le StageMod), dont il écrase la position pendant une cinématique.
@Mixin(value = Camera.class, priority = 2000)
public abstract class CameraMixin {
	@Shadow
	private boolean detached;

	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Shadow
	protected abstract void setRotation(float yRot, float xRot);

	@Inject(method = "setup", at = @At("TAIL"))
	private void coinrush$cinematique(BlockGetter level, Entity entity, boolean detached, boolean mirror, float partialTick, CallbackInfo ci) {
		double[] cine = LecteurClient.camera(partialTick);
		if (cine == null) return;
		this.detached = true;
		setRotation((float) cine[3], (float) cine[4]);
		setPosition(cine[0], cine[1], cine[2]);
	}
}

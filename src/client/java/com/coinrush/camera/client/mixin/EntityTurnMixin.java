package com.coinrush.camera.client.mixin;

import com.coinrush.camera.client.FreeCamera;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Caméra libre : la souris tourne la caméra et laisse le joueur local où il est. */
@Mixin(Entity.class)
public abstract class EntityTurnMixin {
	@Inject(method = "turn", at = @At("HEAD"), cancellable = true)
	private void coinrush$turnCamera(double yRot, double xRot, CallbackInfo ci) {
		if (FreeCamera.useCamera((Entity) (Object) this)) {
			FreeCamera.turn(yRot, xRot);
			ci.cancel();
		}
	}
}

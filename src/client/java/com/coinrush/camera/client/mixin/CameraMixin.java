package com.coinrush.camera.client.mixin;

import com.coinrush.camera.client.CameraOptions;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 3e personne vue de dos : décalage latéral réglé dans les options (positif = caméra à droite), arrêté par les blocs. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	protected abstract void setPosition(double x, double y, double z);

	@Inject(method = "setup", at = @At("TAIL"))
	private void coinrush$sideOffset(BlockGetter level, Entity entity, boolean detached, boolean mirror, float partialTick, CallbackInfo ci) {
		double off = CameraOptions.cameraOffset;
		if (!detached || mirror || off == 0) return;
		// Comme le recul vanilla : huit rayons décalés de 0,1 (les coins) du point vanilla vers le point décalé ; on s'arrête au plus proche obstacle.
		Camera self = (Camera) (Object) this;
		var left = self.getLeftVector();
		Vec3 p0 = self.getPosition(), p1 = p0.subtract(left.x() * off, left.y() * off, left.z() * off);
		double allowed = p0.distanceTo(p1);
		for (int i = 0; i < 8; i++) {
			Vec3 c = new Vec3((i & 1) * 0.2 - 0.1, (i >> 1 & 1) * 0.2 - 0.1, (i >> 2 & 1) * 0.2 - 0.1);
			var hit = level.clip(new ClipContext(p0.add(c), p1.add(c), ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity));
			if (hit.getType() != HitResult.Type.MISS) allowed = Math.min(allowed, hit.getLocation().distanceTo(p0.add(c)));
		}
		Vec3 end = p0.add(p1.subtract(p0).normalize().scale(allowed));
		setPosition(end.x, end.y, end.z);
	}
}

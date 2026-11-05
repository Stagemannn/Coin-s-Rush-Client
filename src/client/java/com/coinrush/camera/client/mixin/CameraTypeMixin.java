package com.coinrush.camera.client.mixin;

import com.coinrush.camera.client.CameraOptions;
import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Option « sans vue de face » : F5 passe de la vue de dos à la première personne. */
@Mixin(CameraType.class)
public abstract class CameraTypeMixin {
	@Inject(method = "cycle", at = @At("RETURN"), cancellable = true)
	private void coinrush$skipFront(CallbackInfoReturnable<CameraType> cir) {
		if (CameraOptions.enabled && CameraOptions.noFront && cir.getReturnValue() == CameraType.THIRD_PERSON_FRONT) cir.setReturnValue(CameraType.FIRST_PERSON);
	}
}

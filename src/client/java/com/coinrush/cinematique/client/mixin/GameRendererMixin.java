package com.coinrush.cinematique.client.mixin;

import com.coinrush.cinematique.client.LecteurClient;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pendant une cinématique : champ de vision « Quake Pro » (110) et main masquée. Les options du joueur ne sont pas touchées. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
	private void coinrush$fovCinematique(CallbackInfoReturnable<Float> cir) {
		if (LecteurClient.actif()) cir.setReturnValue(110f);
	}

	@Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
	private void coinrush$sansMain(CallbackInfo ci) {
		if (LecteurClient.camera(0) != null) ci.cancel();
	}
}

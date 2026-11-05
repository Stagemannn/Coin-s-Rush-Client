package com.coinrush.camera.client.mixin;

import com.coinrush.camera.client.FreeCamera;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Caméra libre : les touches sont relues par rapport à la caméra une fois calculées par le jeu. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin {
	@Inject(method = "tick", at = @At("TAIL"))
	private void coinrush$cameraRelativeMove(CallbackInfo ci) {
		FreeCamera.input((ClientInput) (Object) this);
	}
}

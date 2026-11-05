package com.coinrush.carte.client.mixin;

import com.coinrush.carte.client.CarteClient;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ajoute le bouton « Map » à l'écran d'inventaire, après sa construction (donc après le décalage du livre de recettes). */
@Mixin(InventoryScreen.class)
abstract class InventaireMixin {
	@Inject(method = "init", at = @At("TAIL"))
	private void coinrush$boutonMap(CallbackInfo ci) {
		CarteClient.ajouterBouton((InventoryScreen) (Object) this);
	}
}

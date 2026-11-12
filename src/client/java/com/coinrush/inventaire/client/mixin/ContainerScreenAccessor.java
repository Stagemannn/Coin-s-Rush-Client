package com.coinrush.inventaire.client.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Position de l'écran d'inventaire, pour placer les boutons et le profil à sa gauche. */
@Mixin(AbstractContainerScreen.class)
public interface ContainerScreenAccessor {
	@Accessor("leftPos")
	int coinrush$leftPos();

	@Accessor("topPos")
	int coinrush$topPos();

	@Accessor("imageWidth")
	int coinrush$imageWidth();
}

package com.coinrush.camera.client;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Sous-catégorie « Caméra » des options Coin Rush : caméra 3e personne libre et décalage latéral. */
public final class CameraOptionsScreen extends Screen {
	private final Screen parent;

	public CameraOptionsScreen(Screen parent) {
		super(Component.literal("Caméra"));
		this.parent = parent;
	}

	private Button toggle(int x, int y, int w, String label, BooleanSupplier get, Consumer<Boolean> set) {
		return Button.builder(Component.literal(label + " : " + (get.getAsBoolean() ? "Oui" : "Non")), b -> {
			set.accept(!get.getAsBoolean());
			b.setMessage(Component.literal(label + " : " + (get.getAsBoolean() ? "Oui" : "Non")));
		}).bounds(x, y, w, 20).build();
	}

	/** Curseur à valeurs entières (ou au dixième) entre {@code min} et {@code max}. */
	private static final class ValueSlider extends AbstractSliderButton {
		private final double min, max, step;
		private final DoubleFunction<String> text;
		private final Consumer<Double> set;

		ValueSlider(int x, int y, int w, double min, double max, double step, double current, DoubleFunction<String> text, Consumer<Double> set) {
			super(x, y, w, 20, Component.empty(), (current - min) / (max - min));
			this.min = min;
			this.max = max;
			this.step = step;
			this.text = text;
			this.set = set;
			updateMessage();
		}

		private double current() {
			return Math.round((min + value * (max - min)) / step) * step;
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(text.apply(current())));
		}

		@Override
		protected void applyValue() {
			set.accept(current());
		}
	}

	@Override
	protected void init() {
		int w = Math.min(190, (width - 20) / 2 - 5);
		int x1 = width / 2 - w - 5, x2 = width / 2 + 5, y = Math.max(30, height / 2 - 85);
		addRenderableWidget(toggle(x1, y, w, "Caméra libre", () -> CameraOptions.enabled, v -> CameraOptions.enabled = v));
		addRenderableWidget(new ValueSlider(x2, y, w, -2, 2, 0.1, CameraOptions.cameraOffset,
				v -> "Décalage : " + (v == 0 ? "centré" : v < 0 ? "à gauche " + (-v) : "à droite " + v), v -> CameraOptions.cameraOffset = v));
		y += 24;
		addRenderableWidget(toggle(x1, y, w, "Alignement au clic", () -> CameraOptions.alignOnClick, v -> CameraOptions.alignOnClick = v));
		addRenderableWidget(new ValueSlider(x2, y, w, 0, 100, 1, CameraOptions.alignTicks,
				v -> "Durée alignement : " + (int) v + " ticks", v -> CameraOptions.alignTicks = v.intValue()));
		y += 24;
		addRenderableWidget(toggle(x1, y, w, "Libre en élytres", () -> CameraOptions.elytra, v -> CameraOptions.elytra = v));
		addRenderableWidget(toggle(x2, y, w, "Sans vue de face", () -> CameraOptions.noFront, v -> CameraOptions.noFront = v));
		y += 24;
		addRenderableWidget(new ValueSlider(x1, y, w, 0, 100, 1, CameraOptions.rotationSpeed,
				v -> "Rotation perso : " + (int) v, v -> CameraOptions.rotationSpeed = v.intValue()));
		addRenderableWidget(new ValueSlider(x2, y, w, 0, 100, 1, CameraOptions.pitchSpeed,
				v -> "Inclinaison perso : " + (int) v, v -> CameraOptions.pitchSpeed = v.intValue()));
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, Math.min(height - 28, y + 40), 200, 20).build());
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		g.drawCenteredString(font, title, width / 2, 14, 0xFFFFFFFF);
	}

	@Override
	public void removed() {
		CameraOptions.save();
	}

	@Override
	public void onClose() {
		minecraft.setScreen(parent);
	}
}

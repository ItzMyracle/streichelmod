package com.streichelmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;

/**
 * Streichel Mod: Wenn du einen anderen Spieler triffst, streichelst du ihn.
 * Es gibt nur Effekte auf deinem eigenen Bildschirm (Partikel, Sound, Actionbar).
 * Der Treffer selbst wird nicht veraendert.
 */
public class StreichelModClient implements ClientModInitializer {
	public static final String MOD_ID = "streichelmod";
	private static final int STYLE_COUNT = 3;

	private static KeyMapping toggleKey;
	private static KeyMapping styleKey;

	private long lastPetTick = -100;

	@Override
	public void onInitializeClient() {
		PetSettings.load();

		// Alle Tasten erscheinen unter Optionen > Steuerung > Tastenbelegung > "Streichel Mod"
		KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

		toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.streichelmod.toggle",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				category));

		styleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.streichelmod.style",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_J,
				category));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleKey.consumeClick()) {
				PetSettings.enabled = !PetSettings.enabled;
				PetSettings.save();
				if (client.player != null) {
					client.player.displayClientMessage(Component.translatable(
							PetSettings.enabled ? "message.streichelmod.on" : "message.streichelmod.off"), true);
				}
			}

			while (styleKey.consumeClick()) {
				PetSettings.style = (PetSettings.style + 1) % STYLE_COUNT;
				PetSettings.save();
				if (client.player != null) {
					client.player.displayClientMessage(Component.translatable(
							"message.streichelmod.style",
							Component.translatable("style.streichelmod." + PetSettings.style)), true);
				}
			}
		});

		AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (level.isClientSide() && PetSettings.enabled && entity instanceof Player target && target != player) {
				pet(player, level, target);
			}
			// PASS = der normale Treffer laeuft unveraendert weiter
			return InteractionResult.PASS;
		});
	}

	private void pet(Player me, Level level, Player target) {
		long now = level.getGameTime();
		if (now - lastPetTick < 4) {
			return;
		}
		lastPetTick = now;

		ParticleOptions particle;
		SoundEvent sound;
		float pitch;
		boolean note = false;

		switch (PetSettings.style) {
			case 1 -> {
				particle = ParticleTypes.HAPPY_VILLAGER;
				sound = SoundEvents.AMETHYST_BLOCK_CHIME;
				pitch = 1.4F;
			}
			case 2 -> {
				particle = ParticleTypes.NOTE;
				sound = SoundEvents.EXPERIENCE_ORB_PICKUP;
				pitch = 1.2F;
				note = true;
			}
			default -> {
				particle = ParticleTypes.HEART;
				sound = SoundEvents.CAT_PURR;
				pitch = 1.0F;
			}
		}

		RandomSource random = target.getRandom();
		for (int i = 0; i < 5; i++) {
			double x = target.getX() + (random.nextDouble() - 0.5D) * target.getBbWidth();
			double y = target.getY() + random.nextDouble() * target.getBbHeight() + 0.2D;
			double z = target.getZ() + (random.nextDouble() - 0.5D) * target.getBbWidth();
			// Bei NOTE bestimmt der erste Wert die Farbe (0 bis 1)
			double xd = note ? random.nextDouble() : 0.0D;
			double yd = note ? 0.0D : 0.1D;
			level.addParticle(particle, x, y, z, xd, yd, 0.0D);
		}

		level.playLocalSound(target.getX(), target.getY(), target.getZ(), sound, SoundSource.PLAYERS, 0.6F, pitch, false);

		me.displayClientMessage(Component.translatable("message.streichelmod.petted", target.getDisplayName()), true);
	}
}

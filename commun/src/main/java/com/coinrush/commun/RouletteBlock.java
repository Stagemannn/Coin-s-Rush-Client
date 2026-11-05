package com.coinrush.commun;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Bloc de la roulette. Il ne sait rien du jeu : le serveur branche {@link #surPose} et {@link #surRetrait} pour faire apparaître
 * et disparaître le nom flottant ; sans le serveur (côté client) ils ne font rien. Le fonctionnement de la roulette est côté serveur.
 */
public class RouletteBlock extends Block {
	public static volatile BiConsumer<ServerLevel, BlockPos> surPose = (level, pos) -> { };
	public static volatile BiConsumer<ServerLevel, BlockPos> surRetrait = (level, pos) -> { };

	public RouletteBlock(Properties properties) {
		super(properties);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel s) surPose.accept(s, pos);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		surRetrait.accept(level, pos);
	}
}

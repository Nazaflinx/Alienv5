package dev.luminous.mod.modules.impl.render;

import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.api.utils.math.MathUtil;
import dev.luminous.api.utils.render.Render3DUtil;
import dev.luminous.api.utils.world.BlockUtil;
import dev.luminous.asm.accessors.IEntity;
import dev.luminous.mod.modules.Module;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.EnderChestBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;

public class ESP extends Module {
	private final ColorSetting item = add(new ColorSetting("Item", new Color(255, 255, 255, 100)).injectBoolean(true));
	private final ColorSetting player = add(new ColorSetting("Player", new Color(255, 255, 255, 100)).injectBoolean(true));
	private final ColorSetting chest = add(new ColorSetting("Chest", new Color(255, 255, 255, 100)).injectBoolean(false));
	private final ColorSetting enderChest = add(new ColorSetting("EnderChest", new Color(255, 100, 255, 100)).injectBoolean(false));
	private final ColorSetting shulkerBox = add(new ColorSetting("ShulkerBox", new Color(15, 255, 255, 100)).injectBoolean(false));
	public ESP() {
		super("ESP", Category.Render);
		setChinese("透视");
	}

    @Override
	public void onRender3D(MatrixStack matrixStack) {
		if (item.booleanValue || player.booleanValue) {
			float tickDelta = mc.getTickDelta();
			for (Entity entity : mc.world.getEntities()) {
				if (entity instanceof ItemEntity && item.booleanValue) {
					Vec3d interpolatedPos = new Vec3d(
						MathUtil.interpolate(entity.lastRenderX, entity.getX(), tickDelta),
						MathUtil.interpolate(entity.lastRenderY, entity.getY(), tickDelta),
						MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), tickDelta)
					);
					Box entityBox = ((IEntity) entity).getDimensions().getBoxAt(interpolatedPos);
					Render3DUtil.draw3DBox(matrixStack, entityBox, item.getValue(), false, true);
				} else if (entity instanceof PlayerEntity && entity != mc.player && player.booleanValue) {
					Vec3d interpolatedPos = new Vec3d(
						MathUtil.interpolate(entity.lastRenderX, entity.getX(), tickDelta),
						MathUtil.interpolate(entity.lastRenderY, entity.getY(), tickDelta),
						MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), tickDelta)
					);
					Box entityBox = ((IEntity) entity).getDimensions().getBoxAt(interpolatedPos).expand(0, 0.1, 0);
					Render3DUtil.draw3DBox(matrixStack, entityBox, player.getValue(), false, true);
				}
			}
		}

		if (chest.booleanValue || enderChest.booleanValue || shulkerBox.booleanValue) {
			ArrayList<BlockEntity> blockEntities = BlockUtil.getTileEntities();
			for (BlockEntity blockEntity : blockEntities) {
				Box box = null;
				Color color = null;

				if (blockEntity instanceof ChestBlockEntity && chest.booleanValue) {
					box = new Box(blockEntity.getPos());
					color = chest.getValue();
				} else if (blockEntity instanceof EnderChestBlockEntity && enderChest.booleanValue) {
					box = new Box(blockEntity.getPos());
					color = enderChest.getValue();
				} else if (blockEntity instanceof ShulkerBoxBlockEntity && shulkerBox.booleanValue) {
					box = new Box(blockEntity.getPos());
					color = shulkerBox.getValue();
				}

				if (box != null && color != null) {
					Render3DUtil.draw3DBox(matrixStack, box, color);
				}
			}
		}
	}
}

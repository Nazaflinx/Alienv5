package dev.luminous.mod.modules.impl.render;

import dev.luminous.Alien;
import dev.luminous.mod.modules.settings.impl.BooleanSetting;
import dev.luminous.mod.modules.settings.impl.ColorSetting;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
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
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.ArrayList;

public class ESP extends Module {
	private final ColorSetting item = add(new ColorSetting("Item", new Color(255, 255, 255, 100)).injectBoolean(true));
	private final ColorSetting player = add(new ColorSetting("Player", new Color(255, 255, 255, 100)).injectBoolean(true));
	private final ColorSetting friendColor = add(new ColorSetting("Friend", new Color(0, 255, 255, 100)).injectBoolean(true));
	private final ColorSetting mob = add(new ColorSetting("Mob", new Color(255, 0, 0, 100)).injectBoolean(false));
	private final ColorSetting passive = add(new ColorSetting("Passive", new Color(0, 255, 0, 100)).injectBoolean(false));
	private final ColorSetting vehicle = add(new ColorSetting("Vehicle", new Color(255, 255, 0, 100)).injectBoolean(false));
	private final ColorSetting chest = add(new ColorSetting("Chest", new Color(255, 100, 0, 100)).injectBoolean(false));
	private final ColorSetting enderChest = add(new ColorSetting("EnderChest", new Color(255, 0, 255, 100)).injectBoolean(false));
	private final ColorSetting shulkerBox = add(new ColorSetting("ShulkerBox", new Color(0, 255, 255, 100)).injectBoolean(false));
	private final BooleanSetting outline = add(new BooleanSetting("Outline", true));
	private final BooleanSetting fill = add(new BooleanSetting("Fill", true));
	private final SliderSetting lineWidth = add(new SliderSetting("LineWidth", 1.5, 0.1, 5.0, 0.1));
	private final SliderSetting range = add(new SliderSetting("Range", 64.0, 16.0, 256.0).setSuffix("m"));

	public ESP() {
		super("ESP", Category.Render);
		setChinese("透视");
	}

    @Override
	public void onRender3D(MatrixStack matrixStack) {
		if (item.booleanValue || player.booleanValue || mob.booleanValue || passive.booleanValue || vehicle.booleanValue) {
			float tickDelta = mc.getTickDelta();
			double rangeSquared = range.getValue() * range.getValue();
			Vec3d playerPos = mc.player.getPos();

			for (Entity entity : mc.world.getEntities()) {
				if (entity == mc.player) continue;
				if (playerPos.squaredDistanceTo(entity.getPos()) > rangeSquared) continue;

				Box entityBox = null;
				Color color = null;

				if (entity instanceof ItemEntity && item.booleanValue) {
					Vec3d interpolatedPos = new Vec3d(
						MathUtil.interpolate(entity.lastRenderX, entity.getX(), tickDelta),
						MathUtil.interpolate(entity.lastRenderY, entity.getY(), tickDelta),
						MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), tickDelta)
					);
					entityBox = ((IEntity) entity).getDimensions().getBoxAt(interpolatedPos);
					color = item.getValue();
				} else if (entity instanceof PlayerEntity && player.booleanValue) {
					Vec3d interpolatedPos = new Vec3d(
						MathUtil.interpolate(entity.lastRenderX, entity.getX(), tickDelta),
						MathUtil.interpolate(entity.lastRenderY, entity.getY(), tickDelta),
						MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), tickDelta)
					);
					entityBox = ((IEntity) entity).getDimensions().getBoxAt(interpolatedPos).expand(0, 0.1, 0);

					if (Alien.FRIEND.isFriend((PlayerEntity) entity) && friendColor.booleanValue) {
						color = friendColor.getValue();
					} else {
						color = player.getValue();
					}
				} else if (entity instanceof HostileEntity && mob.booleanValue) {
					Vec3d interpolatedPos = new Vec3d(
						MathUtil.interpolate(entity.lastRenderX, entity.getX(), tickDelta),
						MathUtil.interpolate(entity.lastRenderY, entity.getY(), tickDelta),
						MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), tickDelta)
					);
					entityBox = ((IEntity) entity).getDimensions().getBoxAt(interpolatedPos);
					color = mob.getValue();
				} else if (entity instanceof PassiveEntity && passive.booleanValue) {
					Vec3d interpolatedPos = new Vec3d(
						MathUtil.interpolate(entity.lastRenderX, entity.getX(), tickDelta),
						MathUtil.interpolate(entity.lastRenderY, entity.getY(), tickDelta),
						MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), tickDelta)
					);
					entityBox = ((IEntity) entity).getDimensions().getBoxAt(interpolatedPos);
					color = passive.getValue();
				} else if (entity instanceof MinecartEntity && vehicle.booleanValue) {
					Vec3d interpolatedPos = new Vec3d(
						MathUtil.interpolate(entity.lastRenderX, entity.getX(), tickDelta),
						MathUtil.interpolate(entity.lastRenderY, entity.getY(), tickDelta),
						MathUtil.interpolate(entity.lastRenderZ, entity.getZ(), tickDelta)
					);
					entityBox = ((IEntity) entity).getDimensions().getBoxAt(interpolatedPos);
					color = vehicle.getValue();
				}

				if (entityBox != null && color != null) {
					if (fill.getValue() && outline.getValue()) {
						Render3DUtil.draw3DBox(matrixStack, entityBox, color, outline.getValue(), fill.getValue(), lineWidth.getValueFloat());
					} else if (outline.getValue()) {
						Render3DUtil.drawBox(matrixStack, entityBox, color, lineWidth.getValueFloat());
					} else if (fill.getValue()) {
						Render3DUtil.drawFill(matrixStack, entityBox, color);
					}
				}
			}
		}

		if (chest.booleanValue || enderChest.booleanValue || shulkerBox.booleanValue) {
			ArrayList<BlockEntity> blockEntities = BlockUtil.getTileEntities();
			double rangeSquared = range.getValue() * range.getValue();
			Vec3d playerPos = mc.player.getPos();

			for (BlockEntity blockEntity : blockEntities) {
				if (playerPos.squaredDistanceTo(Vec3d.ofCenter(blockEntity.getPos())) > rangeSquared) continue;

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
					if (fill.getValue() && outline.getValue()) {
						Render3DUtil.draw3DBox(matrixStack, box, color, outline.getValue(), fill.getValue(), lineWidth.getValueFloat());
					} else if (outline.getValue()) {
						Render3DUtil.drawBox(matrixStack, box, color, lineWidth.getValueFloat());
					} else if (fill.getValue()) {
						Render3DUtil.drawFill(matrixStack, box, color);
					}
				}
			}
		}
	}
}

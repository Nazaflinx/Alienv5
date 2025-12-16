package dev.luminous.mod.gui.notification;

import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.math.Animation;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.awt.*;

public class Notification implements Wrapper {
    private final String title;
    private final String message;
    private final Type type;
    private final long creationTime;
    private final long duration;
    private boolean dismissed = false;

    private final Animation slideAnimation = new Animation();
    private final Animation fadeAnimation = new Animation();

    public Notification(String title, String message, Type type, long duration) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.creationTime = System.currentTimeMillis();
        this.duration = duration;
    }

    public void draw(DrawContext drawContext, int x, int y, int width) {
        long elapsed = System.currentTimeMillis() - creationTime;
        double progress = Math.min(1.0, elapsed / 300.0);

        double slideOffset = slideAnimation.get(dismissed ? width + 20 : 0);
        double alpha = fadeAnimation.get(dismissed ? 0.0 : 1.0);

        if (elapsed > duration - 300 && !dismissed) {
            alpha = 1.0 - ((elapsed - (duration - 300)) / 300.0);
        }

        if (alpha < 0.01) return;

        MatrixStack matrixStack = drawContext.getMatrices();
        int height = 60;

        int drawX = (int) (x - slideOffset);
        int drawY = y;

        Color bgColor = new Color(25, 25, 35, (int) (230 * alpha));
        Color accentColor = new Color(
            type.color.getRed(),
            type.color.getGreen(),
            type.color.getBlue(),
            (int) (255 * alpha)
        );

        Render2DUtil.drawRound(matrixStack, drawX, drawY, width, height, 6, bgColor);
        Render2DUtil.drawRect(matrixStack, drawX, drawY, 4, height, accentColor);

        String icon = type.icon;
        int iconSize = 24;
        int iconX = drawX + 12;
        int iconY = drawY + (height - iconSize) / 2;

        Render2DUtil.drawRound(matrixStack, iconX, iconY, iconSize, iconSize, 4,
            new Color(type.color.getRed(), type.color.getGreen(), type.color.getBlue(), (int) (40 * alpha)));

        TextUtil.drawString(drawContext, icon, iconX + iconSize / 2f - TextUtil.getWidth(icon) / 2,
            iconY + iconSize / 2f - TextUtil.getHeight() / 2, accentColor.getRGB());

        int textX = iconX + iconSize + 10;
        int textY = drawY + 12;

        Color titleColor = new Color(255, 255, 255, (int) (255 * alpha));
        TextUtil.drawString(drawContext, title, textX, textY, titleColor.getRGB());

        Color msgColor = new Color(200, 200, 200, (int) (200 * alpha));
        TextUtil.drawStringWithScale(drawContext, message, textX, textY + TextUtil.getHeight() + 4,
            msgColor, 0.85f);

        if (elapsed < duration - 300) {
            double timeProgress = (double) elapsed / (duration - 300);
            int barWidth = (int) (width * timeProgress);
            Render2DUtil.drawRect(matrixStack, drawX, drawY + height - 2, barWidth, 2,
                new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), (int) (150 * alpha)));
        }

        String closeIcon = "×";
        int closeX = drawX + width - 20;
        int closeY = drawY + 5;
        TextUtil.drawString(drawContext, closeIcon, closeX, closeY,
            new Color(180, 180, 180, (int) (180 * alpha)).getRGB());
    }

    public boolean shouldRemove() {
        return System.currentTimeMillis() - creationTime > duration || (dismissed && fadeAnimation.get(0) < 0.01);
    }

    public void dismiss() {
        dismissed = true;
    }

    public boolean isHovered(int mouseX, int mouseY, int x, int y, int width) {
        int height = 60;
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isCloseButtonHovered(int mouseX, int mouseY, int x, int y, int width) {
        int closeX = x + width - 20;
        int closeY = y + 5;
        return mouseX >= closeX && mouseX <= closeX + 15 && mouseY >= closeY && mouseY <= closeY + 15;
    }

    public enum Type {
        INFO("ℹ", new Color(52, 152, 219)),
        SUCCESS("✓", new Color(46, 204, 113)),
        WARNING("⚠", new Color(241, 196, 15)),
        ERROR("✕", new Color(231, 76, 60));

        public final String icon;
        public final Color color;

        Type(String icon, Color color) {
            this.icon = icon;
            this.color = color;
        }
    }
}

package dev.luminous.mod.gui.clickgui;

import dev.luminous.Alien;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render2DUtil;
import dev.luminous.api.utils.render.TextUtil;
import dev.luminous.mod.modules.impl.client.ClickGui;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

public class SearchBar implements Wrapper {
    private String searchText = "";
    private boolean focused = false;
    private int x;
    private int y;
    private int width = 200;
    private int height = 20;
    private long lastBlinkTime = 0;
    private boolean cursorVisible = true;

    public SearchBar(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(DrawContext drawContext, int mouseX, int mouseY) {
        MatrixStack matrixStack = drawContext.getMatrices();

        boolean hovered = isHovered(mouseX, mouseY);
        Color bgColor = focused ? new Color(40, 40, 60, 230) :
                        hovered ? new Color(35, 35, 55, 220) :
                        new Color(30, 30, 50, 200);

        Render2DUtil.drawRound(matrixStack, x, y, width, height, 3, bgColor);

        if (focused || !searchText.isEmpty()) {
            Render2DUtil.drawRoundOutline(matrixStack, x, y, width, height, 3, 1.5f,
                ClickGui.INSTANCE.color.getValue());
        }

        String displayText = searchText.isEmpty() && !focused ? "Search modules..." : searchText;
        Color textColor = searchText.isEmpty() && !focused ?
            new Color(150, 150, 150, 180) : new Color(255, 255, 255);

        TextUtil.drawString(drawContext, displayText, x + 8, y + (height - TextUtil.getHeight()) / 2, textColor.getRGB());

        if (focused && System.currentTimeMillis() - lastBlinkTime > 500) {
            cursorVisible = !cursorVisible;
            lastBlinkTime = System.currentTimeMillis();
        }

        if (focused && cursorVisible) {
            float cursorX = x + 8 + TextUtil.getWidth(searchText);
            Render2DUtil.drawRect(matrixStack, cursorX, y + 4, 1, height - 8,
                new Color(255, 255, 255, 200));
        }

        if (!searchText.isEmpty()) {
            String clearIcon = "×";
            float clearX = x + width - 18;
            float clearY = y + (height - TextUtil.getHeight()) / 2;

            boolean clearHovered = mouseX >= clearX && mouseX <= clearX + 12 &&
                                   mouseY >= y && mouseY <= y + height;

            TextUtil.drawString(drawContext, clearIcon, clearX, clearY,
                clearHovered ? ClickGui.INSTANCE.color.getValue().getRGB() :
                new Color(180, 180, 180).getRGB());
        }
    }

    public void update(double mouseX, double mouseY) {
        if (ClickGuiScreen.clicked) {
            if (isHovered(mouseX, mouseY)) {
                focused = true;
                ClickGuiScreen.clicked = false;
            } else {
                if (!searchText.isEmpty() && mouseX >= x + width - 18 &&
                    mouseX <= x + width - 6 && mouseY >= y && mouseY <= y + height) {
                    searchText = "";
                    ClickGuiScreen.clicked = false;
                    return;
                }
                focused = false;
            }
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
            focused = false;
            return true;
        }

        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;

        if (searchText.length() < 20) {
            searchText += chr;
        }
        return true;
    }

    private boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public String getSearchText() {
        return searchText.toLowerCase();
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    public int getHeight() {
        return height + 10;
    }
}

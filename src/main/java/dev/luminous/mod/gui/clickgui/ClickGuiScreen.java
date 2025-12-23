package dev.luminous.mod.gui.clickgui;

import dev.luminous.Alien;
import dev.luminous.mod.gui.clickgui.tabs.Tab;
import dev.luminous.mod.gui.notification.NotificationManager;
import dev.luminous.mod.modules.impl.client.ClickGui;
import dev.luminous.mod.modules.settings.impl.SliderSetting;
import dev.luminous.mod.modules.settings.impl.StringSetting;
import dev.luminous.api.utils.Wrapper;
import dev.luminous.api.utils.render.Render2DUtil;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.*;

public class ClickGuiScreen extends Screen implements Wrapper {

    public static final SearchBar searchBar = new SearchBar(10, 10);
    public static final Tooltip tooltip = new Tooltip();

    public ClickGuiScreen() {
        super(Text.of("ClickGui"));
    }
    public static boolean clicked = false;
    public static boolean rightClicked = false;
    public static boolean hoverClicked = false;

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext drawContext, int mouseX, int mouseY, float partialTicks) {
        super.render(drawContext, mouseX, mouseY, partialTicks);
        if (ClickGui.INSTANCE.glass.getValue()) {
            int width = mc.getWindow().getScaledWidth();
            int height = mc.getWindow().getScaledHeight();
            Render2DUtil.verticalGradient(drawContext.getMatrices(), 0, 0, width, height,
                    new Color(0, 0, 0, 90), new Color(20, 20, 40, 180));
            Render2DUtil.horizontalGradient(drawContext.getMatrices(), 0, 0, width, height,
                    new Color(0, 180, 255, 40), new Color(138, 43, 226, 40));
        }
        Alien.GUI.draw(mouseX, mouseY, drawContext, partialTicks);
        searchBar.draw(drawContext, mouseX, mouseY);
        tooltip.draw(drawContext);
        NotificationManager.getInstance().draw(drawContext);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBar.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        Alien.MODULE.modules.forEach(module -> module.getSettings().stream()
                .filter(setting -> setting instanceof StringSetting)
                .map(setting -> (StringSetting) setting)
                .filter(StringSetting::isListening)
                .forEach(setting -> setting.keyType(keyCode)));
        Alien.MODULE.modules.forEach(module -> module.getSettings().stream()
                .filter(setting -> setting instanceof SliderSetting)
                .map(setting -> (SliderSetting) setting)
                .filter(SliderSetting::isListening)
                .forEach(setting -> setting.keyType(keyCode)));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchBar.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            hoverClicked = false;
            clicked = true;
            NotificationManager.getInstance().handleClick(mouseX, mouseY);
            searchBar.update(mouseX, mouseY);
        } else if (button == 1) {
            rightClicked = true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            clicked = false;
            hoverClicked = false;
        } else if (button == 1) {
            rightClicked = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void close() {
        super.close();
        rightClicked = false;
        hoverClicked = false;
        clicked = false;
        searchBar.setFocused(false);
        tooltip.endHover();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (Tab tab : Alien.GUI.tabs) {
            tab.setY((int) (tab.getY() + (verticalAmount * 30)));
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}

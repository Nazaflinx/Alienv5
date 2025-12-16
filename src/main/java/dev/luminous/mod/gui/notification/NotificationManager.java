package dev.luminous.mod.gui.notification;

import dev.luminous.api.utils.Wrapper;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class NotificationManager implements Wrapper {
    private static final NotificationManager INSTANCE = new NotificationManager();
    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    private static final int NOTIFICATION_WIDTH = 280;
    private static final int NOTIFICATION_HEIGHT = 60;
    private static final int NOTIFICATION_SPACING = 10;
    private static final int MARGIN_RIGHT = 10;
    private static final int MARGIN_TOP = 10;

    public static NotificationManager getInstance() {
        return INSTANCE;
    }

    public void addNotification(String title, String message, Notification.Type type, long duration) {
        notifications.add(new Notification(title, message, type, duration));
    }

    public void addNotification(String title, String message, Notification.Type type) {
        addNotification(title, message, type, 3000);
    }

    public void info(String title, String message) {
        addNotification(title, message, Notification.Type.INFO);
    }

    public void success(String title, String message) {
        addNotification(title, message, Notification.Type.SUCCESS);
    }

    public void warning(String title, String message) {
        addNotification(title, message, Notification.Type.WARNING);
    }

    public void error(String title, String message) {
        addNotification(title, message, Notification.Type.ERROR);
    }

    public void draw(DrawContext drawContext) {
        if (mc == null || mc.getWindow() == null) return;

        notifications.removeIf(Notification::shouldRemove);

        int screenWidth = mc.getWindow().getScaledWidth();
        int x = screenWidth - NOTIFICATION_WIDTH - MARGIN_RIGHT;
        int y = MARGIN_TOP;

        List<Notification> reversed = new ArrayList<>(notifications);
        for (int i = reversed.size() - 1; i >= 0; i--) {
            Notification notification = reversed.get(i);
            notification.draw(drawContext, x, y, NOTIFICATION_WIDTH);
            y += NOTIFICATION_HEIGHT + NOTIFICATION_SPACING;
        }
    }

    public void handleClick(double mouseX, double mouseY) {
        if (mc == null || mc.getWindow() == null) return;

        int screenWidth = mc.getWindow().getScaledWidth();
        int x = screenWidth - NOTIFICATION_WIDTH - MARGIN_RIGHT;
        int y = MARGIN_TOP;

        List<Notification> reversed = new ArrayList<>(notifications);
        for (int i = reversed.size() - 1; i >= 0; i--) {
            Notification notification = reversed.get(i);
            if (notification.isCloseButtonHovered((int) mouseX, (int) mouseY, x, y, NOTIFICATION_WIDTH)) {
                notification.dismiss();
                return;
            }
            y += NOTIFICATION_HEIGHT + NOTIFICATION_SPACING;
        }
    }

    public void clear() {
        notifications.clear();
    }
}

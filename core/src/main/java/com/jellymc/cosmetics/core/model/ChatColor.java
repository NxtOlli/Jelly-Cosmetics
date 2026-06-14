package com.jellymc.cosmetics.core.model;

public class ChatColor {

    private final String id;
    private final String name;
    private final String colorCode;
    private final String permission;
    private final boolean animated;
    private final String[] animationFrames;
    private final int animationSpeed;

    /**
     * Creates a new static chat color
     *
     * @param id The unique identifier
     * @param name The display name
     * @param colorCode The color code (e.g. "§c")
     * @param permission The permission required to use this chat color
     */
    public ChatColor(String id, String name, String colorCode, String permission) {
        this(id, name, colorCode, permission, false, null, 0);
    }

    /**
     * Creates a new chat color, which can be animated
     *
     * @param id The unique identifier
     * @param name The display name
     * @param colorCode The color code (e.g. "§c")
     * @param permission The permission required to use this chat color
     * @param animated Whether this chat color is animated
     * @param animationFrames The frames of the animation
     * @param animationSpeed The speed of the animation in ticks per frame
     */
    public ChatColor(String id, String name, String colorCode, String permission,
                     boolean animated, String[] animationFrames, int animationSpeed) {
        this.id = id;
        this.name = name;
        this.colorCode = colorCode;
        this.permission = permission;
        this.animated = animated;
        this.animationFrames = animationFrames;
        this.animationSpeed = animationSpeed;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isAnimated() {
        return animated;
    }

    public String[] getAnimationFrames() {
        return animationFrames;
    }

    public int getAnimationSpeed() {
        return animationSpeed;
    }

    /**
     * Gets the current frame of the animation based on the current tick
     *
     * @param tick The current tick
     * @return The current color code
     */
    public String getCurrentFrame(long tick) {
        if (!animated || animationFrames == null || animationFrames.length == 0) {
            return colorCode;
        }

        int frameIndex = (int) ((tick / animationSpeed) % animationFrames.length);
        return animationFrames[frameIndex];
    }
}

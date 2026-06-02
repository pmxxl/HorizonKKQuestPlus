package com.horizonkk.horizonKKQuestPlus.model;

/**
 * 悬赏主菜单配置
 */
public class MenuConfig {

    private final int line;            // GUI行数 (默认27即3行)
    private final int publicSlot;      // 公共悬赏槽位 (默认11)
    private final int acceptedSlot;    // 已接取悬赏槽位 (默认13)
    private final int myPublishedSlot; // 我发布的悬赏槽位 (默认15)

    public MenuConfig(int line, int publicSlot, int acceptedSlot, int myPublishedSlot) {
        this.line = line;
        this.publicSlot = publicSlot;
        this.acceptedSlot = acceptedSlot;
        this.myPublishedSlot = myPublishedSlot;
    }

    public int getLine() { return line; }
    public int getSize() { return line * 9; }
    public int getPublicSlot() { return publicSlot; }
    public int getAcceptedSlot() { return acceptedSlot; }
    public int getMyPublishedSlot() { return myPublishedSlot; }

    public static MenuConfig defaultConfig() {
        return new MenuConfig(27, 11, 13, 15);
    }
}

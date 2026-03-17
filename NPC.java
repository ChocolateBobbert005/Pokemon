import java.awt.*;
import java.awt.image.BufferedImage;

public class NPC {
    public String name;
    public int x, y, size;
    public BufferedImage sprite;

    public NPC(String name, int x, int y, int size, BufferedImage sprite) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.size = size;
        this.sprite = sprite;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, size, size);
    }

    
}
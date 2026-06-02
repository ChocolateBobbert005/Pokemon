import java.awt.image.BufferedImage;

public class ShopkeeperNPC extends NPC {
    public ShopkeeperNPC(String name, int x, int y, BufferedImage sprite) {
        super("SHOPKEEPER", name, x, y, 64, sprite); 
    }
    
    @Override
    public String getType() {
        return "SHOPKEEPER";
    }
}
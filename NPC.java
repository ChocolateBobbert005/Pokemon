import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class NPC {
    public String name;
    public int x, y, size;
    public BufferedImage sprite;
    public ArrayList<Pokemon> party = new ArrayList<>();
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
    
    public void draw(Graphics2D g2, int playerX, int playerY) {
    // Draw the NPC sprite
    g2.drawImage(sprite, x, y, null);

    // Check if player is within 50-60 pixels
    int distX = Math.abs(x - playerX);
    int distY = Math.abs(y - playerY);

    if (distX < 64 && distY < 64) {
        g2.setColor(Color.WHITE);
        // Draw a small background box for the text to make it "pop"
        g2.setColor(new Color(0, 0, 0, 150)); // Transparent black
        g2.fillRect(x, y - 30, 80, 20);
        
        g2.setColor(Color.WHITE);
        g2.drawString("[E] Interact", x + 5, y - 15);
    }
    }
    public boolean isPlayerNear(int pX, int pY) {
        return Math.abs(x - pX) < 64 && Math.abs(y - pY) < 64;
    }
    public void drawInteractionPopup(Graphics2D g2, int playerX, int playerY) {
    if (isPlayerNear(playerX, playerY)) {
        String message = "Press E to talk to " + this.name;
        
        // 1. Set font and calculate width
        g2.setFont(new Font("Arial", Font.BOLD, 14));
        int stringWidth = g2.getFontMetrics().stringWidth(message);
        
        // 2. Draw a rounded background "bubble"
        g2.setColor(new Color(0, 0, 0, 180)); // Semi-transparent black
        g2.fillRoundRect(this.x - (stringWidth / 4), this.y - 45, stringWidth + 20, 30, 10, 10);
        
        // 3. Draw an outline for the bubble
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(this.x - (stringWidth / 4), this.y - 45, stringWidth + 20, 30, 10, 10);
        
        // 4. Draw the text inside
        g2.drawString(message, this.x - (stringWidth / 4) + 10, this.y - 25);
        
    
    }
    
    }

    private void loadParty(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                // Assuming format: Name, Level
                String pName = parts[0].trim();
                int level = Integer.parseInt(parts[1].trim());
                party.add(new Pokemon(pName, level));
            }
        } catch (Exception e) {
            System.out.println("Could not load party for " + name);
        }
    }
}
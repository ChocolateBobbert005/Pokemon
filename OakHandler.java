import java.awt.*;
import java.awt.event.KeyEvent;

public class OakHandler {
    private String[] dialogue = {
        "Hello there! Welcome to the world of Pokémon!",
        "My name is Oak! People call me the Pokémon Prof!",
        "I have three Pokémon here in these Poké Balls.",
        "Go ahead, choose one for your journey!"
    };
    
    private String[] starters = {"Bulbasaur", "Squirtle", "Charmander"};
    private int dialogueIndex = 0;
    private int starterCursor = 0;
    private boolean active = false;
    private boolean pickingStarter = false;

    // --- NEW: POST-GIFT VARIABLES ---
    private boolean postGiftMode = false;
    private String postGiftText = "";

    private int screenWidth, screenHeight;

    public OakHandler(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    // Call this for the very first meeting
    public void start() {
        active = true;
        pickingStarter = false;
        postGiftMode = false;
        dialogueIndex = 0;
    }

    // --- NEW: Call this for every meeting AFTER the first one ---
    public void startPostGiftDialogue(String text) {
        active = true;
        pickingStarter = false;
        postGiftMode = true;
        postGiftText = text;
    }

    public boolean isActive() { return active; }

    public void draw(Graphics2D g2) {
        if (!active) return;

        // If we already gave a gift, just show the simple text box
        if (postGiftMode) {
            drawDialogueBox(g2, postGiftText);
        } 
        // Otherwise, run the normal gift sequence
        else if (!pickingStarter) {
            drawDialogueBox(g2, dialogue[dialogueIndex]);
        } else {
            drawStarterMenu(g2);
        }
    }

    public Pokemon handleInput(KeyEvent e) {
        int code = e.getKeyCode();

        // Logic for simple post-gift dialogue
        if (postGiftMode) {
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_E) {
                active = false; // Just close the box
            }
            return null;
        }

        // Logic for the multi-page intro dialogue
        if (!pickingStarter) {
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_E) {
                dialogueIndex++;
                if (dialogueIndex >= dialogue.length) {
                    pickingStarter = true;
                }
            }
        } 
        // Logic for the starter selection menu
        else {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) 
                starterCursor = (starterCursor > 0) ? starterCursor - 1 : 2;
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) 
                starterCursor = (starterCursor < 2) ? starterCursor + 1 : 0;
            
            if (code == KeyEvent.VK_ENTER) {
                String choice = starters[starterCursor];
                active = false; 
                return new Pokemon(choice, 5); 
            }
        }
        return null;
    }

    private void drawDialogueBox(Graphics2D g2, String text) {
        // UI Box
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRoundRect(50, screenHeight - 160, screenWidth - 100, 120, 15, 15);
        
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(3));
        g2.drawRoundRect(50, screenHeight - 160, screenWidth - 100, 120, 15, 15);
        
        g2.setFont(new Font("Arial", Font.BOLD, 20));
        g2.drawString(text, 80, screenHeight - 110);
        
        g2.setFont(new Font("Arial", Font.ITALIC, 14));
        g2.drawString("Press ENTER to continue...", 80, screenHeight - 60);
    }

    private void drawStarterMenu(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRoundRect(screenWidth/2 - 150, 100, 300, 250, 15, 15);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(screenWidth/2 - 150, 100, 300, 250, 15, 15);

        g2.setFont(new Font("Monospaced", Font.BOLD, 22));
        g2.drawString("SELECT STARTER", screenWidth/2 - 100, 140);

        for (int i = 0; i < starters.length; i++) {
            int y = 200 + (i * 40);
            if (i == starterCursor) {
                g2.setColor(Color.YELLOW);
                g2.drawString("> " + starters[i], screenWidth/2 - 80, y);
            } else {
                g2.setColor(Color.WHITE);
                g2.drawString("  " + starters[i], screenWidth/2 - 80, y);
            }
        }
    }
}
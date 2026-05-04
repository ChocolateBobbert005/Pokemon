import java.awt.image.BufferedImage;
import java.util.List;

public class NurseNPC extends NPC {
    
    // The sequence of text she says
    private String[] dialogue = {
        "Welcome to the Pokémon Center!",
        "We can heal your Pokémon to full health.",
        "... ... ...",
        "Your Pokémon are now fighting fit!",
        "Take care!"
    };

    public NurseNPC(int x, int y, BufferedImage sprite) {
        // Calls your NPC constructor: NPC(x, y, size, sprite, name)
        super("NURSE", "Nurse Joy", x, y, 64, sprite);
    }

    // This method loops through the party and restores HP
    

    public String[] getDialogue() {
        return dialogue;
    }
}
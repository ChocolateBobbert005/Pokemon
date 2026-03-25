import java.awt.image.BufferedImage;

public class TrainerNPC extends NPC {
    private Trainer trainerData;
    private boolean isDefeated = false;
    private String challengeMessage;

    public TrainerNPC(String name, int x, int y, int size, BufferedImage sprite, String challengeMessage) {
        // 'super' sends the location and sprite up to the regular NPC class
        super(name, x, y, size, sprite);
        
        // This automatically pulls the Gym, TM, and Party from your TrainerDex
        this.trainerData = new Trainer(name);
        this.challengeMessage = challengeMessage;
    }

    public Trainer getTrainerData() { return trainerData; }
    public String getChallengeMessage() { return challengeMessage; }
    
    public boolean isDefeated() { return isDefeated; }
    public void setDefeated(boolean defeated) { this.isDefeated = defeated; }
}
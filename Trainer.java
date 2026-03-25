import java.util.ArrayList;

public class Trainer {
    private String trainerName;
    private String gymName;
    private String tmType;
    private ArrayList<Pokemon> party;

    public Trainer(String tN) {
        this.trainerName = tN;
        // Get the metadata using our helper
        this.gymName = TrainerDex.getExtraInfo(tN, 1);
        this.tmType = TrainerDex.getExtraInfo(tN, 2);
        // Get the party (starts at index 3)
        this.party = TrainerDex.getParty(tN);
    }

    public String getTrainerName() { return trainerName; }
    public String getGymName() { return gymName; }
    public String getTmType() { return tmType; }
    public ArrayList<Pokemon> getParty() { return party; }
}
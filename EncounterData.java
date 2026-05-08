public class EncounterData {
    private String name;
    private int chance; // The probability of this Pokemon appearing

    // This is the "Constructor" that was missing!
    public EncounterData(String name, int chance) {
        this.name = name;
        this.chance = chance;
    }

    public String getName() { return name; }
    public int getChance() { return chance; }
}
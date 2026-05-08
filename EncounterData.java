public class EncounterData {
    public String pokemonName; // Changed to match your loop
    public int chance;         // Must be public for direct access

    public EncounterData(String pokemonName, int chance) {
        this.pokemonName = pokemonName;
        this.chance = chance;
    }
}
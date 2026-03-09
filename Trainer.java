import java.util.ArrayList;

public class Trainer {
    private String trainerName;
    private ArrayList<Pokemon> party;


    public Trainer(String tN)
    {
        trainerName = tN;
        party = TrainerDex.getParty(tN);

        
    }
    public String getTrainerName()
    {
        return trainerName;
    }
    public Pokemon getPokemon(int n)
    {
        return party.get(n);
    }

    public void addPokemon(Pokemon p)
    {
        party.add(p);
    }
}

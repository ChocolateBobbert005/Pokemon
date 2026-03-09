import java.util.*;

public class TrainerDex 
{
    private static HashMap<String, String> trainerData = new HashMap<>();


    // public static void trainer(String trainerName) throws Exception
    // {
    //     File trainerFile = new File("H:\\APCS\\SoftwareEng\\EnemyTrainer.txt");
    //     Scanner reader = new Scanner(trainerFile);
    //     while( reader.hasNext())
    //     {
    //         String line = reader.nextLine();
    //         
    //     } 
        
    // }

    public static ArrayList<Pokemon> getParty(String name)
    {
        String trainerString = trainerData.get(name);
        String[] TSplit = trainerString.split(",");

        ArrayList<Pokemon> party = new ArrayList<Pokemon>();

        // Loop
        for(int i = 1; i < TSplit.length; i+=2)
        {
            party.add(new Pokemon(TSplit[i], Integer.parseInt(TSplit[i+1])));
        }
        
        return party;
     
        
    }
}

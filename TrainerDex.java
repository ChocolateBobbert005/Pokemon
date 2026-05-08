import java.util.*;
import java.io.*;

public class TrainerDex {
    private static HashMap<String, String> trainerData = new HashMap<>();

    public static void loadTrainerData(String filePath) {
        try {
            Scanner reader = new Scanner(new File(filePath));
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                if (line.trim().isEmpty()) continue;
                
                String name = line.split(",")[0].trim();
                trainerData.put(name, line);
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("Error loading trainers: " + e.getMessage());
        }
    }

    public static ArrayList<Pokemon> getParty(String name) {
        String raw = trainerData.get(name);
        if (raw == null) return new ArrayList<>();

        String[] TSplit = raw.split(",");
        ArrayList<Pokemon> party = new ArrayList<>();

        // Start at 3: Skip Name(0), Gym(1), and TM(2)
        for (int i = 3; i < TSplit.length; i += 2) {
            String pName = TSplit[i].trim();
            int pLevel = Integer.parseInt(TSplit[i+1].trim());
            party.add(new Pokemon(pName, pLevel));
        }
        return party;
    }

    // Helper to get the extra info
    public static String getExtraInfo(String name, int index) {
        String raw = trainerData.get(name);
        if (raw == null) return "";
        return raw.split(",")[index].trim();
    }
}
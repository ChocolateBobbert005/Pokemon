import java.util.HashMap;
import java.io.*;

public class Move {
    // This MUST be static so GamePanel can access Move.moveDatabase
    public static HashMap<String, Move> moveDatabase = new HashMap<>();
    
    private String name;
    private int basePower;

    public Move(String name, int basePower) {
        this.name = name;
        this.basePower = basePower;
    }

    public int getBasePower() {
        return basePower;
    }

    // This MUST be static so GamePanel can call Move.loadMoves()
   public static void loadMoves(String path) {
    try (BufferedReader br = new BufferedReader(new FileReader(path))) {
        String line;
        while ((line = br.readLine()) != null) {
            // Skip empty lines
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(",");
            if (parts.length >= 2) {
                try {
                    String moveName = parts[0].trim();
                    // This is where the crash was happening:
                    int power = Integer.parseInt(parts[1].trim()); 
                    
                    moveDatabase.put(moveName, new Move(moveName, power));
                } catch (NumberFormatException e) {
                    // This will skip the line if it's a header like "Name, Power"
                    // and prevent the crash.
                    System.out.println("Skipping invalid line: " + line);
                }
            }
        }
    } catch (IOException e) {
        System.err.println("Error loading moves file: " + e.getMessage());
    }
}
}

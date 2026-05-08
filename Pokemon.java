import java.io.File;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Pokemon {
    private String name, type1, type2;
    private int level, currentHP, maxHP, att, def, spA, spD, spe;
    private int currentExp;
    private int baseExpYield;

    private int[] ivs; // individual values
    private int[] evs; // effort values
    private int[] base; // base stats
    private String nature;
    
    // --- NEW: Catch Rate for the official formula ---
    private int catchRate;
    
    // Temporary list so the Battle UI has moves to display!
    private List<String> knownMoves;

    public Pokemon(String name, int level) {
        this.name = name;
        this.level = level;
        this.currentExp = level * level * level;
        this.baseExpYield = 100;
        this.knownMoves = new ArrayList<>(); // Start with a blank slate!
        initializeMoves(); // Look up starting moves from the text file

        // Give them some temporary base stats if the PokeDex isn't fully linked yet
        if (base == null) base = new int[]{45, 49, 49, 65, 65, 45}; 

        // Simplified HP Calculation (can be upgraded with your IVs/EVs later!)
        this.maxHP = ((2 * base[0] * level) / 100) + level + 10;
        this.currentHP = this.maxHP;
        
        // Simplified Attack/Defense for the battle math
        this.att = ((2 * base[1] * level) / 100) + 5;
        this.def = ((2 * base[2] * level) / 100) + 5;
        
        // --- NEW: Set default catch rate (255 is the easiest, like a Pidgey or Caterpie) ---
        this.catchRate = 255; 
    }
        // ===== LEVELING SYSTEM =====

public void gainExp(int amount) {
    this.currentExp += amount;
    System.out.println(this.name + " gained " + amount + " EXP!");

    // Calculate how much total EXP is needed for the NEXT level
    int expNeeded = (this.level + 1) * (this.level + 1) ;

    // Keep leveling up as long as we have enough EXP (in case they gain multiple levels at once)
    while (this.currentExp >= expNeeded) {
        levelUp();
        // Recalculate the threshold for the next loop
        expNeeded = (this.level + 1) * (this.level + 1);
    }
    
    // --- NEW: Print current EXP and what is needed for the next level ---
    System.out.println("EXP Progress: " + this.currentExp + " / " + expNeeded + " (Level " + (this.level + 1) + ")\n");
}

private void levelUp() {
    this.level++;
    
    // The "\n" adds a blank line before the text so it's easier to read in the console
    System.out.println("\n" + this.name + " grew to Level " + this.level + "!");

    // Save old max HP to calculate how much to heal
    int oldMaxHP = this.maxHP;

    // Recalculate stats based on the new level
    this.maxHP = ((2 * base[0] * level) / 100) + level + 10;
    this.att = ((2 * base[1] * level) / 100) + 5;
    this.def = ((2 * base[2] * level) / 100) + 5;

    // Give them the new HP they gained
    this.currentHP += (this.maxHP - oldMaxHP); 
    
    // --- NEW: Print the updated stat block ---
    System.out.println("--- New Stats ---");
    System.out.println("Max HP:  " + this.maxHP);
    System.out.println("Attack:  " + this.att);
    System.out.println("Defense: " + this.def);
    System.out.println("-----------------\n");
    checkNewMoves();
    checkEvolution();
}
    

    // ===== BATTLE METHODS =====
    public void takeDamage(int damage) {
        currentHP -= damage;
        if (currentHP < 0) currentHP = 0;
    }

    public void heal(int amount) {
        currentHP += amount;
        if (currentHP > maxHP) currentHP = maxHP;
    }

    public void setHp(int amount){
        currentHP = amount;
    }
    public boolean isFainted() {
        return currentHP <= 0;
    }
   private void checkNewMoves() {
    try {
        File file = new File("movesets.txt");
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) continue; 
            
            String[] parts = line.split(",");

            // The first part is always the Pokemon's name
            String pokeName = parts[0].trim();

            // If we found the line for THIS Pokemon, scan its moves
            if (this.name.equalsIgnoreCase(pokeName)) {
                
                // Loop through the rest of the array in pairs (i = Move, i+1 = Level)
                for (int i = 1; i < parts.length - 1; i += 2) {
                    String moveName = parts[i].trim();
                    
                    try {
                        int moveLevel = Integer.parseInt(parts[i + 1].trim());
                        
                        // If the level matches our new level, learn the move!
                        if (this.level == moveLevel) {
                            learnMove(moveName);
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Warning: Skipped a typo in movesets.txt near -> " + moveName);
                    }
                }
                // We found and processed our Pokemon, no need to read the rest of the file
                break; 
            }
        }
        scanner.close();
    } catch (Exception e) {
        System.out.println("Error finding/reading movesets.txt: " + e.getMessage());
    }
}
private void learnMove(String newMove) {
    // Only learn it if they don't already know it
    if (!knownMoves.contains(newMove)) {
        
        // If they have room, just add it
        if (knownMoves.size() < 4) {
            knownMoves.add(newMove);
            System.out.println("\n" + this.name + " learned " + newMove + "!");
        } 
        // If they already know 4 moves, forget the oldest one (the first one) to make room
        else {
            String forgottenMove = knownMoves.get(0);
            knownMoves.remove(0);
            knownMoves.add(newMove);
            System.out.println("\n" + this.name + " wants to learn " + newMove + "...");
            System.out.println(this.name + " forgot " + forgottenMove + " and learned " + newMove + "!");
        }
    }
}
private void initializeMoves() {
    try {
        File file = new File("movesets.txt");
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) continue; 
            
            String[] parts = line.split(",");
            String pokeName = parts[0].trim();

            if (this.name.equalsIgnoreCase(pokeName)) {
                
                // Loop through the move/level pairs
                for (int i = 1; i < parts.length - 1; i += 2) {
                    String moveName = parts[i].trim();
                    
                    try {
                        int moveLevel = Integer.parseInt(parts[i + 1].trim());
                        
                        // If the move's level is LESS THAN OR EQUAL to our starting level
                        if (moveLevel <= this.level) {
                            
                            // Add it silently (without printing "Learned X!" to the console)
                            if (!knownMoves.contains(moveName)) {
                                if (knownMoves.size() < 4) {
                                    knownMoves.add(moveName);
                                } else {
                                    // If they know 4 moves, forget the oldest one
                                    knownMoves.remove(0);
                                    knownMoves.add(moveName);
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Just ignore typos here
                    }
                }
                break; // Found our Pokemon, stop reading the file
            }
        }
        scanner.close();
    } catch (Exception e) {
        System.out.println("Error initializing moves: " + e.getMessage());
    }
}

private void checkEvolution() {
    try {
        File file = new File("Evolutions.txt"); // Matches your exact file name
        Scanner scanner = new Scanner(file);

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.trim().isEmpty()) continue; 
            
            String[] parts = line.split(",");
            
            // Your format has 4 parts: Base, Evolved, Type, Requirement
            if (parts.length >= 4) {
                String baseName = parts[0].trim();
                
                // If this line is for our current Pokemon
                if (this.name.equalsIgnoreCase(baseName)) {
                    
                    String evolvedName = parts[1].trim();
                    String evolutionType = parts[2].trim();
                    
                    // We only want to trigger "Level" evolutions automatically
                    if (evolutionType.equalsIgnoreCase("Level")) {
                        try {
                            int evolveLevel = Integer.parseInt(parts[3].trim());
                            
                            // If we reached or passed the level threshold, EVOLVE!
                            if (this.level >= evolveLevel) {
                                evolve(evolvedName);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Warning: Skipped a typo in Evolutions.txt near -> " + baseName);
                        }
                    }
                    // We found our Pokemon, no need to keep reading the rest of the file
                    break; 
                }
            }
        }
        scanner.close();
    } catch (Exception e) {
        System.out.println("Error finding/reading Evolutions.txt: " + e.getMessage());
    }
}

private void evolve(String newName) {
    System.out.println("\nWhat? " + this.name + " is evolving!");
    
    // Boom! The name changes!
    System.out.println("Congratulations! Your " + this.name + " evolved into " + newName + "!\n");
    this.name = newName;
    
    // NOTE: When you fully link your PokeStats database later, 
    // this is exactly where you will load the new Base Stats!
}

    // ===== GETTERS =====
    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getCurrentHp() { return currentHP; }
    public int getMaxHp() { return maxHP; }
    public int getAttack() { return att; }
    public int getDefense() { return def; }
    public String getType1() { return type1; }
    public String getType2() { return type2; }
    public List<String> getKnownMoves() {
    // 1. If the list somehow doesn't exist, make it.
    if (this.knownMoves == null) {
        this.knownMoves = new ArrayList<>();
    }
    
    // 2. If the list is completely empty, force a default move!
    if (this.knownMoves.isEmpty()) {
        System.out.println("CRITICAL WARNING: " + this.name + " loaded 0 moves! Forcing 'Tackle'.");
        this.knownMoves.add("Tackle");
    }
    
    return this.knownMoves;
}
    public int getBaseExpYield() { 
    return baseExpYield; 
}
    
    // --- NEW: Getter for the Catch Rate ---
    public int getCatchRate() { return catchRate; }
}

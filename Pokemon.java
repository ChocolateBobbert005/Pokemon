import java.util.*;

public class Pokemon {
    private String name, type1, type2;
    private int level, currentHP, maxHP, att, def, spA, spD, spe;

    private int[] ivs; // individual values
    private int[] evs; // effort values
    private int[] base; // base stats
    private String nature;
    
    // Temporary list so the Battle UI has moves to display!
    private List<String> knownMoves;

    public Pokemon(String name, int level) {
        this.name = name;
        this.level = level;
        this.knownMoves = new ArrayList<>(Arrays.asList("Tackle", "Scratch")); // Temp moves

        // INITIALIZE THE OTHER ATTRIBUTES
        // (Assuming PokeDex exists in your project to provide this)
        // PokeStats ps = PokeDex.getStats(name);
        // if (ps != null) {
        //     type1 = ps.getType1();
        //     type2 = ps.getType2();
        //     base = ps.getBase();
        // }

        // Give them some temporary base stats if the PokeDex isn't fully linked yet
        if (base == null) base = new int[]{45, 49, 49, 65, 65, 45}; 

        // Simplified HP Calculation (can be upgraded with your IVs/EVs later!)
        this.maxHP = ((2 * base[0] * level) / 100) + level + 10;
        this.currentHP = this.maxHP;
        
        // Simplified Attack/Defense for the battle math
        this.att = ((2 * base[1] * level) / 100) + 5;
        this.def = ((2 * base[2] * level) / 100) + 5;
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

    public boolean isFainted() {
        return currentHP <= 0;
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
    public List<String> getKnownMoves() { return knownMoves; }
}
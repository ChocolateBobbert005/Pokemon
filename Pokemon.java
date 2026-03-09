import java.util.*;
public class Pokemon
{
        private String name, type1, type2;
        private int level, currentHP, maxHP, att, def, spA, spD, spe;

        private int[] ivs; // induvidual values
        private int[] evs; // effort values
        private int[] base; // base stats
        private String nature;

        // MORE TO COME LATER
        
        public Pokemon(String name, int level )
        {
            this.name = name;
            this.level = level;

            // INITIALIZE THE OTHER ATTRIBUTES
            PokeStats ps = PokeDex.getStats( name );

            type1 = ps.getType1();
            type2 = ps.getType2();
            base = ps.getBase();
            
            

        }
}
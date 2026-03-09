public class PokeStats
{
    private String name, type1, type2;
    private int[] baseStats;

    public PokeStats( String n, String t1, String t2, int[] bs)
    {
        name = n;
        type1 = t1;
        type2 = t2;
        baseStats = bs;
    }
    public String getType1() { return type1; }
    public String getType2() { return type2; }
    public int[] getBase() { return baseStats; }
}
public class Item {
    private String name;
    private int buyPrice;
    private int sellPrice;
    private int quantity;

    public Item(String name, int buyPrice, int quantity) {
        this.name = name;
        this.buyPrice = buyPrice;
        this.sellPrice = buyPrice / 2; // Standard Pokémon rules: sell for half value
        this.quantity = quantity;
    }
    public boolean equals(Object o)
    {
        return ((Item)o).getName().equals(name);
    }
    public int getAmount() { return quantity; }
    public String getName() { return name; }
    public int getBuyPrice() { return buyPrice; }
    public int getSellPrice() { return sellPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }
}
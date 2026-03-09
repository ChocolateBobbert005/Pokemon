import javax.swing.JFrame;

public class Game {
    public static void main(String[] args) {

        JFrame frame = new JFrame("Pokemon");
        GamePanel panel = new GamePanel();

        frame.add(panel);
        frame.pack();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

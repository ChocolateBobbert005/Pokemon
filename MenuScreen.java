import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MenuScreen extends JPanel {

    private JFrame frame;
    private Image background;

    public MenuScreen(JFrame frame) {
        this.frame = frame;

        setLayout(new GridBagLayout());

        // Load background image
        try {
            background = new ImageIcon(
                "T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\MenuScreen.png"
            ).getImage();
        } catch (Exception e) {
            System.out.println("Could not load start screen image.");
        }

        // Load button images
        ImageIcon newGameIcon = new ImageIcon(
    "T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\NewGameButton.png"
);
        ImageIcon loadGameIcon = new ImageIcon("T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\NewGameButton.png");

        // Create buttons with images
        JButton newGameBtn = new JButton(newGameIcon);
        JButton loadGameBtn = new JButton(loadGameIcon);

        // Remove default button styling
        newGameBtn.setBorderPainted(false);
        newGameBtn.setContentAreaFilled(false);
        newGameBtn.setFocusPainted(false);

        loadGameBtn.setBorderPainted(false);
        loadGameBtn.setContentAreaFilled(false);
        loadGameBtn.setFocusPainted(false);

        // Optional: set size (matches image size)
        newGameBtn.setPreferredSize(new Dimension(
            newGameIcon.getIconWidth(),
            newGameIcon.getIconHeight()
        ));

        loadGameBtn.setPreferredSize(new Dimension(
            loadGameIcon.getIconWidth(),
            loadGameIcon.getIconHeight()
        ));

        // Layout settings
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(80, 0, 10, 0); // pushes buttons slightly down

        gbc.gridy = 0;
        add(newGameBtn, gbc);

        gbc.gridy = 1;
        add(loadGameBtn, gbc);

        // Button actions
        newGameBtn.addActionListener(e -> startNewGame());
        loadGameBtn.addActionListener(e -> loadGame());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        }
    }

    private void startNewGame() {
        frame.getContentPane().removeAll();

        GamePanel game = new GamePanel();
        frame.add(game);

        frame.revalidate();
        frame.repaint();

        game.requestFocusInWindow();
    }

    private void loadGame() {
        JOptionPane.showMessageDialog(this, "Load game not implemented yet!");
    }
}
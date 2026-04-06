import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class StartScreen extends JPanel implements KeyListener {

    private JFrame frame;
    private Image background;

    public StartScreen(JFrame frame) {
        this.frame = frame;

        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);
        addKeyListener(this);

        // Load background image
        try {
            background = new ImageIcon(
                "T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\StartScreen.png"
            ).getImage();
        } catch (Exception e) {
            System.out.println("Could not load start screen image.");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Draw background
        if (background != null) {
            g2.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        }

        // Draw text
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 28));
        g2.drawString("Press ENTER to Start", 260, 520);
    }

    @Override
   
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {

         frame.getContentPane().removeAll();

         MenuScreen menu = new MenuScreen(frame);
         frame.add(menu);

         frame.revalidate();
         frame.repaint();

         menu.requestFocusInWindow();
    }
}

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}
}
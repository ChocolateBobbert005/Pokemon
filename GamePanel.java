import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;

public class GamePanel extends JPanel implements KeyListener, ActionListener {

    ArrayList<NPC>npcList = new ArrayList<>();
    // ===== SETTINGS =====
    private final int SCREEN_WIDTH = 800;
    private final int SCREEN_HEIGHT = 600;
    private int WORLD_WIDTH;
    private int WORLD_HEIGHT;

    private BufferedImage worldMap;
    private BufferedImage collisionMap;

    private enum Direction { UP, DOWN, LEFT, RIGHT }
    private Direction facing = Direction.DOWN;

    // ===== PLAYER =====
    private int playerX, playerY;
    private final int PLAYER_SIZE = 35;
    private final int SPEED = 4;
    private int cameraX, cameraY;

    private boolean up, down, left, right;
    private BufferedImage playerUp, playerDown, playerLeft, playerRight;

    // ===== GAME LOOP =====
    private final Timer timer = new Timer(16, this);

    // ===== PORTAL SYSTEM =====
    private final HashMap<Integer, MapData> portalMap = new HashMap<>();
    private long lastTransitionTime = 0;
    private final int TRANSITION_COOLDOWN = 600; 
    private final int SCALE = 2; 

    // ===== ENCOUNTER SETTINGS =====
    private final int ENCOUNTER_COLOR = new Color(57, 148, 49).getRGB() & 0xFFFFFF;
    private final double ENCOUNTER_CHANCE = 0.09; // Chance that player encounters pokemon

    class MapData {
        String worldPath, collisionPath;
        Color spawnColor;
        MapData(String w, String c, Color s) {
            this.worldPath = w;
            this.collisionPath = c;
            this.spawnColor = s;
        }
    }

    public GamePanel() {
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        loadPlayerSprites();
        loadPortalData("H:\\APCS\\SoftwareEng\\world.txt");
        
        // Load initial map
        loadMap("T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\Lph.png",
                "T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\LphC2.png");
        
        findSpawnPoint(new Color(0, 0, 255));
        timer.start();
    }

    private void loadPlayerSprites() {
        try {
            String path = "T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\";
            playerUp = scaleSquare(ImageIO.read(new File(path + "boy_up.png")), PLAYER_SIZE);
            playerDown = scaleSquare(ImageIO.read(new File(path + "boy_down.png")), PLAYER_SIZE);
            playerLeft = scaleSquare(ImageIO.read(new File(path + "boy_left.png")), PLAYER_SIZE);
            playerRight = scaleSquare(ImageIO.read(new File(path + "boy_right.png")), PLAYER_SIZE);
        } catch (IOException e) {
            System.err.println("Sprite Load Failed: " + e.getMessage());
        }
    }

    public void loadNPCsFromFile(String filePath) {
    npcList.clear(); // Clear old NPCs before loading new ones
    
    File file = new File(filePath);
    if (!file.exists()) {
        System.err.println("NPC file not found at: " + filePath);
        return;
    }

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] data = line.split(",");
            if (data.length < 4) continue;

            String name = data[0].trim();
            // We multiply coordinates by SCALE so they land on the right spot on the map
            int worldX = Integer.parseInt(data[1].trim()) * SCALE;
            int worldY = Integer.parseInt(data[2].trim()) * SCALE;
            String spritePath = data[3].trim();

            File spriteFile = new File(spritePath);
            if (spriteFile.exists()) {
                BufferedImage rawSprite = ImageIO.read(spriteFile);
                // Scale the NPC sprite to match the PLAYER_SIZE
                BufferedImage scaledSprite = scaleSquare(rawSprite, PLAYER_SIZE);
                
                npcList.add(new NPC(name, worldX, worldY, PLAYER_SIZE, scaledSprite));
            } else {
                System.err.println("Sprite missing for NPC: " + name);
            }
        }
        System.out.println("Loaded " + npcList.size() + " NPCs.");
    } catch (Exception e) {
        System.err.println("Error loading NPCs: " + e.getMessage());
    }
}

    void loadMap(String worldPath, String collisionPath) {
        try {
            BufferedImage rawWorld = ImageIO.read(new File(worldPath));
            BufferedImage rawColl = ImageIO.read(new File(collisionPath));

            worldMap = scaleMap(rawWorld, SCALE);
            collisionMap = scaleMap(rawColl, SCALE);

            WORLD_WIDTH = worldMap.getWidth();
            WORLD_HEIGHT = worldMap.getHeight();
        } catch (IOException e) {
            System.err.println("Map Load Failed: " + e.getMessage());
        }
    }

    private void checkGrassEncounter() {
    // Only check if the player is actually pressing a movement key
    if (!(up || down || left || right)) return;

    // Get the pixel at the player's feet (center of the bottom edge)
    int checkX = playerX + PLAYER_SIZE / 2;
    int checkY = playerY + PLAYER_SIZE - 5; // 5 pixel offset from bottom

    // Stay within map bounds
    if (checkX < 0 || checkY < 0 || checkX >= WORLD_WIDTH || checkY >= WORLD_HEIGHT) return;

    // Pull the color from the collision map
    int pixelColor = collisionMap.getRGB(checkX, checkY) & 0xFFFFFF;

    if (pixelColor == ENCOUNTER_COLOR) {
        // Roll the dice (Random value between 0.0 and 1.0)
        if (Math.random() < ENCOUNTER_CHANCE) {
            triggerEncounter();
        }
    }
    }

    

    private void triggerEncounter() {
        System.out.println("A wild Pokemon appeared!");
        // BEST PRACTICE: Stop player movement immediately to transition to battle
        up = down = left = right = false;
        
        // Here is where you would transition to your Battle Scene
    }

    // Fixed: Replaced originalCollisionMap with collisionMap
    void findSpawnPoint(Color spawnColor) {
    if (collisionMap == null) {
        centerSpawnSafe();
        return;
    }

    int mapW = collisionMap.getWidth();
    int mapH = collisionMap.getHeight();
    int targetRGB = spawnColor.getRGB() & 0xFFFFFF;

    for (int y = 0; y < mapH; y++) {
        for (int x = 0; x < mapW; x++) {
            int pixel = collisionMap.getRGB(x, y) & 0xFFFFFF;

            if (pixel == targetRGB) {
                // We found the marker. Now we probe outward in 4 directions 
                // to find a 35x35 clear area.
                for (int distance = 0; distance < 100; distance += 5) {
                    // Check: Down, Up, Right, Left (in that order of priority)
                    int[][] offsets = {{0, distance}, {0, -distance}, {distance, 0}, {-distance, 0}};
                    
                    for (int[] offset : offsets) {
                        int testX = x + offset[0] - PLAYER_SIZE / 2;
                        int testY = y + offset[1] - PLAYER_SIZE / 2;

                        if (isAreaClear(testX, testY)) {
                            playerX = testX;
                            playerY = testY;
                            return; // Success!
                        }
                    }
                }
            }
        }
    }
    // If no clear spot found near the marker, find the first available clear spot on the map
    centerSpawnSafe();
    }

    

/**
 * Returns true if the 35x35 area starting at (startX, startY) 
 * contains NO black pixels (0x000000).
 */
private boolean isAreaClear(int startX, int startY) {
    // 1. Boundary Check
    if (startX < 0 || startY < 0 || 
        startX + PLAYER_SIZE >= WORLD_WIDTH || 
        startY + PLAYER_SIZE >= WORLD_HEIGHT) {
        return false;
    }

    // 2. Scan the 35x35 box for any black pixels
    for (int yy = startY; yy < startY + PLAYER_SIZE; yy++) {
        for (int xx = startX; xx < startX + PLAYER_SIZE; xx++) {
            int pixel = collisionMap.getRGB(xx, yy) & 0xFFFFFF;
            if (pixel == 0x000000) { 
                return false; // Found a wall, this spot is invalid
            }
        }
    }
    return true; // The spot is completely clear
    }

    void centerSpawnSafe() {
        playerX = WORLD_WIDTH / 2;
        playerY = WORLD_HEIGHT / 2+PLAYER_SIZE;
    }

    // Best Practice: Check corners of player bounds rather than every pixel

   // Inside update()
void update() {
    int nextX = playerX;
    int nextY = playerY;

    // Determine intended movement
    if (up)    { nextY -= SPEED; facing = Direction.UP; }
    if (down)  { nextY += SPEED; facing = Direction.DOWN; }
    if (left)  { nextX -= SPEED; facing = Direction.LEFT; }
    if (right) { nextX += SPEED; facing = Direction.RIGHT; }

    // BEST PRACTICE: Separate X and Y collision checks for "Sliding"
    // Check X movement
    if (!isColliding(nextX, playerY)) {
        playerX = nextX;
    }
    
    // Check Y movement
    if (!isColliding(playerX, nextY)) {
        playerY = nextY;
    }

    checkMapTransition();
    checkGrassEncounter();

    // Camera update (ensure camera follows the confirmed player position)
    cameraX = playerX - SCREEN_WIDTH / 2 + PLAYER_SIZE / 2;
    cameraY = playerY - SCREEN_HEIGHT / 2 + PLAYER_SIZE / 2;
    cameraX = Math.max(0, Math.min(cameraX, WORLD_WIDTH - SCREEN_WIDTH));
    cameraY = Math.max(0, Math.min(cameraY, WORLD_HEIGHT - SCREEN_HEIGHT));
    }

    // Ensure isColliding ignores the green color
    boolean isColliding(int x, int y) {
    if (x < 0 || y < 0 || x + PLAYER_SIZE > WORLD_WIDTH || y + PLAYER_SIZE > WORLD_HEIGHT) return true;

    // Check corners of the feet area
    int[] checkX = {x + 5, x + PLAYER_SIZE - 5};
    int[] checkY = {y + PLAYER_SIZE / 2, y + PLAYER_SIZE - 1};

    for (int px : checkX) {
        for (int py : checkY) {
            int pixel = collisionMap.getRGB(px, py) & 0xFFFFFF;
            // ONLY black is a wall. Green (57, 148, 49) is NOT a wall.
            if (pixel == 0x000000) return true; 
        }
    }
    return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (worldMap == null) return;

    Graphics2D g2 = (Graphics2D) g;

    // --- 1. PREP WORK (Keep this!) ---
    int screenW = getWidth();
    int screenH = getHeight();

    int offsetX = 0;
    int offsetY = 0;

    if (WORLD_WIDTH < screenW) {
        offsetX = (screenW - WORLD_WIDTH) / 2;
    }
    if (WORLD_HEIGHT < screenH) {
        offsetY = (screenH - WORLD_HEIGHT) / 2;
    }

    // --- 2. DRAW MAP (Keep this!) ---
    g2.drawImage(worldMap, offsetX - cameraX, offsetY - cameraY, null);

    // --- 3. NEW: DRAW NPCs (Add this here!) ---
    for (NPC npc : npcList) {
        // We use the EXACT same offset/camera math so they stay stuck to the map
        int npcScreenX = npc.x - cameraX + offsetX;
        int npcScreenY = npc.y - cameraY + offsetY;

        // Only draw if they are actually on the screen (saves performance)
        if (npcScreenX + npc.size > 0 && npcScreenX < screenW && 
            npcScreenY + npc.size > 0 && npcScreenY < screenH) {
            
            g2.drawImage(npc.sprite, npcScreenX, npcScreenY, null);
            
            // Optional: Draw their name tag
            g2.setColor(Color.WHITE);
            g2.drawString(npc.name, npcScreenX, npcScreenY - 5);
        }
    }

    // --- 4. DRAW PLAYER (Keep this!) ---
    BufferedImage sprite = switch (facing) {
        case UP -> playerUp;
        case DOWN -> playerDown;
        case LEFT -> playerLeft;
        case RIGHT -> playerRight;
    };

    if (sprite != null) {
        g2.drawImage(sprite, playerX - cameraX + offsetX, playerY - cameraY + offsetY, null);
    }
    }

    // Helper scaling methods (kept your logic)
    private BufferedImage scaleSquare(BufferedImage img, int size) {
        BufferedImage scaled = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(img, 0, 0, size, size, null);
        g2.dispose();
        return scaled;
    }

    private BufferedImage scaleMap(BufferedImage img, int scale) {
        int w = img.getWidth() * scale;
        int h = img.getHeight() * scale;
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        g2.drawImage(img, 0, 0, w, h, null);
        g2.dispose();
        return scaled;
    }

    // Ported from your snippet with minor error handling
    void loadPortalData(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("=");
                String[] rgb = parts[0].trim().split(",");
                String[] vals = parts[1].trim().split(",");
                Color pColor = new Color(Integer.parseInt(rgb[0].trim()), Integer.parseInt(rgb[1].trim()), Integer.parseInt(rgb[2].trim()));
                Color sColor = new Color(Integer.parseInt(vals[2].trim()), Integer.parseInt(vals[3].trim()), Integer.parseInt(vals[4].trim()));
                portalMap.put(pColor.getRGB(), new MapData(vals[0].trim(), vals[1].trim(), sColor));
            }
        } catch (Exception e) { System.err.println("Portal Data Error"); }
    }

    void checkMapTransition() {
        long now = System.currentTimeMillis();
        if (now - lastTransitionTime < TRANSITION_COOLDOWN) return;

        int cx = playerX + PLAYER_SIZE / 2;
        int cy = playerY + PLAYER_SIZE / 2;
        if (cx < 0 || cy < 0 || cx >= WORLD_WIDTH || cy >= WORLD_HEIGHT) return;

        int key = collisionMap.getRGB(cx, cy) & 0xFFFFFF;
        if (portalMap.containsKey(key | 0xFF000000)) { // Ensure alpha matches
            MapData data = portalMap.get(key | 0xFF000000);
            loadMap(data.worldPath, data.collisionPath);
            findSpawnPoint(data.spawnColor);
            lastTransitionTime = now;
        }
    }

    @Override public void actionPerformed(ActionEvent e) { update(); repaint(); }
    @Override public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_W) up = true;
        if(e.getKeyCode() == KeyEvent.VK_S) down = true;
        if(e.getKeyCode() == KeyEvent.VK_A) left = true;
        if(e.getKeyCode() == KeyEvent.VK_D) right = true;
    }
    @Override public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_W) up = false;
        if(e.getKeyCode() == KeyEvent.VK_S) down = false;
        if(e.getKeyCode() == KeyEvent.VK_A) left = false;
        if(e.getKeyCode() == KeyEvent.VK_D) right = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.HashMap;
import java.util.ArrayList;

public class GamePanel extends JPanel implements KeyListener, ActionListener {

    ArrayList<NPC> npcList = new ArrayList<>();
    
    // ===== SETTINGS =====
    private final int SCREEN_WIDTH = 800;
    private final int SCREEN_HEIGHT = 600;
    private int WORLD_WIDTH;
    private int WORLD_HEIGHT;

    private BufferedImage worldMap;
    private BufferedImage collisionMap;

    private enum Direction { UP, DOWN, LEFT, RIGHT }
    private Direction facing = Direction.DOWN;

    // ===== GAME STATES =====
    private enum GameState { OVERWORLD, BATTLE }
    private GameState currentState = GameState.OVERWORLD;

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
    private final double ENCOUNTER_CHANCE = 0.09; 
    
    // ===== BATTLE VARIABLES =====
    private final String SPRITE_PATH = "H:\\My programing workspace\\Pokemon Sprites\\";
    
    private ArrayList<Pokemon> playerParty = new ArrayList<>();
    private Pokemon myPokemon; 
    
    private Pokemon currentEnemy;
    private BufferedImage playerPokemonImg;
    private BufferedImage enemyPokemonImg;
    
    private enum BattleMenu { START_MESSAGE, MAIN, FIGHT, PLAYER_MESSAGE, ENEMY_MESSAGE, END_MESSAGE, CATCH_THROWN, CATCH_FAILED, POKEMON_MENU, SWAP_MESSAGE }
    private BattleMenu currentBattleMenu = BattleMenu.MAIN;
    private int menuCursor = 0; 
    private int partyCursor = 0; 
    private String battleMessage = ""; 

    // Font(Name, Style, Size)
    private final Font NPC_FONT = new Font("Dialog", Font.BOLD, 14);
    private final Color NPC_NAME_COLOR = Color.GRAY; // Or new Color(255, 255, 0)
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
        loadPortalData("C:\\Users\\Lemkcar\\Documents\\GitCode\\Pokemon\\world.txt");
        
        loadMap("T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\Lph.png",
                "T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\LphC2.png");
        
        findSpawnPoint(new Color(0, 0, 255));
        
        myPokemon = new Pokemon("Charmander", 5);
        playerParty.add(myPokemon);
        
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

    private BufferedImage loadPokemonImage(String pokemonName) {
        String fileName = pokemonName.toLowerCase() + ".jpg";
        String fullPath = SPRITE_PATH + fileName; 
        try {
            BufferedImage img = ImageIO.read(new File(fullPath));
            return scaleSquare(img, 150); 
        } catch (IOException e) {
            System.err.println("Could not find image for " + pokemonName + " at: " + fullPath);
            return null; 
        }
    }

    public void loadNPCsFromFile(String filePath) {
        npcList.clear(); 
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] data = line.split(",");
                if (data.length < 4) continue;

                String name = data[0].trim();
                int worldX = Integer.parseInt(data[1].trim()) * SCALE;
                int worldY = Integer.parseInt(data[2].trim()) * SCALE;
                String spritePath = data[3].trim();

                File spriteFile = new File(spritePath);
                if (spriteFile.exists()) {
                    BufferedImage rawSprite = ImageIO.read(spriteFile);
                    BufferedImage scaledSprite = scaleSquare(rawSprite, PLAYER_SIZE);
                    npcList.add(new NPC(name, worldX, worldY, PLAYER_SIZE, scaledSprite));
                }
            }
        } catch (Exception e) { System.err.println("Error loading NPCs: " + e.getMessage()); }
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
        refreshNPCs(worldPath);
    }

    private void checkGrassEncounter() {
        if (!(up || down || left || right)) return;

        int checkX = playerX + PLAYER_SIZE / 2;
        int checkY = playerY + PLAYER_SIZE - 5; 

        if (checkX < 0 || checkY < 0 || checkX >= WORLD_WIDTH || checkY >= WORLD_HEIGHT) return;

        int pixelColor = collisionMap.getRGB(checkX, checkY) & 0xFFFFFF;

        if (pixelColor == ENCOUNTER_COLOR) {
            if (Math.random() < ENCOUNTER_CHANCE) {
                triggerEncounter();
            }
        }
    }

    private void triggerEncounter() {
        up = down = left = right = false; 
        
        if (myPokemon.isFainted()) myPokemon.heal(myPokemon.getMaxHp());

        String[] possible = {"Pidgey", "Rattata", "Caterpie"};
        String enemyName = possible[(int)(Math.random() * possible.length)];
        
        // --- NEW: EASTER EGG ---
        if (enemyName.equals("Caterpie")) {
            enemyName = "CaterPIE";
        }
        
        currentEnemy = new Pokemon(enemyName, 3 + (int)(Math.random()*3));
        
        playerPokemonImg = loadPokemonImage(myPokemon.getName());
        enemyPokemonImg = loadPokemonImage(currentEnemy.getName());
        
        battleMessage = "A wild " + currentEnemy.getName() + " appeared!";
        currentBattleMenu = BattleMenu.START_MESSAGE;
        menuCursor = 0;
        currentState = GameState.BATTLE; 
    }
    
    private void endBattle() {
        currentState = GameState.OVERWORLD; 
        currentEnemy = null;
    }
    
    private int calculateDamage(Pokemon attacker, Pokemon defender) {
        int power = 40; 
        double damage = (((2.0 * attacker.getLevel() / 5.0) + 2.0) * power * ((double)attacker.getAttack() / defender.getDefense())) / 50.0 + 2.0;
        return (int) Math.max(1, damage);
    }
    
    private void enemyAttack() {
        int damage = calculateDamage(currentEnemy, myPokemon);
        myPokemon.takeDamage(damage);
        battleMessage = "Enemy " + currentEnemy.getName() + " attacked!";
        currentBattleMenu = BattleMenu.ENEMY_MESSAGE;
    }

    void findSpawnPoint(Color spawnColor) {
        if (collisionMap == null) { centerSpawnSafe(); return; }
        int mapW = collisionMap.getWidth();
        int mapH = collisionMap.getHeight();
        int targetRGB = spawnColor.getRGB() & 0xFFFFFF;

        for (int y = 0; y < mapH; y++) {
            for (int x = 0; x < mapW; x++) {
                int pixel = collisionMap.getRGB(x, y) & 0xFFFFFF;
                if (pixel == targetRGB) {
                    for (int distance = 0; distance < 100; distance += 5) {
                        int[][] offsets = {{0, distance}, {0, -distance}, {distance, 0}, {-distance, 0}};
                        for (int[] offset : offsets) {
                            int testX = x + offset[0] - PLAYER_SIZE / 2;
                            int testY = y + offset[1] - PLAYER_SIZE / 2;
                            if (isAreaClear(testX, testY)) {
                                playerX = testX; playerY = testY; return; 
                            }
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
 * REFRESH NPCS
 * Call this single method whenever you want to update the room's occupants.
 */
public void refreshNPCs(String currentMapPath) {
    npcList.clear(); // Clear the stage
    parseMasterNPCFile("npcs.txt", currentMapPath);
}

/**
 * MASTER FILE PARSER
 * Isolated logic to scan the big file and filter by map name.
 */
private void parseMasterNPCFile(String path, String filterMap) {
    File file = new File(path);
    if (!file.exists()) return;

    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] data = line.split(",");
            if (data.length < 5) continue;

            String mapInFile = data[0].trim();
            
            // Only proceed if the NPC lives in the map we just entered
            if (mapInFile.equalsIgnoreCase(filterMap)) {
                String name = data[1].trim();
                int x = Integer.parseInt(data[2].trim()) * SCALE;
                int y = Integer.parseInt(data[3].trim()) * SCALE;
                String spritePath = data[4].trim();

                BufferedImage sprite = loadAndScaleNPCSprite(spritePath);
                if (sprite != null) {
                    npcList.add(new NPC(name, x, y, PLAYER_SIZE, sprite));
                }
            }
        }
    } catch (Exception e) {
        System.err.println("NPC System Error: " + e.getMessage());
    }
}

/**
 * INDEPENDENT SCALER
 * Scales NPC sprites without touching your map scaling methods.
 */
private BufferedImage loadAndScaleNPCSprite(String path) {
    try {
        BufferedImage img = ImageIO.read(new File(path));
        BufferedImage scaled = new BufferedImage(PLAYER_SIZE, PLAYER_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics();
        
        // Use Nearest Neighbor to keep the pixel art sharp
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g2.drawImage(img, 0, 0, PLAYER_SIZE, PLAYER_SIZE, null);
        g2.dispose();
        
        return scaled;
    } catch (Exception e) {
        return null; 
    }
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

    private boolean isAreaClear(int startX, int startY) {
        if (startX < 0 || startY < 0 || startX + PLAYER_SIZE >= WORLD_WIDTH || startY + PLAYER_SIZE >= WORLD_HEIGHT) return false;
        for (int yy = startY; yy < startY + PLAYER_SIZE; yy++) {
            for (int xx = startX; xx < startX + PLAYER_SIZE; xx++) {
                int pixel = collisionMap.getRGB(xx, yy) & 0xFFFFFF;
                if (pixel == 0x000000) return false; 
            }
        }
        return true; 
    }

    void centerSpawnSafe() { playerX = WORLD_WIDTH / 2; playerY = WORLD_HEIGHT / 2+PLAYER_SIZE; }

    void update() {
        if (currentState == GameState.BATTLE) return; 

   // Inside update()
    void update() {
    int nextX = playerX;
    int nextY = playerY;

        if (up)    { nextY -= SPEED; facing = Direction.UP; }
        if (down)  { nextY += SPEED; facing = Direction.DOWN; }
        if (left)  { nextX -= SPEED; facing = Direction.LEFT; }
        if (right) { nextX += SPEED; facing = Direction.RIGHT; }

        if (!isColliding(nextX, playerY)) playerX = nextX;
        if (!isColliding(playerX, nextY)) playerY = nextY;

        checkMapTransition();
        checkGrassEncounter();

        cameraX = playerX - SCREEN_WIDTH / 2 + PLAYER_SIZE / 2;
        cameraY = playerY - SCREEN_HEIGHT / 2 + PLAYER_SIZE / 2;
        cameraX = Math.max(0, Math.min(cameraX, WORLD_WIDTH - SCREEN_WIDTH));
        cameraY = Math.max(0, Math.min(cameraY, WORLD_HEIGHT - SCREEN_HEIGHT));
    }

    // Ensure isColliding ignores the green color
    boolean isColliding(int nextX, int nextY) {
    // 1. Map Boundary Check (Prevents "Out of Bounds" errors)
    if (nextX < 0 || nextY < 0 || 
        nextX + PLAYER_SIZE > WORLD_WIDTH || 
        nextY + PLAYER_SIZE > WORLD_HEIGHT) return true;

    // 2. NPC Collision Check
    // We create a hitbox for the player's FEET only (bottom half)
    Rectangle playerFeet = new Rectangle(nextX + 4, nextY + (PLAYER_SIZE / 2), PLAYER_SIZE - 8, PLAYER_SIZE / 2);

    for (NPC npc : npcList) {
        Rectangle npcFeet = new Rectangle(npc.x + 4, npc.y + (npc.size / 2), npc.size - 8, npc.size / 2);
        if (playerFeet.intersects(npcFeet)) {
            return true; 
        }
    }

    // 3. COLLISION MAP CHECK (The Black Pixels)
    // We check the corners of the player's "Feet" area on the collision map
    int footLeft = nextX + 6;
    int footRight = nextX + PLAYER_SIZE - 6;
    int footTop = nextY + (PLAYER_SIZE / 2) + 4;
    int footBottom = nextY + PLAYER_SIZE - 2;

    // Check 4 points around the feet to make sure the whole base is clear
    int[] checkX = {footLeft, footRight};
    int[] checkY = {footTop, footBottom};

    for (int x : checkX) {
        for (int y : checkY) {
            // Safety check: make sure coordinates are inside the map image
            if (x >= 0 && x < collisionMap.getWidth() && y >= 0 && y < collisionMap.getHeight()) {
                
                // Get the color and strip the Alpha (transparency)
                int pixelColor = collisionMap.getRGB(x, y) & 0xFFFFFF;

                // 0x000000 is the Hex code for pure black
                if (pixelColor == 0x000000) {
                    return true; // Wall hit!
                }
            }
        }
    }

    return false; // No walls or NPCs hit!
}


    public void drawShadow(Graphics2D g2, int screenX, int screenY) {
    g2.setColor(new Color(0, 0, 0, 60)); // Transparent black
    // Draw an oval at the NPC's feet
    g2.fillOval(screenX + 5, screenY + PLAYER_SIZE - 12, PLAYER_SIZE - 10, 10);
    }
    @Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (worldMap == null) return;

    Graphics2D g2 = (Graphics2D) g;

    // --- 1. PREP WORK ---
    int screenW = getWidth();
    int screenH = getHeight();
    int offsetX = (WORLD_WIDTH < screenW) ? (screenW - WORLD_WIDTH) / 2 : 0;
    int offsetY = (WORLD_HEIGHT < screenH) ? (screenH - WORLD_HEIGHT) / 2 : 0;

    // --- 2. DRAW MAP ---
    g2.drawImage(worldMap, offsetX - cameraX, offsetY - cameraY, null);

    // --- 3. DRAW NPCs & PLAYER (Y-Sorted) ---
    
    // This line sorts NPCs from top to bottom before drawing
    npcList.sort((a, b) -> Integer.compare(a.y, b.y));
    
    boolean playerDrawn = false;

    for (NPC npc : npcList) {
        int npcScreenX = npc.x - cameraX + offsetX;
        int npcScreenY = npc.y - cameraY + offsetY;

        // DEPTH CHECK: If player is "further up" the map than this NPC, draw player first
        if (!playerDrawn && (playerY + PLAYER_SIZE) < (npc.y + npc.size)) {
            drawPlayer(g2, offsetX, offsetY);
            playerDrawn = true;
        }

        // Draw NPC if on screen
        if (npcScreenX + npc.size > 0 && npcScreenX < screenW && 
            npcScreenY + npc.size > 0 && npcScreenY < screenH) {
            
        } else if (currentState == GameState.BATTLE) {
            g.setColor(Color.WHITE); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
            
            // Name Tag Logic (Outlines & Centering)
            g2.setFont(NPC_FONT);
            FontMetrics metrics = g2.getFontMetrics(NPC_FONT);
            int nameWidth = metrics.stringWidth(npc.name);
            int centeredNameX = npcScreenX + (npc.size / 2) - (nameWidth / 2);
            int nameY = npcScreenY - 8;

            // Black Outline
            g2.setColor(Color.BLACK);
            g2.drawString(npc.name, centeredNameX + 1, nameY + 1);
            g2.drawString(npc.name, centeredNameX - 1, nameY - 1);
            g2.drawString(npc.name, centeredNameX + 1, nameY - 1);
            g2.drawString(npc.name, centeredNameX - 1, nameY + 1);

            // Main Color
            g2.setColor(NPC_NAME_COLOR);
            g2.drawString(npc.name, centeredNameX, nameY);
        }
    }

    // SAFETY: If the player is the "lowest" thing on the map, draw them last
    if (!playerDrawn) {
        drawPlayer(g2, offsetX, offsetY);
    }
}

// Helper method so we don't have to repeat the switch statement
private void drawPlayer(Graphics2D g2, int offsetX, int offsetY) {
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
        g2.dispose(); return scaled;
    }

    private BufferedImage scaleMap(BufferedImage img, int scale) {
        int w = img.getWidth() * scale; int h = img.getHeight() * scale;
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = scaled.createGraphics(); g2.drawImage(img, 0, 0, w, h, null);
        g2.dispose(); return scaled;
    }

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

        int cx = playerX + PLAYER_SIZE / 2; int cy = playerY + PLAYER_SIZE / 2;
        if (cx < 0 || cy < 0 || cx >= WORLD_WIDTH || cy >= WORLD_HEIGHT) return;

        int key = collisionMap.getRGB(cx, cy) & 0xFFFFFF;
        if (portalMap.containsKey(key | 0xFF000000)) { 
            MapData data = portalMap.get(key | 0xFF000000);
            loadMap(data.worldPath, data.collisionPath);
            findSpawnPoint(data.spawnColor);
            lastTransitionTime = now;
        }
    }

    @Override public void actionPerformed(ActionEvent e) { update(); repaint(); }
    
    @Override public void keyPressed(KeyEvent e) {
        if (currentState == GameState.OVERWORLD) {
            if(e.getKeyCode() == KeyEvent.VK_W) up = true;
            if(e.getKeyCode() == KeyEvent.VK_S) down = true;
            if(e.getKeyCode() == KeyEvent.VK_A) left = true;
            if(e.getKeyCode() == KeyEvent.VK_D) right = true;
        } 
        else if (currentState == GameState.BATTLE) {
            
            if (currentBattleMenu == BattleMenu.MAIN || currentBattleMenu == BattleMenu.FIGHT) {
                if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) { if (menuCursor >= 2) menuCursor -= 2; }
                if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) { if (menuCursor <= 1) menuCursor += 2; }
                if (e.getKeyCode() == KeyEvent.VK_A || e.getKeyCode() == KeyEvent.VK_LEFT) { if (menuCursor % 2 != 0) menuCursor -= 1; }
                if (e.getKeyCode() == KeyEvent.VK_D || e.getKeyCode() == KeyEvent.VK_RIGHT) { if (menuCursor % 2 == 0) menuCursor += 1; }
            }
            else if (currentBattleMenu == BattleMenu.POKEMON_MENU) {
                if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) { 
                    if (partyCursor > 0) partyCursor--; 
                }
                if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) { 
                    if (partyCursor < playerParty.size() - 1) partyCursor++; 
                }
            }

            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (currentBattleMenu == BattleMenu.START_MESSAGE) {
                    currentBattleMenu = BattleMenu.MAIN;
                }
                else if (currentBattleMenu == BattleMenu.MAIN) {
                    if (menuCursor == 0) { 
                        currentBattleMenu = BattleMenu.FIGHT; menuCursor = 0; 
                    } 
                    else if (menuCursor == 1) { 
                        battleMessage = "You threw a Pokéball!";
                        currentBattleMenu = BattleMenu.CATCH_THROWN;
                    }
                    else if (menuCursor == 2) {
                        currentBattleMenu = BattleMenu.POKEMON_MENU;
                        partyCursor = 0;
                    }
                    else if (menuCursor == 3) endBattle(); // RUN
                } 
                else if (currentBattleMenu == BattleMenu.POKEMON_MENU) {
                    Pokemon selected = playerParty.get(partyCursor);
                    
                    if (selected == myPokemon) {
                        // Already active
                    } else if (selected.isFainted()) {
                        // Fainted
                    } else {
                        myPokemon = selected;
                        playerPokemonImg = loadPokemonImage(myPokemon.getName()); 
                        battleMessage = "Go! " + myPokemon.getName() + "!";
                        currentBattleMenu = BattleMenu.SWAP_MESSAGE; 
                    }
                }
                else if (currentBattleMenu == BattleMenu.SWAP_MESSAGE) {
                    enemyAttack(); 
                }
                else if (currentBattleMenu == BattleMenu.FIGHT) {
                    if (menuCursor < myPokemon.getKnownMoves().size()) {
                        String moveName = myPokemon.getKnownMoves().get(menuCursor);
                        int damage = calculateDamage(myPokemon, currentEnemy);
                        currentEnemy.takeDamage(damage);
                        battleMessage = myPokemon.getName() + " used " + moveName + "!";
                        currentBattleMenu = BattleMenu.PLAYER_MESSAGE; 
                    }
                } 
                else if (currentBattleMenu == BattleMenu.PLAYER_MESSAGE) {
                    if (currentEnemy.isFainted()) {
                        battleMessage = "Enemy " + currentEnemy.getName() + " fainted!";
                        currentBattleMenu = BattleMenu.END_MESSAGE;
                    } else enemyAttack();
                }
                else if (currentBattleMenu == BattleMenu.CATCH_THROWN) {
                    double hpPercent = (double) currentEnemy.getCurrentHp() / currentEnemy.getMaxHp();
                    double catchChance = 0.7 - (hpPercent * 0.5); 
                    
                    if (Math.random() < catchChance) {
                        battleMessage = "Gotcha! " + currentEnemy.getName() + " was caught!";
                        if (playerParty.size() < 6) {
                            playerParty.add(currentEnemy);
                        } else {
                            System.out.println(currentEnemy.getName() + " was sent to the PC! (Not yet implemented)");
                        }
                        currentBattleMenu = BattleMenu.END_MESSAGE;
                    } else {
                        battleMessage = "Oh no! It broke free!";
                        currentBattleMenu = BattleMenu.CATCH_FAILED;
                    }
                }
                else if (currentBattleMenu == BattleMenu.CATCH_FAILED) {
                    enemyAttack(); 
                }
                else if (currentBattleMenu == BattleMenu.ENEMY_MESSAGE) {
                    if (myPokemon.isFainted()) {
                        battleMessage = myPokemon.getName() + " fainted!";
                        currentBattleMenu = BattleMenu.END_MESSAGE;
                    } else { 
                        currentBattleMenu = BattleMenu.MAIN; menuCursor = 0; 
                    }
                }
                else if (currentBattleMenu == BattleMenu.END_MESSAGE) {
                    if (myPokemon.isFainted()) { 
                        for (Pokemon p : playerParty) p.heal(p.getMaxHp()); 
                        centerSpawnSafe(); 
                    }
                    endBattle();
                }
            }
            
            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                if (currentBattleMenu == BattleMenu.FIGHT || currentBattleMenu == BattleMenu.POKEMON_MENU) {
                    currentBattleMenu = BattleMenu.MAIN; menuCursor = 0;
                }
            }
        }
    }
    
    @Override public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_W) up = false;
        if(e.getKeyCode() == KeyEvent.VK_S) down = false;
        if(e.getKeyCode() == KeyEvent.VK_A) left = false;
        if(e.getKeyCode() == KeyEvent.VK_D) right = false;
    }
    
    @Override public void keyTyped(KeyEvent e) {}
}
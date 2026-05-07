import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.HashMap;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class GamePanel extends JPanel implements KeyListener, ActionListener {

    ArrayList<NPC> npcList = new ArrayList<>();
    
    // ===== SETTINGS =====
    private final int SCREEN_WIDTH = 800;
    private final int SCREEN_HEIGHT = 600;
    private int WORLD_WIDTH;
    private int WORLD_HEIGHT;

    private BufferedImage worldMap;
    private BufferedImage collisionMap;

    private final String[] pauseOptions = {"Resume", "Pokemon", "Bag", "Save", "Load", "Exit"};
    private int pauseCursor = 0;

    private enum Direction { UP, DOWN, LEFT, RIGHT }
    private Direction facing = Direction.DOWN;

    // ===== GAME STATES =====
    private enum GameState { OVERWORLD, BATTLE, PAUSE }
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
    private final double ENCOUNTER_CHANCE = 0.05; 
    
    // ===== BATTLE VARIABLES =====
    private final String SPRITE_PATH = "T:\\HS\\Student\\Computer Science\\Software Engineering\\Pokemon Sprites\\";
    
    private ArrayList<Pokemon> playerParty = new ArrayList<>();
    private Pokemon myPokemon; 
    
    
    private Pokemon currentEnemy;
    private BufferedImage playerPokemonImg;
    private BufferedImage enemyPokemonImg;
    
    private enum BattleMenu { START_MESSAGE, MAIN, FIGHT, BAG_MENU,  PLAYER_MESSAGE, ENEMY_MESSAGE, END_MESSAGE, CATCH_THROWN, CATCH_FAILED, POKEMON_MENU, SWAP_MESSAGE }
    private BattleMenu currentBattleMenu = BattleMenu.MAIN;
    private int menuCursor = 0; 
    private int partyCursor = 0; 
    private String battleMessage = ""; 
    private List<Pokemon> currentTrainerParty;
    public String currentMapPath = "T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\PalletTown.png";
    public String currentCollisionPath = "T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\PalletTownCollision.png";
    private HashMap<String, ArrayList<EncounterData>> routeEncounters = new HashMap<>();
    public String currentRouteName = "Pallet_Town";
    
    
    

    // Font(Name, Style, Size)
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
        loadEncounters();
    

        loadPlayerSprites();
        loadPortalData("C:\\Users\\WainBra\\Documents\\GitCode\\Pokemon\\world.txt");
        
        loadMap("T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\Lph.png",
                "T:\\HS\\Student\\Computer Science\\Software Engineering\\TeamSeniorSlackers\\LphC2.png");
        
        findSpawnPoint(new Color(0, 0, 255));
        
        myPokemon = new Pokemon("Charmander", 5);
        playerParty.add(myPokemon);
        // --- ADD THESE LINES TO FIX SWITCHING ---
        playerParty.add(new Pokemon("Squirtle", 5));
        playerParty.add(new Pokemon("Bulbasaur", 15));
        
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

    public void loadNPCsForMap(String currentMap) {
    // 1. Clear the old NPCs so they don't follow you to the new map!
    npcList.clear();

    try {
        Scanner reader = new Scanner(new File("C:\\Users\\Lemkcar\\Documents\\GitCode\\Pokemon\\NPC.java"));
        while (reader.hasNextLine()) {
            String line = reader.nextLine();
            if (line.trim().isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split(",");
            String mapID = parts[0].trim();

            // 2. Only load the NPC if the Map ID matches where the player is
            if (mapID.equalsIgnoreCase(currentMap)) {
                String type = parts[1].trim();
                String name = parts[2].trim();
                String spritePath = parts[3].trim();
                int x = Integer.parseInt(parts[4].trim());
                int y = Integer.parseInt(parts[5].trim());
                String message = parts[6].trim();

                BufferedImage sprite = ImageIO.read(new File(spritePath));

                if (type.equalsIgnoreCase("TRAINER")) {
                    npcList.add(new TrainerNPC(name, x, y, 64, sprite, message));
                } else {
                    npcList.add(new NPC(name, x, y, 64, sprite));
                    // Note: You might want to add a 'message' field to your NPC class
                }
            }
        }
        reader.close();
    } catch (IOException e) {
        System.out.println("Error loading NPCs for map " + currentMap + ": " + e.getMessage());
    }
    }

    void loadMap(String worldPath, String collisionPath) {
        this.currentMapPath = worldPath;
        this.currentCollisionPath = collisionPath;
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
        determineRouteFromMap(worldPath);
    
    System.out.println("MAP LOADED: " + worldPath + " | ROUTE UPDATED TO: " + currentRouteName);
        
    }

  public void checkGrassEncounters() {
    // 1. Calculate player's center point for accurate tile detection
    int cx = playerX + PLAYER_SIZE / 2; 
    int cy = playerY + PLAYER_SIZE / 2;

    // 2. Boundary Safety: Ensure we aren't checking outside the image pixels
    if (cx >= 0 && cy >= 0 && cx < WORLD_WIDTH && cy < WORLD_HEIGHT) {
        
        // 3. Get the hex color from the collision map (ignoring Alpha)
        int currentTileColor = collisionMap.getRGB(cx, cy) & 0xFFFFFF;
        
        // This matches the dark green color we identified earlier
        int GRASS_COLOR = 0x399431; 

        // 4. Trigger logic: Only roll if we are ON the grass AND MOVING
        if (currentTileColor == GRASS_COLOR && (up || down || left || right)) {
            System.out.println("LOG: Player is currently IN grass and moving...");
            
            /* * ENCOUNTER RATE: 0.004 (0.4%)
             * At 60 FPS, this rolls 60 times a second. 
             * This math results in roughly one encounter every 4-5 seconds of walking.
             */
            if (Math.random() < 0.02) { 
                
                // 5. Route Safety: Ensure the map actually has a valid encounter table
                if (currentRouteName != null && !currentRouteName.equalsIgnoreCase("none")) {
                    
                    // 6. Roll the dice against your encounters.txt data
                    String wildMon = rollWildEncounter(currentRouteName);
                    
                    if (wildMon != null) {
                        // 7. Success! Pull the player into the battle screen
                        triggerWildBattle(wildMon);
                    }
                }
            }
        }
    }
}

   
    
    private void endBattle() {
        currentState = GameState.OVERWORLD; 
        currentEnemy = null;
    }
    
    private int calculateDamage(Pokemon attacker, Pokemon defender, Move move) {
    // If the move exists in the database, use its power. Otherwise, default to 40 (like Struggle).
    int power = (move != null) ? move.getBasePower() : 40; 
    
    double damage = (((2.0 * attacker.getLevel() / 5.0) + 2.0) * power * ((double)attacker.getAttack() / defender.getDefense())) / 50.0 + 2.0;
    
    return (int) Math.max(1, damage); // Always do at least 1 damage
    }
    private void enemyAttack() {
    // 1. Safely pick a random move from the enemy's known moves
    java.util.List<String> moves = currentEnemy.getKnownMoves();
    String moveName = "Tackle"; // Guaranteed fallback move
    
    // Only attempt to pull from the list if it actually has moves inside
    if (moves != null && !moves.isEmpty()) {
        int randomIndex = (int)(Math.random() * moves.size());
        moveName = moves.get(randomIndex);
    } else {
        System.out.println("WARNING: " + currentEnemy.getName() + " has an empty move list! Forcing Tackle.");
    }
    
    // 2. Look up the move in the database
    Move selectedMove = Move.moveDatabase.get(moveName);
    
    // 3. Calculate and apply damage (with an extra safety net!)
    if (selectedMove != null) {
        // The move exists in the database, calculate normally
        int damage = calculateDamage(currentEnemy, myPokemon, selectedMove);
        myPokemon.takeDamage(damage);
    } else {
        // The move is missing from the database! Apply flat damage so the game doesn't crash
        System.out.println("WARNING: '" + moveName + "' not found in moveDatabase! Applying 5 default damage.");
        myPokemon.takeDamage(5);
    }
    
    // 4. Update the UI
    battleMessage = "Enemy " + currentEnemy.getName() + " used " + moveName + "!";
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
                String name = data[2].trim();
                int x = Integer.parseInt(data[4].trim()) * SCALE;
                int y = Integer.parseInt(data[5].trim()) * SCALE;
                String spritePath = data[3].trim();

                BufferedImage sprite = loadAndScaleNPCSprite(spritePath);
                if (sprite != null) {
                    npcList.add(new NPC(name, x, y, PLAYER_SIZE, sprite));
                }
            }
        }
    } catch (Exception e) {
        System.err.println("NPC System Error: " + e.getMessage());
        e.printStackTrace();
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
        if (startX < 0 || startY < 0 || startX + PLAYER_SIZE >= WORLD_WIDTH || startY + PLAYER_SIZE >= WORLD_HEIGHT) return false;
        for (int yy = startY; yy < startY + PLAYER_SIZE; yy++) {
            for (int xx = startX; xx < startX + PLAYER_SIZE; xx++) {
                int pixel = collisionMap.getRGB(xx, yy) & 0xFFFFFF;
                if (pixel == 0x000000) return false; 
            }
        }
        return true; 
    }

    public boolean isNear(NPC npc) {
    // 1. Find the center of the player
    int px = playerX + PLAYER_SIZE / 2;
    int py = playerY + PLAYER_SIZE / 2;

    // 2. Find the center of the NPC
    int nx = npc.x + npc.size / 2;
    int ny = npc.y + npc.size / 2;

    // 3. Calculate the distance between those two points
    double distance = Math.sqrt(Math.pow(px - nx, 2) + Math.pow(py - ny, 2));

    // 4. Return true if the distance is less than a certain threshold
    // (60 to 80 pixels is usually a good "talking range")
    return distance < 70; 
    }
    public void interact() {
        for (NPC npc : npcList) {
            if (isNear(npc)) {
                if (npc instanceof TrainerNPC) {
                    TrainerNPC trainer = (TrainerNPC) npc;
                    
                    if (!trainer.isDefeated()) {
                        System.out.println(trainer.getChallengeMessage());
                        System.out.println("goes to battle!");
                        //startBattle(trainer); // Transition to Battle State
                    } else {
                        System.out.println(trainer.name + ": You're pretty good. Keep training!");
                    }
                } else {
                    // Regular NPC logic
                    System.out.println(npc.name + ": It's a beautiful day for a walk.");
                }
                break; 
            }
        }
    }

    void centerSpawnSafe() { playerX = WORLD_WIDTH / 2; playerY = WORLD_HEIGHT / 2+PLAYER_SIZE; }

   // Inside update()
    void update() {
    if (currentState == GameState.PAUSE) return; // This freezes all movement and encounters!
    int nextX = playerX;
    int nextY = playerY;

        if (up)    { nextY -= SPEED; facing = Direction.UP; }
        if (down)  { nextY += SPEED; facing = Direction.DOWN; }
        if (left)  { nextX -= SPEED; facing = Direction.LEFT; }
        if (right) { nextX += SPEED; facing = Direction.RIGHT; }
        checkGrassEncounters();

        if (!isColliding(nextX, playerY)) playerX = nextX;
        if (!isColliding(playerX, nextY)) playerY = nextY;

        checkMapTransition();
        

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

private void drawPauseMenu(Graphics2D g2) {
    // 1. Draw a dark, semi-transparent tint over the whole game
    g2.setColor(new Color(0, 0, 0, 150)); 
    g2.fillRect(0, 0, getWidth(), getHeight());

    // 2. Draw the Menu Box in the center
    int boxW = 250;
    int boxH = 340;
    int boxX = (getWidth() - boxW) / 2;
    int boxY = (getHeight() - boxH) / 2;

    g2.setColor(Color.WHITE);
    g2.fillRoundRect(boxX, boxY, boxW, boxH, 15, 15);
    g2.setColor(Color.BLACK);
    g2.setStroke(new BasicStroke(3));
    g2.drawRoundRect(boxX, boxY, boxW, boxH, 15, 15);

    // 3. Draw the Title
    g2.setFont(new Font("Arial", Font.BOLD, 28));
    g2.drawString("PAUSED", boxX + 65, boxY + 45);

    // 4. Draw the Menu Options
    g2.setFont(new Font("Arial", Font.BOLD, 20));
    for (int i = 0; i < pauseOptions.length; i++) {
        int textY = boxY + 100 + (i * 40);
        
        // Draw the text
        g2.drawString(pauseOptions[i], boxX + 70, textY);
        
        // Draw the cursor
        if (i == pauseCursor) {
            g2.setColor(Color.BLUE);
            g2.drawString(">", boxX + 45, textY);
            g2.setColor(Color.BLACK); // Reset back to black for the next text
        }
    }
}
    
    public void drawShadow(Graphics2D g2, int screenX, int screenY) {
    g2.setColor(new Color(0, 0, 0, 60)); // Transparent black
    // Draw an oval at the NPC's feet
    g2.fillOval(screenX + 5, screenY + PLAYER_SIZE - 12, PLAYER_SIZE - 10, 10);
    }
    @Override
protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    
    if (currentState == GameState.OVERWORLD || currentState == GameState.PAUSE) {
        if (worldMap == null) return;

        int screenW = getWidth(); int screenH = getHeight();
        int offsetX = 0; int offsetY = 0;
        if (WORLD_WIDTH < screenW) offsetX = (screenW - WORLD_WIDTH) / 2;
        if (WORLD_HEIGHT < screenH) offsetY = (screenH - WORLD_HEIGHT) / 2;

        g2.drawImage(worldMap, offsetX - cameraX, offsetY - cameraY, null);

        boolean playerDrawn = false;
        for (NPC npc : npcList) {
            int npcScreenX = npc.x - cameraX + offsetX;
            int npcScreenY = npc.y - cameraY + offsetY;
            
            if (npcScreenX + npc.size > 0 && npcScreenX < screenW && npcScreenY + npc.size > 0 && npcScreenY < screenH) {
                // --- DRAW NPC ---
                g2.drawImage(npc.sprite, npcScreenX, npcScreenY, null);
                // g2.setColor(Color.WHITE);
                // g2.setFont(new Font("Arial", Font.PLAIN, 12));
                //g2.drawString(npc.name, npcScreenX, npcScreenY - 5);

                // --- NEW: INTERACTION POPUP ---
                // Check distance between player and npc
                double dist = Math.sqrt(Math.pow(playerX - npc.x, 2) + Math.pow(playerY - npc.y, 2));
                if (dist < 64) { // 64 is the interaction range
                    String prompt = "[E] Talk to " + npc.name;
                    g2.setFont(new Font("Arial", Font.BOLD, 14));
                    int textWidth = g2.getFontMetrics().stringWidth(prompt);
                    
                    // Draw Background Box
                    g2.setColor(new Color(0, 0, 0, 150));
                    g2.fillRoundRect(npcScreenX - (textWidth/4), npcScreenY - 35, textWidth + 20, 25, 10, 10);
                    
                    // Draw Text
                    g2.setColor(Color.WHITE);
                    g2.drawString(prompt, npcScreenX - (textWidth/4) + 10, npcScreenY - 18);
                }

                // --- Y-SORTING LOGIC (Preserved) ---
                if (!playerDrawn && (playerY + PLAYER_SIZE) < (npc.y + npc.size)) {
                    drawPlayer(g2, offsetX, offsetY);
                    playerDrawn = true;
                }
            }
        }
        
        if (!playerDrawn) {
            drawPlayer(g2, offsetX, offsetY);
        }
        

        // --- DRAW PLAYER (Final pass) ---
        BufferedImage sprite = switch (facing) {
            case UP -> playerUp; case DOWN -> playerDown; case LEFT -> playerLeft; case RIGHT -> playerRight;
        };
        if (sprite != null) g2.drawImage(sprite, playerX - cameraX + offsetX, playerY - cameraY + offsetY, null);
        if (currentState == GameState.PAUSE) {
        drawPauseMenu(g2);
        }
    } else if (currentState == GameState.BATTLE) {
        // --- ALL BATTLE CODE PRESERVED EXACTLY AS PROVIDED ---
        g.setColor(Color.WHITE); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        
        // --- ENEMY STATS ---
        g.setColor(Color.BLACK); g.setFont(new Font("Monospaced", Font.BOLD, 24));
        g.drawString(currentEnemy.getName() + " Lv" + currentEnemy.getLevel(), 450, 80);
        g.drawRect(450, 90, 200, 20);
        g.setColor(Color.GREEN);
        g.fillRect(450, 90, (int)((double)currentEnemy.getCurrentHp() / currentEnemy.getMaxHp() * 200), 20);
        if (enemyPokemonImg != null) g.drawImage(enemyPokemonImg, 475, 120, null); 
        
        // --- PLAYER STATS ---
        g.setColor(Color.BLACK);
        g.drawString(myPokemon.getName() + " Lv" + myPokemon.getLevel(), 100, 300);
        g.drawRect(100, 310, 200, 20);
        g.setColor(Color.GREEN);
        g.fillRect(100, 310, (int)((double)myPokemon.getCurrentHp() / myPokemon.getMaxHp() * 200), 20);
        g.setColor(Color.BLACK);
        g.drawString(myPokemon.getCurrentHp() + "/" + myPokemon.getMaxHp(), 100, 350);
        if (playerPokemonImg != null) g.drawImage(playerPokemonImg, 125, 130, null);
        
        // --- BATTLE UI ---
        if (currentBattleMenu == BattleMenu.POKEMON_MENU) {
            g.setColor(new Color(240, 240, 240)); 
            g.fillRect(50, 50, SCREEN_WIDTH - 100, SCREEN_HEIGHT - 100);
            g.setColor(Color.BLACK);
            g.drawRect(50, 50, SCREEN_WIDTH - 100, SCREEN_HEIGHT - 100);
            
            g.setFont(new Font("Monospaced", Font.BOLD, 24));
            g.drawString("Choose a Pokémon:", 80, 100);
            
            for (int i = 0; i < playerParty.size(); i++) {
                Pokemon p = playerParty.get(i);
                int yPos = 160 + (i * 60);
                g.setColor(Color.BLACK);
                g.drawString(p.getName() + " Lv" + p.getLevel() + "  HP: " + p.getCurrentHp() + "/" + p.getMaxHp(), 120, yPos);
                
                if (p == myPokemon) {
                    g.setColor(Color.BLUE);
                    g.drawString("(ACTIVE)", 500, yPos);
                } else if (p.isFainted()) {
                    g.setColor(Color.RED);
                    g.drawString("(FAINTED)", 500, yPos);
                }
            }
            g.setColor(Color.BLACK);
            g.drawString(">", 90, 160 + (partyCursor * 60)); 
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Press BACKSPACE to cancel.", 80, SCREEN_HEIGHT - 70);
        } 
        else {
            g.drawRect(50, 400, SCREEN_WIDTH - 100, 150);
            if (currentBattleMenu == BattleMenu.MAIN) {
                g.drawString("What will " + myPokemon.getName() + " do?", 70, 450);
                g.drawRect(450, 400, 300, 150);
                g.drawString("FIGHT", 500, 450); g.drawString("BAG", 650, 450);
                g.drawString("POKEMON", 500, 500); g.drawString("RUN", 650, 500);
                
                int cursorX = (menuCursor % 2 == 0) ? 470 : 620;
                int cursorY = (menuCursor < 2) ? 450 : 500;
                g.drawString(">", cursorX, cursorY);
            }
            else if (currentBattleMenu == BattleMenu.FIGHT) {
                java.util.List<String> moves = myPokemon.getKnownMoves();
                for (int i = 0; i < moves.size(); i++) {
                    int moveX = (i % 2 == 0) ? 100 : 350;
                    int moveY = (i < 2) ? 450 : 500;
                    g.drawString(moves.get(i), moveX, moveY);
                }
                int cursorX = (menuCursor % 2 == 0) ? 80 : 330;
                int cursorY = (menuCursor < 2) ? 450 : 500;
                if (menuCursor < moves.size()) g.drawString(">", cursorX, cursorY);
            }
            else if (currentBattleMenu == BattleMenu.BAG_MENU) {
    g.drawString("Items:", 70, 450);
    g.drawRect(450, 400, 300, 150);
    g.drawString("Pokéball", 500, 450); 
    g.drawString("Potion", 500, 500);

    // Re-use menuCursor for moving up and down the list
    int cursorY = (menuCursor == 0) ? 450 : 500;
    g.drawString(">", 470, cursorY);
}
            else { 
                g.drawString(battleMessage, 70, 450);
                g.setFont(new Font("Arial", Font.PLAIN, 16));
                g.drawString("Press ENTER to continue...", 70, 500);
            }
        }
    }
}


// Separate helper for Battle so it doesn't clutter the Overworld logic
private void drawBattleScreen(Graphics2D g2) {
    g2.setColor(Color.WHITE);
    g2.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
    
    g2.setColor(Color.BLACK);
    g2.setFont(new Font("Arial", Font.BOLD, 20));
    g2.drawString(battleMessage, 50, SCREEN_HEIGHT - 100);
    
    if (playerPokemonImg != null) g2.drawImage(playerPokemonImg, 50, 250, null);
    if (enemyPokemonImg != null) g2.drawImage(enemyPokemonImg, 500, 50, null);
    
    // You can add your Battle Menu drawing logic here!
}
public void startTrainerBattle(NPC trainer) {
    if (trainer.party.isEmpty()) return;

    // 1. Point the battle engine to the Trainer's first Pokemon
    this.currentEnemy = trainer.party.get(0); 
    
    // 2. Set the UI messages
    this.battleMessage = "Trainer " + trainer.name + " wants to battle!";
    
    // 3. Reset battle state
    this.currentBattleMenu = BattleMenu.MAIN;
    this.menuCursor = 0;
    this.currentState = GameState.BATTLE;
    
    // 4. Freeze movement so you don't walk away during the transition
    up = down = left = right = false;
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

    int cx = playerX + PLAYER_SIZE / 2; 
    int cy = playerY + PLAYER_SIZE / 2;
    if (cx < 0 || cy < 0 || cx >= WORLD_WIDTH || cy >= WORLD_HEIGHT) return;

    // FIX: Changed back to 0xFFFFFF to read the COLOR of the portal, not the transparency
    int key = collisionMap.getRGB(cx, cy) & 0xFFFFFF;
    
    // We add 0xFF000000 to the key to match how Java stores colors in the Map
    if (portalMap.containsKey(key | 0xFF000000)) { 
        MapData data = portalMap.get(key | 0xFF000000);
        
        // 1. Load the new map files
        loadMap(data.worldPath, data.collisionPath);
        findSpawnPoint(data.spawnColor);
        
        // 2. Update the Route Name so Viridian Forest spawns the right critters
        determineRouteFromMap(data.worldPath);
        
        System.out.println("DEBUG: Transitioned to " + data.worldPath + ". New Route: " + currentRouteName);
        
        lastTransitionTime = now;
    }
}
    private void triggerTrainerBattle(NPC trainer) {
    up = down = left = right = false; 
    if (myPokemon.isFainted()) myPokemon.heal(myPokemon.getMaxHp());
    
    currentTrainerParty = new ArrayList<>();
    String targetName = trainer.name; // Assuming your NPC class has a 'name' field
    
    // 1. Read the text file and find the trainer
    try (BufferedReader br = new BufferedReader(new FileReader("GymLeaders.txt"))) {
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(",");
            
            // If we found the right trainer in the file
            if (parts[0].trim().equalsIgnoreCase(targetName)) {
                
                // parts[1] is Badge ("Boulder"), parts[2] is TM ("TM39")
                // Pokemon data starts at index 3. We loop by 2 to grab Name and Level pairs.
                for (int i = 3; i < parts.length - 1; i += 2) {
                    String pName = parts[i].trim();
                    int pLevel = Integer.parseInt(parts[i+1].trim());
                    currentTrainerParty.add(new Pokemon(pName, pLevel));
                }
                break; // Stop reading the file once we found our trainer
            }
        }
    } catch (Exception e) {
        System.out.println("Error loading trainer party: " + e.getMessage());
        return; // Aborts the battle if the file is missing or broken
    }
    
    // Safety check just in case the trainer had no Pokemon in the file
    if (currentTrainerParty.isEmpty()) {
        System.out.println("No Pokemon found for " + targetName + "!");
        return;
    }

    // 2. Set the current enemy to their first Pokemon
    currentEnemy = currentTrainerParty.get(0);
    
    // 3. Load Images
    playerPokemonImg = loadPokemonImage(myPokemon.getName());
    enemyPokemonImg = loadPokemonImage(currentEnemy.getName());
    
    // 4. Set Battle Variables
    battleMessage = targetName + " wants to battle!";
    currentBattleMenu = BattleMenu.START_MESSAGE;
    menuCursor = 0;
    currentState = GameState.BATTLE; 
}
    @Override public void actionPerformed(ActionEvent e) { update(); repaint(); }
    
    @Override public void keyPressed(KeyEvent e) {

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
    if (currentState == GameState.OVERWORLD) {
        currentState = GameState.PAUSE;
        pauseCursor = 0; // Reset to the top option
        // Force the player to stop walking so they don't wander off while paused
        up = false; down = false; left = false; right = false;
    } 
    else if (currentState == GameState.PAUSE) {
        currentState = GameState.OVERWORLD; // Unpause
    }
}



// --- PAUSE MENU CONTROLS ---
if (currentState == GameState.PAUSE) {
    if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) {
        pauseCursor--;
        if (pauseCursor < 0) pauseCursor = pauseOptions.length - 1; // Wrap to bottom
    }
    if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) {
        pauseCursor++;
        if (pauseCursor >= pauseOptions.length) pauseCursor = 0; // Wrap to top
    }
    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
     // Execute the selected option!
        if (pauseCursor == 0) { 
            currentState = GameState.OVERWORLD; // Resume
        } 
        else if (pauseCursor == 1) { 
            // Pokemon Menu (Placeholder until you build the screen)
            System.out.println("Opening Pokemon Menu... (Coming soon!)");
        } 
        else if (pauseCursor == 2) { 
            // Bag Menu (Placeholder until you build the screen)
            System.out.println("Opening Bag... (Coming soon!)");
        } 
        else if (pauseCursor == 3) { 
            saveGame(); // Save
        } 
        else if (pauseCursor == 4) { 
            loadGame(); // Load
            currentState = GameState.OVERWORLD; // Unpause so you can play the loaded file
        } 
        else if (pauseCursor == 5) { 
            System.exit(0); // Quits the entire game window
        }
    }
}
        
        if (currentState == GameState.OVERWORLD) {
            int code = e.getKeyCode();
            if(e.getKeyCode() == KeyEvent.VK_W) up = true;
            if(e.getKeyCode() == KeyEvent.VK_S) down = true;
            if(e.getKeyCode() == KeyEvent.VK_A) left = true;
            if(e.getKeyCode() == KeyEvent.VK_D) right = true;
            if (e.getKeyCode() == KeyEvent.VK_K) {
            saveGame();
            }
            if (e.getKeyCode() == KeyEvent.VK_L) {
            loadGame();
            }

            // --- CHANGE INTERACT TO E ---
            if (e.getKeyCode() == KeyEvent.VK_E) {
                if (currentState == GameState.OVERWORLD) {
                for (NPC npc : npcList) {
                    // Using the distance logic we added to your paintComponent
                    double dist = Math.sqrt(Math.pow(playerX - npc.x, 2) + Math.pow(playerY - npc.y, 2));
            
                    if (dist < 64) {
                        System.out.println("predded");
                        triggerTrainerBattle(npc);
                        break; 
                }
        }
    }
}
        } 
       else if (currentState == GameState.BATTLE) {
            
            // --- 1. MENU NAVIGATION (W/A/S/D / Arrows) ---
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
            else if (currentBattleMenu == BattleMenu.BAG_MENU) {
                if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_UP) { menuCursor = 0; }
                if (e.getKeyCode() == KeyEvent.VK_S || e.getKeyCode() == KeyEvent.VK_DOWN) { menuCursor = 1; }
            }

            // --- 2. ENTER KEY LOGIC ---
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                if (currentBattleMenu == BattleMenu.START_MESSAGE) {
                    currentBattleMenu = BattleMenu.MAIN;
                }
                else if (currentBattleMenu == BattleMenu.MAIN) {
                    if (menuCursor == 0) { 
                        currentBattleMenu = BattleMenu.FIGHT; menuCursor = 0; 
                    } 
                    else if (menuCursor == 1) { 
                        // Open the Bag!
                        currentBattleMenu = BattleMenu.BAG_MENU;
                        menuCursor = 0;
                    }
                    else if (menuCursor == 2) {
                        currentBattleMenu = BattleMenu.POKEMON_MENU;
                        partyCursor = 0;
                    }
                    else if (menuCursor == 3) endBattle(); // RUN
                } 
                else if (currentBattleMenu == BattleMenu.BAG_MENU) {
                    if (menuCursor == 0) { // Pokéball
                        battleMessage = "You threw a Pokéball!";
                        currentBattleMenu = BattleMenu.CATCH_THROWN;
                    } 
                    else if (menuCursor == 1) { // Potion
                        myPokemon.heal(20);
                        battleMessage = "You used a Potion! " + myPokemon.getName() + " healed!";
                        currentBattleMenu = BattleMenu.SWAP_MESSAGE; 
                    }
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
                        
                        // NEW Move calculation logic!
                        Move selectedMove = Move.moveDatabase.get(moveName);
                        int damage = calculateDamage(myPokemon, currentEnemy, selectedMove);
                        
                        currentEnemy.takeDamage(damage);
                        battleMessage = myPokemon.getName() + " used " + moveName + "!";
                        currentBattleMenu = BattleMenu.PLAYER_MESSAGE; 
                    }
                } 
                else if (currentBattleMenu == BattleMenu.PLAYER_MESSAGE) {
                    if (currentEnemy.isFainted()) {
                        battleMessage = "Enemy " + currentEnemy.getName() + " fainted!";
                        int expGained = (currentEnemy.getBaseExpYield() * currentEnemy.getLevel()) / 7;
                        myPokemon.gainExp(expGained);
                        currentBattleMenu = BattleMenu.END_MESSAGE;
                    } else enemyAttack();
                }
            
                else if (currentBattleMenu == BattleMenu.CATCH_THROWN) {
                    int hpMax = currentEnemy.getMaxHp();
                    int hpCurrent = currentEnemy.getCurrentHp();
                    int catchRate = currentEnemy.getCatchRate();
                    
                    // The EXACT Gen 3/4 Math Formula
                    double a = ((3.0 * hpMax - 2.0 * hpCurrent) * catchRate * 1.0) / (3.0 * hpMax) * 1.0;

                    boolean caught = false;
                    int shakesPassed = 0;

                    if (a >= 255) {
                        caught = true;
                    } else {
                        double b = 65536.0 * Math.pow(a / 255.0, 0.25);
                        for (int i = 0; i < 4; i++) {
                            if ((Math.random() * 65536) < b) {
                                shakesPassed++;
                            }
                        }
                        if (shakesPassed == 4) caught = true;
                    }

                    if (caught) {
                        battleMessage = "Gotcha! " + currentEnemy.getName() + " was caught!";
                        if (playerParty.size() < 6) {
                            playerParty.add(currentEnemy);
                        } else {
                            System.out.println(currentEnemy.getName() + " was sent to the PC!");
                        }
                        currentBattleMenu = BattleMenu.END_MESSAGE;
                    } else {
                        if (shakesPassed == 0) battleMessage = "Oh no! The Pokémon broke free!";
                        else if (shakesPassed == 1) battleMessage = "Aww! It appeared to be caught!";
                        else if (shakesPassed == 2) battleMessage = "Aargh! Almost had it!";
                        else battleMessage = "Shoot! It was so close too!";
                        
                        currentBattleMenu = BattleMenu.CATCH_FAILED;
                    }
                }
                else if (currentBattleMenu == BattleMenu.CATCH_FAILED) {
                    enemyAttack(); // The missing link! Enemy attacks if catch fails.
                }

                else if (currentBattleMenu == BattleMenu.ENEMY_MESSAGE) {
                    if (myPokemon.isFainted()) {
                        battleMessage = myPokemon.getName() + " fainted!";
                        currentBattleMenu = BattleMenu.END_MESSAGE;
                    } else { 
                        currentBattleMenu = BattleMenu.MAIN; menuCursor = 0; 
                    }
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
            
            // --- 3. BACKSPACE LOGIC ---
            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                // Now supports backing out of the BAG_MENU
                if (currentBattleMenu == BattleMenu.FIGHT || currentBattleMenu == BattleMenu.POKEMON_MENU || currentBattleMenu == BattleMenu.BAG_MENU) {
                    currentBattleMenu = BattleMenu.MAIN; menuCursor = 0;
                }
            }
            
            if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                if (currentBattleMenu == BattleMenu.FIGHT || currentBattleMenu == BattleMenu.POKEMON_MENU) {
                    currentBattleMenu = BattleMenu.MAIN; menuCursor = 0;
                }
            }
        }
    }
    class EncounterData {
    String pokemonName;
    int chance;

    public EncounterData(String pokemonName, int chance) {
        this.pokemonName = pokemonName;
        this.chance = chance;
    }
}

public void determineRouteFromMap(String mapFilePath) {
    // Convert to lowercase to make checking easier
    String path = mapFilePath.toLowerCase();

    // Check the file path for keywords based on your map image names!
    if (path.contains("route1.png")) {
        currentRouteName = "Route_1";
    } 
    else if (path.contains("route 02.png")) {
        currentRouteName = "Route_2";
    } 
    else if (path.contains("viridianforest.png")) {
        currentRouteName = "Viridian_Forest";
    } 
    else if (path.contains("pallettown.png")) {
        currentRouteName = "Pallet_Town"; 
    } 
    else if (path.contains("viridian city.png")) {
        currentRouteName = "Viridian_City";
    } 
    else if (path.contains("pewtercity")) {
        currentRouteName = "Pewter_City";
    } 
    else {
        // For indoor maps like Oak's Lab, Gyms, or Gates, set to "None"
        // so wild Pokemon don't accidentally spawn indoors!
        currentRouteName = "None"; 
    }
    
    System.out.println("The player is now on: " + currentRouteName);
}
 public void saveGame() {
    // 1. Pop up a text box asking for the save name
    String saveName = JOptionPane.showInputDialog(this, "Enter a name for this save file:", "Save Game", JOptionPane.PLAIN_MESSAGE);

    // 2. If you click "Cancel" or leave it blank, abort the save entirely
    if (saveName == null || saveName.trim().isEmpty()) {
        System.out.println("Save cancelled.");
        return;
    }

    // 3. Automatically add ".txt" if you forgot to type it
    if (!saveName.endsWith(".txt")) {
        saveName += ".txt";
    }

    try {
        // 4. Combine your safe folder path with your new custom file name!
        String basePath = "C:\\Users\\WainBra\\Documents\\GitCode\\Pokemon\\";
        String filePath = basePath + saveName;
        
        PrintWriter writer = new PrintWriter(new FileWriter(filePath));
        
        writer.println("MAP_PATHS," + currentMapPath + "," + currentCollisionPath); 
        writer.println("PLAYER_POS," + playerX + "," + playerY);
        
        for (Pokemon p : playerParty) {
            writer.println("POKEMON," + p.getName() + "," + p.getLevel() + "," + p.getCurrentHp());
        }
        
        writer.close();
        System.out.println("Saved successfully as: " + saveName);
        
    } catch (IOException e) {
        System.out.println("Error saving the game: " + e.getMessage());
    }
}
public void loadEncounters() {
    String filePath = "C:\\Users\\WainBra\\Documents\\GitCode\\Pokemon\\encounters.txt";
    routeEncounters.clear(); 
    
    try {
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("CRITICAL ERROR: encounters.txt NOT FOUND at " + filePath);
            return;
        }

        Scanner scanner = new Scanner(file);
        
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty() || line.startsWith("#")) continue; 
            
            // Split the line by commas
            String[] parts = line.split(",\\s*"); 
            
            // We need at least RouteName, Pokemon, Chance (3 items minimum)
            if (parts.length >= 3) {
                String routeName = parts[0].trim().toLowerCase(); 
                
                // Create the route list if it doesn't exist yet
                if (!routeEncounters.containsKey(routeName)) {
                    routeEncounters.put(routeName, new ArrayList<>());
                }
                
                // NEW LOGIC: Loop through the rest of the line in pairs (Name, Chance)
                // This allows you to have as many Pokemon on one line as you want!
                for (int i = 1; i < parts.length - 1; i += 2) {
                    String pokeName = parts[i].trim();
                    int chance = Integer.parseInt(parts[i + 1].trim());
                    routeEncounters.get(routeName).add(new EncounterData(pokeName, chance));
                }
            }
        }
        scanner.close();
        System.out.println("SYSTEM: Encounter tables loaded successfully!");
        
    } catch (Exception e) {
        System.out.println("Error loading encounters: " + e.getMessage());
        e.printStackTrace();
    }
}

public void triggerWildBattle(String wildMonName) {
    // 1. Freeze player movement so they don't walk away during the transition
    up = down = left = right = false; 

    // Optional: Heal the player's lead Pokémon if it fainted previously 
    // (You had this in your trainer battle logic, so I kept it here to prevent instant game-overs!)
    if (myPokemon.isFainted()) {
        myPokemon.heal(myPokemon.getMaxHp());
    }
    
    // 2. Generate the Wild Enemy 
    // This picks a random level between 2 and 5. You can adjust this math later based on the route!
    int wildLevel = (int)(Math.random() * 4) + 2; 
    currentEnemy = new Pokemon(wildMonName, wildLevel);
    
    // 3. Load the battle sprites using your existing helper method
    playerPokemonImg = loadPokemonImage(myPokemon.getName());
    enemyPokemonImg = loadPokemonImage(currentEnemy.getName());
    
    // 4. Set up the Battle UI Variables
    battleMessage = "Wild " + wildMonName + " appeared!";
    currentBattleMenu = BattleMenu.START_MESSAGE; // Starts with the intro text prompt
    menuCursor = 0;
    
    // 5. THE BIG SWITCH: Change the game state to Battle!
    // The moment this line runs, your key listener and drawing methods will swap to the battle screen.
    currentState = GameState.BATTLE; 
}
public String rollWildEncounter(String currentRouteName) {
    String safeRoute = currentRouteName.trim().toLowerCase();
    ArrayList<EncounterData> possibleEncounters = routeEncounters.get(safeRoute);

    if (possibleEncounters == null || possibleEncounters.isEmpty()) {
        System.out.println("DEBUG: No encounter table found for: [" + safeRoute + "]");
        return null; 
    }

    // Roll a 100-sided die
    int roll = (int)(Math.random() * 100) + 1; 
    
    System.out.println("--- Wild Encounter Roll ---");
    System.out.println("Route: " + safeRoute);
    System.out.println("Dice Roll: " + roll + "/100");

    int cumulativeChance = 0;
    
    for (EncounterData encounter : possibleEncounters) {
        // Figure out where the bottom of this Pokemon's range starts
        int rangeStart = cumulativeChance + 1; 
        
        // Add its chance to find the top of the range
        cumulativeChance += encounter.chance;
        
        // Print the exact range (e.g., "Range: 1-50", "Range: 51-100")
        System.out.println("Checking " + encounter.pokemonName + " (Range: " + rangeStart + "-" + cumulativeChance + ")");

        if (roll <= cumulativeChance) {
            System.out.println("RESULT: A wild " + encounter.pokemonName + " appeared!");
            System.out.println("---------------------------");
            return encounter.pokemonName;
        }
    }
    
    // Fallback safety
    return possibleEncounters.get(0).pokemonName; 
}

   public void loadGame() {
    // 1. Pop up a text box asking which file to load
    String saveName = JOptionPane.showInputDialog(this, "Enter the name of the save file to load:", "Load Game", JOptionPane.PLAIN_MESSAGE);

    if (saveName == null || saveName.trim().isEmpty()) {
        System.out.println("Load cancelled.");
        return;
    }

    if (!saveName.endsWith(".txt")) {
        saveName += ".txt";
    }

    try {
        // 2. Combine the path to find your specific file
        String basePath = "C:\\Users\\WainBra\\Documents\\GitCode\\Pokemon\\";
        String filePath = basePath + saveName;
        File saveFile = new File(filePath);
        
        if (!saveFile.exists()) {
            // Warns you with a popup if you type a name that doesn't exist!
            JOptionPane.showMessageDialog(this, "No save file named '" + saveName + "' found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Scanner scanner = new Scanner(saveFile);
        playerParty.clear(); 

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] data = line.split(","); 

            if (data[0].equals("MAP_PATHS")) {
                loadMap(data[1], data[2]);
            } 
            else if (data[0].equals("PLAYER_POS")) {
                playerX = Integer.parseInt(data[1]);
                playerY = Integer.parseInt(data[2]);
            } 
            else if (data[0].equals("POKEMON")) {
                Pokemon loadedPokemon = new Pokemon(data[1], Integer.parseInt(data[2]));
                int hpDifference = loadedPokemon.getMaxHp() - Integer.parseInt(data[3]);
                if (hpDifference > 0) loadedPokemon.takeDamage(hpDifference);
                playerParty.add(loadedPokemon);
            }
        }
        
        scanner.close();
        if (!playerParty.isEmpty()) myPokemon = playerParty.get(0);
        
        System.out.println("Successfully loaded: " + saveName);
        
    } catch (Exception e) {
        System.out.println("Error loading the game: " + e.getMessage());
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
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.util.HashMap;
import java.util.Scanner;
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

    // ===== PLAYER & INVENTORY =====
    private int playerX, playerY;
    private final int PLAYER_SIZE = 35;
    private final int SPEED = 4;
    private int cameraX, cameraY;
    
    // Inventory items
    private int pokeballs = 5;
    private int potions = 1;

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
    private final String SPRITE_PATH = "T:\\HS\\Student\\Computer Science\\Software Engineering\\Pokemon Sprites\\";
    
    private ArrayList<Pokemon> playerParty = new ArrayList<>();
    private Pokemon myPokemon; 
    
    private Pokemon currentEnemy;
    private BufferedImage playerPokemonImg;
    private BufferedImage enemyPokemonImg;
    
    // Updated Enum for Catch Sequence and Bag
    private enum BattleMenu { 
        START_MESSAGE, MAIN, FIGHT, BAG, ITEM_MESSAGE, PLAYER_MESSAGE, 
        ENEMY_MESSAGE, CRIT_MESSAGE, END_MESSAGE, 
        SHAKE_1, SHAKE_2, SHAKE_3, CATCH_SUCCESS, CATCH_FAILED,
        POKEMON_MENU, SWAP_MESSAGE 
    }
    private BattleMenu currentBattleMenu = BattleMenu.MAIN;
    private int menuCursor = 0; 
    private int partyCursor = 0; 
    private String battleMessage = ""; 
    private boolean lastHitWasCrit = false; 
    private boolean isPlayerTurnAction = true; 
    private int shakesEarned = 0; // Calculated when ball is thrown

    private final Font NPC_FONT = new Font("Dialog", Font.BOLD, 14);

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

    private int calculateDamage(Pokemon attacker, Pokemon defender, int movePower) {
        lastHitWasCrit = false; 
        double baseDamage = ((((2.0 * attacker.getLevel() / 5.0) + 2.0) * movePower * ((double)attacker.getAttack() / defender.getDefense())) / 50.0) + 2.0;
        double critMultiplier = 1.0;
        if (Math.random() < 0.0625) { 
            critMultiplier = 1.5; 
            lastHitWasCrit = true;
        }
        double randomMultiplier = 0.85 + (Math.random() * 0.15);
        int finalDamage = (int)(baseDamage * critMultiplier * randomMultiplier);
        return Math.max(1, finalDamage);
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

    public void refreshNPCs(String currentMapPath) {
        npcList.clear(); 
        parseMasterNPCFile("npcs.txt", currentMapPath);
    }

    private void parseMasterNPCFile(String path, String filterMap) {
        File file = new File(path);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length < 5) continue;
                String mapInFile = data[0].trim();
                if (mapInFile.equalsIgnoreCase(filterMap)) {
                    String name = data[1].trim();
                    int x = Integer.parseInt(data[2].trim()) * SCALE;
                    int y = Integer.parseInt(data[3].trim()) * SCALE;
                    String sPath = data[4].trim();
                    BufferedImage sprite = loadAndScaleNPCSprite(sPath);
                    if (sprite != null) npcList.add(new NPC(name, x, y, PLAYER_SIZE, sprite));
                }
            }
        } catch (Exception e) { System.err.println("NPC System Error: " + e.getMessage()); }
    }

    private BufferedImage loadAndScaleNPCSprite(String path) {
        try {
            BufferedImage img = ImageIO.read(new File(path));
            return scaleSquare(img, PLAYER_SIZE);
        } catch (Exception e) { return null; }
    }

    private void checkGrassEncounter() {
        if (!(up || down || left || right)) return;
        int checkX = playerX + PLAYER_SIZE / 2;
        int checkY = playerY + PLAYER_SIZE - 5; 
        if (checkX < 0 || checkY < 0 || checkX >= WORLD_WIDTH || checkY >= WORLD_HEIGHT) return;
        int pixelColor = collisionMap.getRGB(checkX, checkY) & 0xFFFFFF;
        if (pixelColor == ENCOUNTER_COLOR) {
            if (Math.random() < ENCOUNTER_CHANCE) triggerEncounter();
        }
    }

    private void triggerEncounter() {
        up = down = left = right = false; 
        if (myPokemon.isFainted()) myPokemon.heal(myPokemon.getMaxHp());
        String[] possible = {"Pidgey", "Rattata", "Caterpie"};
        String enemyName = possible[(int)(Math.random() * possible.length)];
        if (enemyName.equals("Caterpie")) enemyName = "CaterPIE";
        
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
    
    private void enemyAttack() {
        isPlayerTurnAction = false; 
        int damage = calculateDamage(currentEnemy, myPokemon, 40);
        myPokemon.takeDamage(damage);
        battleMessage = "Enemy " + currentEnemy.getName() + " used Tackle!";
        if (lastHitWasCrit) currentBattleMenu = BattleMenu.CRIT_MESSAGE;
        else currentBattleMenu = BattleMenu.ENEMY_MESSAGE;
    }

    // Authentic Catch Formula logic
    private void tryCatchPokemon() {
        double catchRate = 200; // Base rate for common wild pokemon
        double ballMultiplier = 1.0; 
        double a = (((3 * currentEnemy.getMaxHp() - 2 * currentEnemy.getCurrentHp()) * catchRate * ballMultiplier) / (3 * currentEnemy.getMaxHp()));
        
        if (a >= 255) {
            shakesEarned = 4;
        } else {
            shakesEarned = 0;
            double p = Math.pow(a / 255.0, 0.25);
            for (int i = 0; i < 4; i++) {
                if (Math.random() < p) shakesEarned++;
                else break;
            }
        }
        battleMessage = "You threw a Pokeball!";
        currentBattleMenu = BattleMenu.ITEM_MESSAGE;
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

    private boolean isAreaClear(int startX, int startY) {
        if (startX < 0 || startY < 0 || startX + PLAYER_SIZE >= WORLD_WIDTH || startY + PLAYER_SIZE >= WORLD_HEIGHT) return false;
        for (int yy = startY; yy < startY + PLAYER_SIZE; yy++) {
            for (int xx = startX; xx < startX + PLAYER_SIZE; xx++) {
                if ((collisionMap.getRGB(xx, yy) & 0xFFFFFF) == 0x000000) return false; 
            }
        }
        return true; 
    }

    void centerSpawnSafe() { playerX = WORLD_WIDTH / 2; playerY = WORLD_HEIGHT / 2; }

    void update() {
        if (currentState == GameState.BATTLE) return;
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

    boolean isColliding(int nextX, int nextY) {
        if (nextX < 0 || nextY < 0 || nextX + PLAYER_SIZE > WORLD_WIDTH || nextY + PLAYER_SIZE > WORLD_HEIGHT) return true;
        Rectangle playerFeet = new Rectangle(nextX + 4, nextY + (PLAYER_SIZE / 2), PLAYER_SIZE - 8, PLAYER_SIZE / 2);
        for (NPC npc : npcList) {
            Rectangle npcFeet = new Rectangle(npc.x + 4, npc.y + (npc.size / 2), npc.size - 8, npc.size / 2);
            if (playerFeet.intersects(npcFeet)) return true;
        }
        int[] checkX = {nextX + 6, nextX + PLAYER_SIZE - 6};
        int[] checkY = {nextY + (PLAYER_SIZE / 2) + 4, nextY + PLAYER_SIZE - 2};
        for (int x : checkX) {
            for (int y : checkY) {
                if ((collisionMap.getRGB(x, y) & 0xFFFFFF) == 0x000000) return true;
            }
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        if (currentState == GameState.OVERWORLD) {
            if (worldMap == null) return;
            int offsetX = (WORLD_WIDTH < getWidth()) ? (getWidth() - WORLD_WIDTH) / 2 : 0;
            int offsetY = (WORLD_HEIGHT < getHeight()) ? (getHeight() - WORLD_HEIGHT) / 2 : 0;
            g2.drawImage(worldMap, offsetX - cameraX, offsetY - cameraY, null);
            boolean playerDrawn = false;
            for (NPC npc : npcList) {
                int npcScreenX = npc.x - cameraX + offsetX;
                int npcScreenY = npc.y - cameraY + offsetY;
                if (!playerDrawn && (playerY + PLAYER_SIZE) < (npc.y + npc.size)) {
                    drawPlayer(g2, offsetX, offsetY);
                    playerDrawn = true;
                }
                g2.drawImage(npc.sprite, npcScreenX, npcScreenY, null);
                g2.setColor(Color.WHITE);
                g2.setFont(NPC_FONT);
                g2.drawString(npc.name, npcScreenX, npcScreenY - 5);
            }
            if (!playerDrawn) drawPlayer(g2, offsetX, offsetY);
        } else if (currentState == GameState.BATTLE) {
            drawBattleUI(g);
        }
    }

    private void drawPlayer(Graphics2D g2, int offsetX, int offsetY) {
        BufferedImage sprite = switch (facing) {
            case UP -> playerUp; case DOWN -> playerDown; case LEFT -> playerLeft; case RIGHT -> playerRight;
        };
        if (sprite != null) g2.drawImage(sprite, playerX - cameraX + offsetX, playerY - cameraY + offsetY, null);
    }

    private void drawBattleUI(Graphics g) {
        g.setColor(Color.WHITE); g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        g.setColor(Color.BLACK); g.setFont(new Font("Monospaced", Font.BOLD, 24));
        
        // Enemy Stats
        g.drawString(currentEnemy.getName() + " Lv" + currentEnemy.getLevel(), 450, 80);
        g.drawRect(450, 90, 200, 20);
        g.setColor(Color.GREEN);
        g.fillRect(450, 90, (int)((double)currentEnemy.getCurrentHp() / currentEnemy.getMaxHp() * 200), 20);
        if (enemyPokemonImg != null) g.drawImage(enemyPokemonImg, 475, 120, null); 
        
        // Player Stats
        g.setColor(Color.BLACK);
        g.drawString(myPokemon.getName() + " Lv" + myPokemon.getLevel(), 100, 300);
        g.drawRect(100, 310, 200, 20);
        g.setColor(Color.GREEN);
        g.fillRect(100, 310, (int)((double)myPokemon.getCurrentHp() / myPokemon.getMaxHp() * 200), 20);
        g.setColor(Color.BLACK);
        g.drawString(myPokemon.getCurrentHp() + "/" + myPokemon.getMaxHp(), 100, 350);
        if (playerPokemonImg != null) g.drawImage(playerPokemonImg, 125, 130, null);

        // Menus
        g.drawRect(50, 400, SCREEN_WIDTH - 100, 150);
        if (currentBattleMenu == BattleMenu.MAIN) {
            g.drawString("What will " + myPokemon.getName() + " do?", 70, 450);
            g.drawRect(450, 400, 300, 150);
            g.drawString("FIGHT", 500, 450); g.drawString("BAG", 650, 450);
            g.drawString("POKEMON", 500, 500); g.drawString("RUN", 650, 500);
            int cursorX = (menuCursor % 2 == 0) ? 470 : 620;
            int cursorY = (menuCursor < 2) ? 450 : 500;
            g.drawString(">", cursorX, cursorY);
        } else if (currentBattleMenu == BattleMenu.FIGHT) {
            java.util.List<String> moves = myPokemon.getKnownMoves();
            for (int i = 0; i < moves.size(); i++) {
                int moveX = (i % 2 == 0) ? 100 : 350;
                int moveY = (i < 2) ? 450 : 500;
                g.drawString(moves.get(i), moveX, moveY);
            }
            int cursorX = (menuCursor % 2 == 0) ? 80 : 330;
            int cursorY = (menuCursor < 2) ? 450 : 500;
            g.drawString(">", cursorX, cursorY);
        } else if (currentBattleMenu == BattleMenu.BAG) {
            g.drawString("POTION   x" + potions, 100, 450);
            g.drawString("POKEBALL x" + pokeballs, 100, 500);
            int cursorY = (menuCursor == 0) ? 450 : 500;
            g.drawString(">", 70, cursorY);
        } else if (currentBattleMenu == BattleMenu.POKEMON_MENU) {
            drawPartyMenu(g);
        } else {
            g.drawString(battleMessage, 70, 450);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Press ENTER...", 70, 500);
        }
    }
}

    private void drawPartyMenu(Graphics g) {
        g.setColor(new Color(240, 240, 240)); 
        g.fillRect(50, 50, SCREEN_WIDTH - 100, SCREEN_HEIGHT - 100);
        g.setColor(Color.BLACK);
        g.drawRect(50, 50, SCREEN_WIDTH - 100, SCREEN_HEIGHT - 100);
        for (int i = 0; i < playerParty.size(); i++) {
            Pokemon p = playerParty.get(i);
            g.drawString(p.getName() + " Lv" + p.getLevel() + " HP: " + p.getCurrentHp(), 120, 160 + (i * 60));
        }
        g.drawString(">", 90, 160 + (partyCursor * 60)); 
    }

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
            int code = e.getKeyCode();
            if(e.getKeyCode() == KeyEvent.VK_W) up = true;
            if(e.getKeyCode() == KeyEvent.VK_S) down = true;
            if(e.getKeyCode() == KeyEvent.VK_A) left = true;
            if(e.getKeyCode() == KeyEvent.VK_D) right = true;
        } else if (currentState == GameState.BATTLE) {
            handleBattleInput(e);
        }
    }

    private void handleBattleInput(KeyEvent e) {
        int code = e.getKeyCode();
        if (currentBattleMenu == BattleMenu.MAIN || currentBattleMenu == BattleMenu.FIGHT) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) { if (menuCursor >= 2) menuCursor -= 2; }
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) { if (menuCursor <= 1) menuCursor += 2; }
            if (code == KeyEvent.VK_A || code == KeyEvent.VK_LEFT) { if (menuCursor % 2 != 0) menuCursor -= 1; }
            if (code == KeyEvent.VK_D || code == KeyEvent.VK_RIGHT) { if (menuCursor % 2 == 0) menuCursor += 1; }
        } else if (currentBattleMenu == BattleMenu.BAG) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) menuCursor = 0;
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) menuCursor = 1;
        } else if (currentBattleMenu == BattleMenu.POKEMON_MENU) {
            if (code == KeyEvent.VK_W || code == KeyEvent.VK_UP) if (partyCursor > 0) partyCursor--;
            if (code == KeyEvent.VK_S || code == KeyEvent.VK_DOWN) if (partyCursor < playerParty.size() - 1) partyCursor++;
        }

        if (code == KeyEvent.VK_ENTER) {
            // Sequence for the 3 Shakes
            if (currentBattleMenu == BattleMenu.ITEM_MESSAGE) {
                if (battleMessage.contains("Pokeball")) {
                    currentBattleMenu = BattleMenu.SHAKE_1; battleMessage = ". . .";
                } else enemyAttack();
            }
            else if (currentBattleMenu == BattleMenu.SHAKE_1) {
                if (shakesEarned >= 1) { currentBattleMenu = BattleMenu.SHAKE_2; battleMessage = ". . . Shake!"; }
                else { currentBattleMenu = BattleMenu.CATCH_FAILED; battleMessage = "Oh no! The Pokemon broke free!"; }
            }
            else if (currentBattleMenu == BattleMenu.SHAKE_2) {
                if (shakesEarned >= 2) { currentBattleMenu = BattleMenu.SHAKE_3; battleMessage = ". . . Shake!!"; }
                else { currentBattleMenu = BattleMenu.CATCH_FAILED; battleMessage = "Aww! It appeared to be caught!"; }
            }
            else if (currentBattleMenu == BattleMenu.SHAKE_3) {
                if (shakesEarned >= 3) { currentBattleMenu = BattleMenu.CATCH_SUCCESS; battleMessage = ". . . Shake!!!"; }
                else { currentBattleMenu = BattleMenu.CATCH_FAILED; battleMessage = "Arrgh! Almost had it!"; }
            }
            else if (currentBattleMenu == BattleMenu.CATCH_SUCCESS) {
                if (shakesEarned == 4) {
                    battleMessage = "Gotcha! " + currentEnemy.getName() + " was caught!";
                    if (playerParty.size() < 6) playerParty.add(currentEnemy);
                    currentBattleMenu = BattleMenu.END_MESSAGE;
                } else {
                    battleMessage = "Shoot! It was so close!"; currentBattleMenu = BattleMenu.CATCH_FAILED;
                }
            }
            else if (currentBattleMenu == BattleMenu.CATCH_FAILED) enemyAttack();
            else if (currentBattleMenu == BattleMenu.START_MESSAGE) currentBattleMenu = BattleMenu.MAIN;
            else if (currentBattleMenu == BattleMenu.MAIN) {
                if (menuCursor == 0) { currentBattleMenu = BattleMenu.FIGHT; menuCursor = 0; }
                else if (menuCursor == 1) { currentBattleMenu = BattleMenu.BAG; menuCursor = 0; }
                else if (menuCursor == 2) { currentBattleMenu = BattleMenu.POKEMON_MENU; partyCursor = 0; }
                else if (menuCursor == 3) endBattle();
            } 
            else if (currentBattleMenu == BattleMenu.BAG) {
                if (menuCursor == 0 && potions > 0) {
                    potions--; myPokemon.heal(myPokemon.getMaxHp());
                    battleMessage = "Used Potion! " + myPokemon.getName() + " fully healed!";
                    currentBattleMenu = BattleMenu.ITEM_MESSAGE;
                } else if (menuCursor == 1 && pokeballs > 0) {
                    pokeballs--; tryCatchPokemon();
                }
            }
            else if (currentBattleMenu == BattleMenu.FIGHT) {
                isPlayerTurnAction = true; 
                currentEnemy.takeDamage(calculateDamage(myPokemon, currentEnemy, 40));
                battleMessage = myPokemon.getName() + " used " + myPokemon.getKnownMoves().get(menuCursor) + "!";
                currentBattleMenu = lastHitWasCrit ? BattleMenu.CRIT_MESSAGE : BattleMenu.PLAYER_MESSAGE;
            } 
            else if (currentBattleMenu == BattleMenu.CRIT_MESSAGE) {
                battleMessage = "A critical hit!";
                currentBattleMenu = isPlayerTurnAction ? BattleMenu.PLAYER_MESSAGE : BattleMenu.ENEMY_MESSAGE;
            }
            else if (currentBattleMenu == BattleMenu.PLAYER_MESSAGE) {
                if (currentEnemy.isFainted()) { battleMessage = "Enemy fainted!"; currentBattleMenu = BattleMenu.END_MESSAGE; }
                else enemyAttack();
            } 
            else if (currentBattleMenu == BattleMenu.ENEMY_MESSAGE) {
                if (myPokemon.isFainted()) { battleMessage = myPokemon.getName() + " fainted!"; currentBattleMenu = BattleMenu.END_MESSAGE; }
                else currentBattleMenu = BattleMenu.MAIN;
            } 
            else if (currentBattleMenu == BattleMenu.END_MESSAGE) endBattle();
        }
        if (code == KeyEvent.VK_BACK_SPACE) currentBattleMenu = BattleMenu.MAIN;
    }
    
    @Override public void keyReleased(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_W) up = false;
        if(e.getKeyCode() == KeyEvent.VK_S) down = false;
        if(e.getKeyCode() == KeyEvent.VK_A) left = false;
        if(e.getKeyCode() == KeyEvent.VK_D) right = false;
    }
    @Override public void keyTyped(KeyEvent e) {}
}
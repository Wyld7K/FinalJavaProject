import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Timer;

import javax.sound.sampled.Clip;
import javax.swing.JPanel;

public class GamePanel extends JPanel implements Runnable {
    final int originalTileSize = 16;
    final int scale = 3;

    public final int tileSize = originalTileSize * scale;
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 12;
    public final int screenWidth = tileSize * maxScreenCol;
    public final int screenHeight = tileSize * maxScreenRow;
    public final int FPS = 60;
    public boolean isPlayingBossTheme = false;
    public boolean isPlayingSoundEffect = false;

    public Thread gameThread;
    public KeyHandler keyHandler = new KeyHandler();
    public Player player = new Player(this, keyHandler, "player-spritesheet.png");
    public TileManager tileManager;
    public CollisionChecker collisionChecker = new CollisionChecker(this);
    public InteractableObject amongUs;
    public InteractableObject jonTafferBoss;
    public InteractableObject jonAngryTafferBoss;
    public int damageCounter = 0;

    // World Settings
    public final int maxWorldCol = 50;
    public final int maxWorldRow = 50;
    public final int worldWidth = tileSize * maxWorldCol;
    public final int worldHeight = tileSize * maxWorldRow;

    public boolean isBattlingBoss = false;
    public Clip amongUsClip;
    public Clip bossMusic;
    public Clip jonTafferClip;
    public int drawCount = 0;
    public Clip backgroundMusic;

    public int bossDamage = 0;
    public Timer bossDamageTimer = new Timer();
    boolean isAmongusBossDefeated = false;
    boolean shouldSpawnJohnTaffer = false;
    boolean isPlayingJon = false;

    boolean isJonToggle = false;

    public GamePanel() {

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.black);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyHandler);
        this.setFocusable(true);
        this.tileManager = new TileManager(this);

        amongUs = new InteractableObject(Sprite.initSprite("amongus"), "amongus", tileSize * 37,
                tileSize * 8);
        jonTafferBoss = new InteractableObject(Sprite.initSprite("jontaffer"), "jon", tileSize * 25,
                tileSize * 39);

    }

    /**
     * Starts the game clock and sound
     */
    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();

    }

    /**
     * Core delta game loop of game
     */
    @Override
    public void run() {
        double drawInterval = 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {

            currentTime = System.nanoTime();

            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            // Delta has reached draw interval
            if (delta >= 1) {
                // Update information of game
                update();

                // Draw that reflects new information of game
                repaint();
                delta--;
            }
        }

    }

    public void update() {
        player.update();

    }

    /**
     * Draws map and player
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        tileManager.draw(g2d, PathFinder.getFilePathForFile("map.txt").toAbsolutePath().toString());
        player.draw(g2d);

        InteractableObject chest = new InteractableObject(
                new Sprite("chests", new int[] { 120, 130 }, new String[] { "Chest-Opening" }, new int[] { 2 }),
                "Chest", tileSize * 21,
                tileSize * 23);

        chest.draw((Graphics2D) g, this, "Chest-Opening");

        if (!isAmongusBossDefeated)
            amongUs.draw((Graphics2D) g, this);
        else if (isAmongusBossDefeated && drawCount < 2) {
            amongUs.singleSprite = Sprite.initSprite("dabbing.png");
            amongUs.draw((Graphics2D) g, this);
            drawCount++;

        }

        if (shouldSpawnJohnTaffer) {

            if (Math.abs(jonTafferBoss.worldX - player.worldX) < 100
                    && Math.abs(jonTafferBoss.worldY - player.worldY) < 100) {
                jonTafferBoss.singleSprite = Sprite.initSprite("jon-taffer");
                jonTafferBoss.draw((Graphics2D) g, this);

                if (damageCounter >= 75 && damageCounter != 0) {
                    damageCounter = 0;
                    player.health -= 1;
                    SoundManager.playSound(PathFinder.getFilePathForFile("off.wav").toFile()).start();

                } else if (damageCounter >= 75 && player.health == 0)
                    SoundManager.playSound(PathFinder.getFilePathForFile("oof.wav").toFile()).start();

                else {
                    System.out.println(damageCounter);

                    damageCounter++;
                }

            } else if (Math.abs(jonTafferBoss.worldX - player.worldX) < 200
                    && Math.abs(jonTafferBoss.worldY - player.worldY) < 200) {
                jonTafferBoss.singleSprite = Sprite.initSprite("jon");

                jonTafferBoss.draw((Graphics2D) g, this);
            }

            if (!isPlayingJon) {

                SoundManager.playSound(PathFinder.getFilePathForFile("victory.wav").toFile()).start();
                try {
                    Thread.sleep(7000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                jonTafferClip = SoundManager.playSound(PathFinder.getFilePathForFile("jon.wav").toFile());
                jonTafferClip.start();

                isPlayingJon = true;

            }

        }

        drawHearts(g2d);
        g2d.dispose();

    }

    public int checkForDamage() {
        initBossBattle();
        return -1;

    }

    private void initBossBattle() {
        if (Math.abs(amongUs.worldX - player.worldX) < 300 && Math.abs(amongUs.worldY - player.worldY) < 280) {
            isBattlingBoss = true;
            if (!isPlayingBossTheme && !isPlayingSoundEffect) {
                amongUsClip = SoundManager.playSound(PathFinder.getFilePathForFile("amongusEffect.wav").toFile());
                bossMusic = SoundManager.playSound(PathFinder.getFilePathForFile("bosstheme.wav").toFile());

                amongUsClip.start();
                bossMusic.start();

                isPlayingBossTheme = true;
                isPlayingSoundEffect = true;

            } else if (Math.abs(amongUs.worldX - player.worldX) < 80
                    && Math.abs(amongUs.worldY - player.worldY) < 100 && player.keyHandler.attackPressed) {

                if (bossDamage < 30) {
                    bossDamage++;

                } else {
                    isAmongusBossDefeated = true;
                    bossMusic.stop();
                    shouldSpawnJohnTaffer = true;
                    isBattlingBoss = false;

                }
                // Check if player is damaging boss

            }

        } else {

            isPlayingBossTheme = false;
            isPlayingSoundEffect = false;
            if (bossMusic != null) {
                bossMusic.stop();
            }

        }

    }

    private void drawHearts(Graphics2D g2d) {
        int[] healthIndicies = new int[3];
        int numFullHearts = player.health / 2;
        int remainder = player.health % 2;

        // Init num of full hearts
        for (int i = 0; i < (numFullHearts); i++)
            healthIndicies[i] = 2;

        for (int i = numFullHearts; i < numFullHearts + remainder; i++)
            healthIndicies[i] = 1;

        for (int i = 0; i < player.hearts.size(); i++) {
            g2d.drawImage(player.hearts.get(healthIndicies[i]), 15 + (i * 35), 15, tileSize / 2, tileSize / 2, null);

        }
    }
}

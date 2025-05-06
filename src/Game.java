import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;

public class Game extends JPanel implements ActionListener, KeyListener, MouseMotionListener, MouseListener {
    // Window dimensions
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    // Game objects
    private Plane plane;
    private ArrayList<Bullet> bullets;
    private Timer timer;
    private Timer explosionTimer; // Added timer for explosion animation
    private Random random;
    private boolean gameOver;
    private int score;

    // Graphics
    private BufferedImage planeImg;
    private BufferedImage bulletImg;
    private BufferedImage explosionImg;
    private BufferedImage backgroundImg;

    // Sound
    private Clip explosionSound;

    // Mouse control
    private boolean mouseControlEnabled = true;

    // Plane auto-movement settings
    private int planeAutoSpeedX = 2;
    private int planeAutoSpeedY = 0;
    private int maxVelocity = 5;
    private double acceleration = 0.2;
    private double deceleration = 0.1;

    // Difficulty levels
    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
    private Difficulty currentDifficulty = Difficulty.EASY;
    private Map<Difficulty, Integer> difficultyScores = new HashMap<>();
    private Map<Difficulty, Integer> highScores = new HashMap<>();
    
    // Level progression thresholds
    private static final int MEDIUM_THRESHOLD = 500;
    private static final int HARD_THRESHOLD = 1000;
    
    // Bullet spawn rates (percentage chance per frame)
    private static final int EASY_SPAWN_RATE = 3;
    private static final int MEDIUM_SPAWN_RATE = 5;
    private static final int HARD_SPAWN_RATE = 8;
    
    // Bullet speeds
    private static final int EASY_BULLET_SPEED = -6;
    private static final int MEDIUM_BULLET_SPEED = -8;
    private static final int HARD_BULLET_SPEED = -12;
    
    // Score multipliers
    private static final int EASY_SCORE_MULTIPLIER = 1;
    private static final int MEDIUM_SCORE_MULTIPLIER = 2;
    private static final int HARD_SCORE_MULTIPLIER = 3;
    
    // Score file
    private static final String SCORES_FILE = "game_scores.txt";
    
    // Game state
    private boolean isPaused = false;
    private boolean showLevelSelect = true; // Start with level select screen
    private boolean levelCompleted = false;
    private boolean showingExplosion = false; // Flag for explosion animation
    private int explosionDuration = 0; // Counter for explosion animation
    
    // UI elements
    private Color[] difficultyColors = {
        new Color(46, 204, 113), // Easy - Green
        new Color(241, 196, 15), // Medium - Yellow
        new Color(231, 76, 60)   // Hard - Red
    };
    
    // Button areas for level selection (for mouse interaction)
    private Rectangle[] levelButtons = new Rectangle[3];
    private Rectangle startButton;
    private int selectedLevelIndex = 0; // 0=Easy, 1=Medium, 2=Hard
    
    // Button hover state
    private boolean startButtonHover = false;
    private boolean[] levelButtonHover = new boolean[3];

    public Game() {
        // Initialize panel
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        addMouseListener(this);

        // Initialize UI elements
        int buttonWidth = 300;
        int buttonHeight = 60;
        int startY = 200;
        int spacing = 80;
        
        for (int i = 0; i < 3; i++) {
            levelButtons[i] = new Rectangle((WIDTH - buttonWidth) / 2, startY + i * spacing, buttonWidth, buttonHeight);
            levelButtonHover[i] = false;
        }
        
        startButton = new Rectangle((WIDTH - buttonWidth) / 2, startY + 3 * spacing, buttonWidth, buttonHeight);

        // Load resources
        loadImages();
        loadSounds();
        loadHighScores();

        // Initialize game objects
        initializeGame();
        
        // Request focus to ensure keyboard input works
        requestFocusInWindow();
        
        // Add a focus listener to handle focus changes
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // No need to do anything special here
            }
            
            @Override
            public void focusLost(FocusEvent e) {
                // Pause the game if it loses focus and is running
                if (!showLevelSelect && !gameOver && !levelCompleted && !isPaused) {
                    isPaused = true;
                    repaint();
                }
            }
        });
    }

    private void initializeGame() {
        plane = new Plane(100, HEIGHT / 2, planeImg);
        bullets = new ArrayList<>();
        random = new Random();
        gameOver = false;
        score = 0;
        levelCompleted = false;
        showingExplosion = false;
        explosionDuration = 0;

        // Apply difficulty settings
        applyDifficultySettings();

        // Start game loop
        timer = new Timer(20, this);
        timer.start();
        
        // Create explosion timer but don't start it yet
        explosionTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // After explosion timer finishes, show game over screen
                showingExplosion = false;
                explosionTimer.stop();
                repaint();
            }
        });
        explosionTimer.setRepeats(false);
    }
    
    private void applyDifficultySettings() {
        switch (currentDifficulty) {
            case EASY:
                planeAutoSpeedX = 2;
                maxVelocity = 5;
                break;
            case MEDIUM:
                planeAutoSpeedX = 3;
                maxVelocity = 6;
                break;
            case HARD:
                planeAutoSpeedX = 4;
                maxVelocity = 7;
                break;
        }
    }

    private void loadImages() {
        try {
            // Load images using file paths matching your project structure
            BufferedImage originalPlaneImg = ImageIO.read(new File("src/Resources/plane.png"));
            BufferedImage originalBulletImg = ImageIO.read(new File("src/Resources/bullet.png"));
            BufferedImage originalExplosionImg = ImageIO.read(new File("src/Resources/exploision.png"));
            
            // Try to load background image
            try {
                backgroundImg = ImageIO.read(new File("src/Resources/background.png"));
                backgroundImg = scaleImage(backgroundImg, WIDTH, HEIGHT);
            } catch (IOException e) {
                System.out.println("Background image not found, using generated background");
                backgroundImg = createGradientBackground(WIDTH, HEIGHT);
            }

            // Scale down images to appropriate sizes
            planeImg = scaleImage(originalPlaneImg, 200, 100);
            bulletImg = scaleImage(originalBulletImg, 60, 30);
            explosionImg = scaleImage(originalExplosionImg, 100, 100);

            System.out.println("Images loaded successfully");
            System.out.println("Explosion image dimensions: " + explosionImg.getWidth() + "x" + explosionImg.getHeight());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to load images, using placeholders");
            // Create placeholder images if loading fails
            planeImg = createPlaceholderImage(100, 50, Color.BLUE);
            bulletImg = createPlaceholderImage(30, 15, Color.RED);
            explosionImg = createExplosionPlaceholder(100, 100);
            backgroundImg = createGradientBackground(WIDTH, HEIGHT);
        }
    }
    
    private BufferedImage createGradientBackground(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        
        // Create a gradient from dark blue to black
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(0, 0, 40),
            0, height, new Color(0, 0, 10)
        );
        
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);
        
        // Add some "stars"
        g2d.setColor(Color.WHITE);
        Random rand = new Random(42); // Fixed seed for consistent stars
        for (int i = 0; i < 200; i++) {
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            int size = rand.nextInt(2) + 1;
            g2d.fillRect(x, y, size, size);
        }
        
        g2d.dispose();
        return img;
    }

    private BufferedImage scaleImage(BufferedImage original, int width, int height) {
        BufferedImage scaledImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        return scaledImg;
    }

    private BufferedImage createPlaceholderImage(int width, int height, Color color) {
        // Create a simple colored rectangle as placeholder
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return img;
    }
    
    private BufferedImage createExplosionPlaceholder(int width, int height) {
        // Create a more visible explosion placeholder
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        
        // Draw explosion-like shape
        g.setColor(Color.RED);
        g.fillOval(0, 0, width, height);
        
        g.setColor(Color.ORANGE);
        g.fillOval(width/4, height/4, width/2, height/2);
        
        g.setColor(Color.YELLOW);
        g.fillOval(width/3, height/3, width/3, height/3);
        
        g.dispose();
        return img;
    }

    private void loadSounds() {
        try {
            // Load sound file using file path matching your project structure
            File soundFile = new File("src/Resources/exploision.wav");

            // Try to convert the audio to a supported format
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat baseFormat = audioStream.getFormat();

            // Convert to a more compatible format (16 bit, mono)
            AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    1,
                    2,
                    baseFormat.getSampleRate(),
                    false
            );

            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioStream);
            explosionSound = AudioSystem.getClip();
            explosionSound.open(convertedStream);
            System.out.println("Sound loaded successfully");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Failed to load sound - will continue without sound effects");
        }
    }

    private void playExplosionSound() {
        if (explosionSound != null) {
            try {
                if (explosionSound.isRunning()) {
                    explosionSound.stop();
                }
                explosionSound.setFramePosition(0);
                explosionSound.start();
            } catch (Exception e) {
                System.out.println("Error playing sound: " + e.getMessage());
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isPaused || showLevelSelect) {
            return;
        }
        
        if (!gameOver && !levelCompleted) {
            // Update game state
            updatePlane();
            updateBullets();
            checkCollisions();
            spawnBullets();
            
            // Update score based on difficulty
            int multiplier = getScoreMultiplier();
            score += multiplier;
            
            // Check for level progression
            checkLevelProgression();
        }
        // Redraw the screen
        repaint();
    }
    
    private int getScoreMultiplier() {
        switch (currentDifficulty) {
            case EASY: return EASY_SCORE_MULTIPLIER;
            case MEDIUM: return MEDIUM_SCORE_MULTIPLIER;
            case HARD: return HARD_SCORE_MULTIPLIER;
            default: return 1;
        }
    }
    
    private void checkLevelProgression() {
        // Check if player has reached score threshold for next level
        if (currentDifficulty == Difficulty.EASY && score >= MEDIUM_THRESHOLD) {
            levelCompleted = true;
            difficultyScores.put(Difficulty.EASY, score);
            updateHighScore(Difficulty.EASY, score);
        } else if (currentDifficulty == Difficulty.MEDIUM && score >= HARD_THRESHOLD) {
            levelCompleted = true;
            difficultyScores.put(Difficulty.MEDIUM, score);
            updateHighScore(Difficulty.MEDIUM, score);
        }
    }

    private void updatePlane() {
        // Apply user control velocity on top of auto movement

        // Update plane position based on its velocity
        plane.setX((int) (plane.getX() + plane.getVelocityX()));
        plane.setY((int) (plane.getY() + plane.getVelocityY()));

        // Decelerate when no key is pressed
        if (plane.getVelocityX() > 0) {
            plane.setVelocityX(Math.max(0, plane.getVelocityX() - deceleration));
        } else if (plane.getVelocityX() < 0) {
            plane.setVelocityX(Math.min(0, plane.getVelocityX() + deceleration));
        }

        if (plane.getVelocityY() > 0) {
            plane.setVelocityY(Math.max(0, plane.getVelocityY() - deceleration));
        } else if (plane.getVelocityY() < 0) {
            plane.setVelocityY(Math.min(0, plane.getVelocityY() + deceleration));
        }

        // Keep plane within screen bounds
        if (plane.getY() < 0) {
            plane.setY(0);
            plane.setVelocityY(0);
        } else if (plane.getY() > HEIGHT - plane.getHeight()) {
            plane.setY(HEIGHT - plane.getHeight());
            plane.setVelocityY(0);
        }
        if (plane.getX() < 0) {
            plane.setX(0);
            plane.setVelocityX(0);
        } else if (plane.getX() > WIDTH - plane.getWidth()) {
            plane.setX(WIDTH - plane.getWidth());
            plane.setVelocityX(0);
        }

        // Apply auto-movement last for consistent behavior
        plane.setX(plane.getX() + planeAutoSpeedX);

        // Check if the plane reached right edge, if so, reset the position
        if (plane.getX() > WIDTH) {
            plane.setX(0);
            plane.setY(HEIGHT / 2);
        }
    }

    private void updateBullets() {
        // Update all bullets and remove those that go off-screen
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet bullet = it.next();
            bullet.update();

            // Remove bullets that have gone off the left side of the screen
            if (bullet.getX() + bullet.getWidth() < 0) {
                it.remove();
            }
        }
    }

    private void spawnBullets() {
        // Ensure we have a valid range for random position
        int maxY = HEIGHT - bulletImg.getHeight();
        if (maxY <= 0) maxY = HEIGHT - 1;

        // Get spawn rate based on difficulty
        int spawnRate;
        int bulletSpeed;
        
        switch (currentDifficulty) {
            case EASY:
                spawnRate = EASY_SPAWN_RATE;
                bulletSpeed = EASY_BULLET_SPEED;
                break;
            case MEDIUM:
                spawnRate = MEDIUM_SPAWN_RATE;
                bulletSpeed = MEDIUM_BULLET_SPEED;
                break;
            case HARD:
                spawnRate = HARD_SPAWN_RATE;
                bulletSpeed = HARD_BULLET_SPEED;
                break;
            default:
                spawnRate = EASY_SPAWN_RATE;
                bulletSpeed = EASY_BULLET_SPEED;
        }

        // Randomly spawn new bullets from the right side of the screen
        if (random.nextInt(100) < spawnRate) {
            int y = random.nextInt(maxY);
            Bullet bullet = new Bullet(WIDTH, y, bulletImg);
            // Set bullet velocity based on difficulty
            bullet.setVelocityX(bulletSpeed);
            bullets.add(bullet);
        }
    }

    private void checkCollisions() {
        // Check for collisions between plane and bullets
        Rectangle planeRect = plane.getBounds();

        for (Bullet bullet : bullets) {
            if (planeRect.intersects(bullet.getBounds())) {
                gameOver = true;
                plane.setExploding(true);
                showingExplosion = true;
                playExplosionSound();
                
                // Start explosion timer to show explosion for 1 second
                explosionTimer.start();
                
                // Save score for current difficulty
                difficultyScores.put(currentDifficulty, score);
                updateHighScore(currentDifficulty, score);
                saveHighScores();
                break;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing for smoother text
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Draw background
        g2d.drawImage(backgroundImg, 0, 0, null);

        if (showLevelSelect) {
            drawLevelSelect(g2d);
            return;
        }

        // Draw game objects
        if (!gameOver || showingExplosion) {
            // Draw bullets
            for (Bullet bullet : bullets) {
                bullet.draw(g2d);
            }
            
            // Draw plane or explosion
            if (showingExplosion) {
                // Draw explosion at plane's position
                g2d.drawImage(explosionImg, plane.getX(), plane.getY(), null);
            } else {
                plane.draw(g2d);
            }
        }

        // Draw score and difficulty with better styling
        drawGameHUD(g2d);

        if (isPaused) {
            drawPauseScreen(g2d);
        } else if (gameOver && !showingExplosion) {
            drawGameOverScreen(g2d);
        } else if (levelCompleted) {
            drawLevelCompletedScreen(g2d);
        }
    }
    
    private void drawGameHUD(Graphics2D g2d) {
        // Draw semi-transparent panel for score
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(10, 10, 200, 80, 10, 10);
        
        // Draw score with shadow effect
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.drawString("Score: " + score, 22, 42);
        
        // Get difficulty color
        Color diffColor;
        switch (currentDifficulty) {
            case EASY: diffColor = difficultyColors[0]; break;
            case MEDIUM: diffColor = difficultyColors[1]; break;
            case HARD: diffColor = difficultyColors[2]; break;
            default: diffColor = Color.WHITE;
        }
        
        g2d.setColor(Color.WHITE);
        g2d.drawString("Score: " + score, 20, 40);
        
        g2d.setColor(diffColor);
        g2d.drawString("Level: " + currentDifficulty, 20, 70);
        
        // Draw high scores in a panel on the right
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRoundRect(WIDTH - 210, 10, 200, 100, 10, 10);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(Color.WHITE);
        g2d.drawString("High Scores:", WIDTH - 190, 35);
        
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        int yPos = 60;
        for (Difficulty diff : Difficulty.values()) {
            switch (diff) {
                case EASY: g2d.setColor(difficultyColors[0]); break;
                case MEDIUM: g2d.setColor(difficultyColors[1]); break;
                case HARD: g2d.setColor(difficultyColors[2]); break;
            }
            g2d.drawString(diff + ": " + highScores.getOrDefault(diff, 0), WIDTH - 190, yPos);
            yPos += 25;
        }
        
        // Draw controls reminder at bottom
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("P: Pause | M: Toggle Mouse Control | ESC: Menu", 20, HEIGHT - 20);
    }
    
    private void drawPauseScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw panel
        g2d.setColor(new Color(30, 30, 60));
        g2d.fillRoundRect(WIDTH/2 - 200, HEIGHT/2 - 150, 400, 300, 20, 20);
        g2d.setColor(new Color(60, 60, 120));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(WIDTH/2 - 200, HEIGHT/2 - 150, 400, 300, 20, 20);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(Color.WHITE);
        String pauseText = "GAME PAUSED";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(pauseText);
        g2d.drawString(pauseText, (WIDTH - textWidth) / 2, HEIGHT / 2 - 70);
        
        // Draw buttons
        drawButton(g2d, "Resume Game (P)", WIDTH/2 - 150, HEIGHT/2 - 20, 300, 50, true);
        drawButton(g2d, "Level Select (ESC)", WIDTH/2 - 150, HEIGHT/2 + 50, 300, 50, false);
        drawButton(g2d, "Toggle Mouse: " + (mouseControlEnabled ? "ON" : "OFF") + " (M)", 
                  WIDTH/2 - 150, HEIGHT/2 + 120, 300, 50, false);
    }
    
    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw panel
        g2d.setColor(new Color(60, 30, 30));
        g2d.fillRoundRect(WIDTH/2 - 200, HEIGHT/2 - 200, 400, 400, 20, 20);
        g2d.setColor(new Color(120, 60, 60));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(WIDTH/2 - 200, HEIGHT/2 - 200, 400, 400, 20, 20);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(Color.RED);
        String gameOverText = "GAME OVER";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(gameOverText);
        g2d.drawString(gameOverText, (WIDTH - textWidth) / 2, HEIGHT / 2 - 120);

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        String scoreText = "Your score: " + score;
        textWidth = g2d.getFontMetrics().stringWidth(scoreText);
        g2d.drawString(scoreText, (WIDTH - textWidth) / 2, HEIGHT / 2 - 70);
        
        // Draw buttons
        drawButton(g2d, "Restart Game (R)", WIDTH/2 - 150, HEIGHT/2 - 20, 300, 50, true);
        drawButton(g2d, "Level Select (L)", WIDTH/2 - 150, HEIGHT/2 + 50, 300, 50, false);
        drawButton(g2d, "Toggle Mouse: " + (mouseControlEnabled ? "ON" : "OFF") + " (M)", 
                  WIDTH/2 - 150, HEIGHT/2 + 120, 300, 50, false);
    }
    
    private void drawLevelCompletedScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);
        
        // Draw panel
        g2d.setColor(new Color(30, 60, 30));
        g2d.fillRoundRect(WIDTH/2 - 200, HEIGHT/2 - 200, 400, 400, 20, 20);
        g2d.setColor(new Color(60, 120, 60));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(WIDTH/2 - 200, HEIGHT/2 - 200, 400, 400, 20, 20);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        g2d.setColor(new Color(100, 255, 100));
        String completedText = "LEVEL COMPLETED!";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(completedText);
        g2d.drawString(completedText, (WIDTH - textWidth) / 2, HEIGHT / 2 - 120);

        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);
        String scoreText = "Your score: " + score;
        textWidth = g2d.getFontMetrics().stringWidth(scoreText);
        g2d.drawString(scoreText, (WIDTH - textWidth) / 2, HEIGHT / 2 - 70);
        
        // Draw buttons
        drawButton(g2d, "Next Level (N)", WIDTH/2 - 150, HEIGHT/2 - 20, 300, 50, true);
        drawButton(g2d, "Restart Level (R)", WIDTH/2 - 150, HEIGHT/2 + 50, 300, 50, false);
        drawButton(g2d, "Level Select (L)", WIDTH/2 - 150, HEIGHT/2 + 120, 300, 50, false);
    }
    private void drawLevelSelect(Graphics2D g2d) {
        // Draw title
        g2d.setFont(new Font("Arial", Font.BOLD, 50));
        g2d.setColor(Color.WHITE);
        String titleText = "PLANE DODGE";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(titleText);
        g2d.drawString(titleText, (WIDTH - textWidth) / 2, 80);
        
        g2d.setFont(new Font("Arial", Font.BOLD, 30));
        String subtitleText = "SELECT DIFFICULTY";
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(subtitleText);
        g2d.drawString(subtitleText, (WIDTH - textWidth) / 2, 130);
        
        // Draw level buttons
        Difficulty[] difficulties = Difficulty.values();
        for (int i = 0; i < difficulties.length; i++) {
            boolean isSelected = i == selectedLevelIndex;
            Color buttonColor = difficultyColors[i];
            boolean isHovered = levelButtonHover[i];
            
            // Draw button
            drawDifficultyButton(g2d, difficulties[i].toString(), 
                              levelButtons[i].x, levelButtons[i].y, 
                              levelButtons[i].width, levelButtons[i].height, 
                              buttonColor, isSelected, isHovered);
            
            // Draw high score for this level
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            g2d.setColor(Color.WHITE);
            String highScoreText = "High Score: " + highScores.getOrDefault(difficulties[i], 0);
            g2d.drawString(highScoreText, levelButtons[i].x + levelButtons[i].width + 20, 
                          levelButtons[i].y + levelButtons[i].height/2 + 5);
        }
        
        // Draw start button with hover effect
        drawButton(g2d, "START GAME", startButton.x, startButton.y, startButton.width, startButton.height, true, startButtonHover);
        
        // Draw click instructions - FIXED POSITIONING TO POINT TO START BUTTON
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(Color.YELLOW);
        String clickText = "CLICK HERE TO START THE GAME";
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(clickText);
        
        // Position text above the start button
        int textY = startButton.y - 30;
        g2d.drawString(clickText, (WIDTH - textWidth) / 2, textY);
        
        // Draw arrow pointing to start button (not HARD button)
        int arrowX = startButton.x + startButton.width/2;
        int arrowY = startButton.y - 5; // Just above the start button
        int arrowSize = 15;
        
        // Draw a filled triangle pointing down to the start button
        int[] xPoints = {arrowX, arrowX - arrowSize, arrowX + arrowSize};
        int[] yPoints = {arrowY, arrowY - arrowSize, arrowY - arrowSize};
        g2d.fillPolygon(xPoints, yPoints, 3);
        
        // Add a pulsing effect to make it more noticeable
        long currentTime = System.currentTimeMillis();
        float pulse = (float)(Math.sin(currentTime / 300.0) * 0.2 + 0.8); // Value between 0.6 and 1.0
        
        // Draw a glowing outline around the start button
        g2d.setStroke(new BasicStroke(3));
        g2d.setColor(new Color(1.0f, 1.0f, 0.0f, pulse));
        g2d.drawRoundRect(startButton.x - 5, startButton.y - 5, 
                         startButton.width + 10, startButton.height + 10, 15, 15);
        
        // Draw keyboard instructions
        g2d.setFont(new Font("Arial", Font.PLAIN, 16));
        g2d.setColor(Color.WHITE);
        String instructionText = "Use UP/DOWN arrows to select, ENTER to start";
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(instructionText);
        g2d.drawString(instructionText, (WIDTH - textWidth) / 2, HEIGHT - 80);
        
        String mouseText = "Or click on your selection with the mouse";
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(mouseText);
        g2d.drawString(mouseText, (WIDTH - textWidth) / 2, HEIGHT - 50);
        
        // Draw troubleshooting info
        g2d.setFont(new Font("Arial", Font.ITALIC, 14));
        g2d.setColor(new Color(255, 255, 255, 180));
        String troubleText = "If keyboard controls don't work, click on the game window first";
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(troubleText);
        g2d.drawString(troubleText, (WIDTH - textWidth) / 2, HEIGHT - 20);
    }
    private void drawButton(Graphics2D g2d, String text, int x, int y, int width, int height, boolean primary) {
        drawButton(g2d, text, x, y, width, height, primary, false);
    }
    
    private void drawButton(Graphics2D g2d, String text, int x, int y, int width, int height, boolean primary, boolean hover) {
        // Draw button background
        Color bgColor = primary ? new Color(70, 130, 180) : new Color(70, 70, 100);
        Color hoverColor = primary ? new Color(90, 150, 200) : new Color(90, 90, 120);
        Color borderColor = primary ? new Color(100, 160, 210) : new Color(100, 100, 140);
        
        g2d.setColor(hover ? hoverColor : bgColor);
        g2d.fillRoundRect(x, y, width, height, 10, 10);
        
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(borderColor);
        g2d.drawRoundRect(x, y, width, height, 10, 10);
        
        // Draw text
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g2d.drawString(text, x + (width - textWidth) / 2, y + height / 2 + fm.getAscent() / 2);
    }
    
    private void drawDifficultyButton(Graphics2D g2d, String text, int x, int y, int width, int height, 
                                    Color color, boolean selected, boolean hover) {
        // Draw button background with difficulty color
        Color bgColor = selected ? color : new Color(color.getRed()/2, color.getGreen()/2, color.getBlue()/2);
        Color hoverColor = new Color(
            Math.min(255, bgColor.getRed() + 30),
            Math.min(255, bgColor.getGreen() + 30),
            Math.min(255, bgColor.getBlue() + 30)
        );
        Color borderColor = color;
        
        g2d.setColor(hover ? hoverColor : bgColor);
        g2d.fillRoundRect(x, y, width, height, 10, 10);
        
        // Draw border (thicker if selected)
        g2d.setStroke(new BasicStroke(selected ? 4 : 2));
        g2d.setColor(borderColor);
        g2d.drawRoundRect(x, y, width, height, 10, 10);
        
        // Draw text
        g2d.setFont(new Font("Arial", Font.BOLD, 22));
        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        g2d.drawString(text, x + (width - textWidth) / 2, y + height / 2 + fm.getAscent() / 2);
        
        // Draw selection indicator
        if (selected) {
            g2d.setColor(Color.WHITE);
            g2d.fillOval(x + 15, y + height/2 - 5, 10, 10);
        }
    }
    
    @Override
    public void mouseClicked(MouseEvent e) {
        handleMouseClick(e.getPoint());
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        // Request focus when mouse is pressed to ensure keyboard input works
        requestFocusInWindow();
    }
    
    @Override
    public void mouseReleased(MouseEvent e) {
        // Not used
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        // Not used
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        // Not used
    }
    
    private void handleMouseClick(Point point) {
        // Request focus to ensure keyboard input works
        requestFocusInWindow();
        
        if (showLevelSelect) {
            // Check if any level button was clicked
            for (int i = 0; i < levelButtons.length; i++) {
                if (levelButtons[i].contains(point)) {
                    selectedLevelIndex = i;
                    currentDifficulty = Difficulty.values()[i];
                    repaint();
                    return;
                }
            }
            
            // Check if start button was clicked
            if (startButton.contains(point)) {
                showLevelSelect = false;
                resetGame();
                return;
            }
        } else if (isPaused) {
            // Handle clicks in pause menu
            Rectangle resumeButton = new Rectangle(WIDTH/2 - 150, HEIGHT/2 - 20, 300, 50);
            Rectangle menuButton = new Rectangle(WIDTH/2 - 150, HEIGHT/2 + 50, 300, 50);
            Rectangle mouseToggleButton = new Rectangle(WIDTH/2 - 150, HEIGHT/2 + 120, 300, 50);
            
            if (resumeButton.contains(point)) {
                isPaused = false;
            } else if (menuButton.contains(point)) {
                showLevelSelect = true;
            } else if (mouseToggleButton.contains(point)) {
                mouseControlEnabled = !mouseControlEnabled;
                repaint();
            }
        } else if (gameOver && !showingExplosion) {
            // Handle clicks in game over screen
            Rectangle restartButton = new Rectangle(WIDTH/2 - 150, HEIGHT/2 - 20, 300, 50);
            Rectangle menuButton = new Rectangle(WIDTH/2 - 150, HEIGHT/2 + 50, 300, 50);
            Rectangle mouseToggleButton = new Rectangle(WIDTH/2 - 150, HEIGHT/2 + 120, 300, 50);
            
            if (restartButton.contains(point)) {
                resetGame();
            } else if (menuButton.contains(point)) {
                showLevelSelect = true;
            } else if (mouseToggleButton.contains(point)) {
                mouseControlEnabled = !mouseControlEnabled;
                repaint();
            }
        } else if (levelCompleted) {
            // Handle clicks in level completed screen
            Rectangle nextLevelButton = new Rectangle(WIDTH/2 - 150, HEIGHT/2 - 20, 300, 50);
            Rectangle restartButton = new Rectangle(WIDTH/2 - 150, HEIGHT/2 + 50, 300, 50);
            Rectangle menuButton = new Rectangle(WIDTH/2 - 150, HEIGHT/2 + 120, 300, 50);
            
            if (nextLevelButton.contains(point)) {
                advanceToNextLevel();
            } else if (restartButton.contains(point)) {
                resetGame();
            } else if (menuButton.contains(point)) {
                showLevelSelect = true;
            }
        }
    }
    
    @Override
    public void mouseMoved(MouseEvent e) {
        Point point = e.getPoint();
        
        if (showLevelSelect) {
            // Update hover states for level buttons
            boolean changed = false;
            for (int i = 0; i < levelButtons.length; i++) {
                boolean newHover = levelButtons[i].contains(point);
                if (levelButtonHover[i] != newHover) {
                    levelButtonHover[i] = newHover;
                    changed = true;
                }
            }
            
            // Update hover state for start button
            boolean newStartHover = startButton.contains(point);
            if (startButtonHover != newStartHover) {
                startButtonHover = newStartHover;
                changed = true;
            }
            
            // Only repaint if hover state changed
            if (changed) {
                repaint();
            }
        } else if (mouseControlEnabled && !gameOver && !isPaused && !showLevelSelect && !levelCompleted && !showingExplosion) {
            // Calculate the desired velocity based on mouse position
            int targetY = e.getY() - plane.getHeight() / 2;
            int targetX = e.getX() - plane.getWidth() / 2;

            // Smoothly adjust the plane's position towards the target
            double diffY = targetY - plane.getY();
            double diffX = targetX - plane.getX();

            // Apply acceleration towards the target position
            plane.setVelocityY(diffY * acceleration);
            plane.setVelocityX(diffX * acceleration);

            // Limit the velocity to prevent excessive speed
            plane.setVelocityY(Math.max(Math.min(plane.getVelocityY(), maxVelocity), -maxVelocity));
            plane.setVelocityX(Math.max(Math.min(plane.getVelocityX(), maxVelocity), -maxVelocity));
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    private void resetGame() {
        // Reset game state with current difficulty
        plane = new Plane(100, HEIGHT / 2, planeImg);
        bullets.clear();
        gameOver = false;
        levelCompleted = false;
        showingExplosion = false;
        score = 0;
        timer.setDelay(20);
        mouseControlEnabled = true;
        
        // Apply difficulty settings
        applyDifficultySettings();
        
        // Request focus to ensure keyboard input works
        requestFocusInWindow();
    }
    
    private void advanceToNextLevel() {
        // Move to next difficulty level
        if (currentDifficulty == Difficulty.EASY) {
            currentDifficulty = Difficulty.MEDIUM;
            selectedLevelIndex = 1;
        } else if (currentDifficulty == Difficulty.MEDIUM) {
            currentDifficulty = Difficulty.HARD;
            selectedLevelIndex = 2;
        }
        
        resetGame();
    }
    
    private void updateHighScore(Difficulty difficulty, int newScore) {
        int currentHighScore = highScores.getOrDefault(difficulty, 0);
        if (newScore > currentHighScore) {
            highScores.put(difficulty, newScore);
        }
    }
    
    private void loadHighScores() {
        // Initialize default high scores
        for (Difficulty diff : Difficulty.values()) {
            highScores.put(diff, 0);
        }
        
        try {
            File file = new File(SCORES_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        try {
                            Difficulty diff = Difficulty.valueOf(parts[0]);
                            int score = Integer.parseInt(parts[1]);
                            highScores.put(diff, score);
                        } catch (IllegalArgumentException e) {
                            System.out.println("Invalid difficulty in scores file: " + parts[0]);
                        }
                    }
                }
                reader.close();
            }
        } catch (IOException e) {
            System.out.println("Error loading high scores: " + e.getMessage());
        }
    }
    
    private void saveHighScores() {
        try {
            FileWriter writer = new FileWriter(SCORES_FILE);
            for (Map.Entry<Difficulty, Integer> entry : highScores.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue() + "\n");
            }
            writer.close();
        } catch (IOException e) {
            System.out.println("Error saving high scores: " + e.getMessage());
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        
        if (showLevelSelect) {
            handleLevelSelectInput(key);
            return;
        }
        
        if (isPaused) {
            if (key == KeyEvent.VK_P) {
                isPaused = false;
            } else if (key == KeyEvent.VK_ESCAPE) {
                showLevelSelect = true;
            } else if (key == KeyEvent.VK_M) {
                mouseControlEnabled = !mouseControlEnabled;
                repaint();
            }
            return;
        }

        if (gameOver && !showingExplosion) {
            if (key == KeyEvent.VK_R) {
                resetGame();
            } else if (key == KeyEvent.VK_L || key == KeyEvent.VK_ESCAPE) {
                showLevelSelect = true;
            } else if (key == KeyEvent.VK_M) {
                mouseControlEnabled = !mouseControlEnabled;
                repaint();
            }
        } else if (levelCompleted) {
            if (key == KeyEvent.VK_N) {
                advanceToNextLevel();
            } else if (key == KeyEvent.VK_R) {
                resetGame();
            } else if (key == KeyEvent.VK_L || key == KeyEvent.VK_ESCAPE) {
                showLevelSelect = true;
            }
        } else if (!showingExplosion) {
            if (key == KeyEvent.VK_P) {
                isPaused = true;
            } else if (key == KeyEvent.VK_ESCAPE) {
                showLevelSelect = true;
            } else if (key == KeyEvent.VK_M) {
                mouseControlEnabled = !mouseControlEnabled;
                repaint();
            }

            if (!mouseControlEnabled) {
                if (key == KeyEvent.VK_UP) {
                    plane.setVelocityY(Math.max(plane.getVelocityY() - acceleration, -maxVelocity));
                } else if (key == KeyEvent.VK_DOWN) {
                    plane.setVelocityY(Math.min(plane.getVelocityY() + acceleration, maxVelocity));
                } else if (key == KeyEvent.VK_LEFT) {
                    plane.setVelocityX(Math.max(plane.getVelocityX() - acceleration, -maxVelocity));
                } else if (key == KeyEvent.VK_RIGHT) {
                    plane.setVelocityX(Math.min(plane.getVelocityX() + acceleration, maxVelocity));
                }
            }
        }
    }
    
    private void handleLevelSelectInput(int key) {
        if (key == KeyEvent.VK_UP) {
            // Move selection up
            selectedLevelIndex = Math.max(0, selectedLevelIndex - 1);
            currentDifficulty = Difficulty.values()[selectedLevelIndex];
            repaint();
        } else if (key == KeyEvent.VK_DOWN) {
            // Move selection down
            selectedLevelIndex = Math.min(2, selectedLevelIndex + 1);
            currentDifficulty = Difficulty.values()[selectedLevelIndex];
            repaint();
        } else if (key == KeyEvent.VK_ENTER) {
            // Start game with selected difficulty
            showLevelSelect = false;
            resetGame();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Handled in updatePlane
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    public static void main(String[] args) {
        System.out.println("Game class loaded successfully");
        System.out.println("Working directory: " + System.getProperty("user.dir"));

        // Set up the game window
        JFrame frame = new JFrame("Plane Dodge Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        Game game = new Game();
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        
        // Request focus after frame is visible
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                game.requestFocusInWindow();
            }
        });
    }
}
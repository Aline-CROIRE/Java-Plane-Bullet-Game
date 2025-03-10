import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class Game extends JPanel implements ActionListener, KeyListener, MouseMotionListener {
    // Window dimensions
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    // Game objects
    private Plane plane;
    private ArrayList<Bullet> bullets;
    private Timer timer;
    private Random random;
    private boolean gameOver;
    private int score;

    // Graphics
    private BufferedImage planeImg;
    private BufferedImage bulletImg;
    private BufferedImage explosionImg;

    // Sound
    private Clip explosionSound;

    // Mouse control
    private boolean mouseControlEnabled = true; // Added option to enable/disable mouse

    public Game() {
        // Initialize panel
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);  // Added MouseMotionListener

        // Load resources
        loadImages();
        loadSounds();

        // Initialize game objects
        plane = new Plane(100, HEIGHT / 2, planeImg);
        bullets = new ArrayList<>();
        random = new Random();
        gameOver = false;
        score = 0;

        // Start game loop
        timer = new Timer(20, this); // 50 FPS
        timer.start();
    }

    private void loadImages() {
        try {
            // Load images using file paths matching your project structure
            BufferedImage originalPlaneImg = ImageIO.read(new File("src/Resources/plane.png"));
            BufferedImage originalBulletImg = ImageIO.read(new File("src/Resources/bullet.png"));
            BufferedImage originalExplosionImg = ImageIO.read(new File("src/Resources/exploision.png"));

            // Scale down images to appropriate sizes
            planeImg = scaleImage(originalPlaneImg, 200, 100);
            bulletImg = scaleImage(originalBulletImg, 60, 30);
            explosionImg = scaleImage(originalExplosionImg, 100, 100);

            System.out.println("Images loaded successfully");
            System.out.println("Original plane image dimensions: " + originalPlaneImg.getWidth() + "x" + originalPlaneImg.getHeight());
            System.out.println("Scaled plane image dimensions: " + planeImg.getWidth() + "x" + planeImg.getHeight());
            System.out.println("Original bullet image dimensions: " + originalBulletImg.getWidth() + "x" + originalBulletImg.getHeight());
            System.out.println("Scaled bullet image dimensions: " + bulletImg.getWidth() + "x" + bulletImg.getHeight());

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Failed to load images, using placeholders");
            // Create placeholder images if loading fails
            planeImg = createPlaceholderImage(100, 50, Color.BLUE);
            bulletImg = createPlaceholderImage(30, 15, Color.RED);
            explosionImg = createPlaceholderImage(100, 100, Color.ORANGE);
        }
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
                    16,                  // 16 bit instead of 24 bit
                    1,                   // mono instead of stereo
                    2,                   // frame size (bytes)
                    baseFormat.getSampleRate(),
                    false                // little endian
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
        if (!gameOver) {
            // Update game state
            updatePlane();
            updateBullets();
            checkCollisions();
            spawnBullets();
            score++;
        }
        // Redraw the screen
        repaint();
    }

    private void updatePlane() {
        // Update plane position based on its velocity
        plane.update();

        // Keep plane within screen bounds
        if (plane.getY() < 0) {
            plane.setY(0);
        } else if (plane.getY() > HEIGHT - plane.getHeight()) {
            plane.setY(HEIGHT - plane.getHeight());
        }
        if (plane.getX() < 0) {
            plane.setX(0);
        } else if (plane.getX() > WIDTH - plane.getWidth()) {
            plane.setX(WIDTH - plane.getWidth());
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
        if (maxY <= 0) maxY = HEIGHT - 1; // Fallback if bullet is too tall

        // Randomly spawn new bullets from the right side of the screen
        if (random.nextInt(100) < 5) { // 5% chance each frame
            int y = random.nextInt(maxY);
            bullets.add(new Bullet(WIDTH, y, bulletImg));
        }
    }

    private void checkCollisions() {
        // Check for collisions between plane and bullets
        Rectangle planeRect = plane.getBounds();

        for (Bullet bullet : bullets) {
            if (planeRect.intersects(bullet.getBounds())) {
                gameOver = true;
                plane.setExploding(true);
                playExplosionSound();
                timer.setDelay(50); // Slow down game for explosion effect
                break;
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw game objects
        if (!gameOver || plane.isExploding()) {
            // Draw plane (or explosion if game over)
            if (plane.isExploding()) {
                g2d.drawImage(explosionImg, plane.getX(), plane.getY(), null);
            } else {
                plane.draw(g2d);
            }

            // Draw bullets
            for (Bullet bullet : bullets) {
                bullet.draw(g2d);
            }
        }

        // Draw score
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.drawString("Score: " + score, 20, 30);

        // Draw game over message
        if (gameOver) {
            g2d.setFont(new Font("Arial", Font.BOLD, 40));
            g2d.setColor(Color.RED);
            String gameOverText = "GAME OVER";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(gameOverText);
            g2d.drawString(gameOverText, (WIDTH - textWidth) / 2, HEIGHT / 2);

            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            String restartText = "Press R to restart";
            textWidth = g2d.getFontMetrics().stringWidth(restartText);
            g2d.drawString(restartText, (WIDTH - textWidth) / 2, HEIGHT / 2 + 40);

            String mouseControlText = "Press M to toggle mouse control";
            textWidth = g2d.getFontMetrics().stringWidth(mouseControlText);
            g2d.drawString(mouseControlText, (WIDTH - textWidth) / 2, HEIGHT / 2 + 80);
        } else {
            // Display mouse control option during the game
            g2d.setFont(new Font("Arial", Font.PLAIN, 12));
            g2d.setColor(Color.WHITE);
            String mouseControlText = "Press M to toggle mouse control";
            g2d.drawString(mouseControlText, 10, HEIGHT - 20);
        }
    }

    private void resetGame() {
        // Reset game state
        plane = new Plane(100, HEIGHT / 2, planeImg);
        bullets.clear();
        gameOver = false;
        score = 0;
        timer.setDelay(20); // Reset to normal speed
        mouseControlEnabled = true; // Reset mouse control to default
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        if (gameOver) {
            // Restart game if R is pressed
            if (key == KeyEvent.VK_R) {
                resetGame();
            } else if (key == KeyEvent.VK_M) {
                // You can also allow toggling mouse control even when game is over
                mouseControlEnabled = !mouseControlEnabled;
            }

        } else {
            if (key == KeyEvent.VK_M) {
                mouseControlEnabled = !mouseControlEnabled;
            }

            if(!mouseControlEnabled){ //only moves if not mouse control
                if (key == KeyEvent.VK_UP) {
                    plane.setVelocityY(-5);
                } else if (key == KeyEvent.VK_DOWN) {
                    plane.setVelocityY(5);
                } else if (key == KeyEvent.VK_LEFT) {
                    plane.setVelocityX(-5);
                } else if (key == KeyEvent.VK_RIGHT) {
                    plane.setVelocityX(5);
                }
            }


        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();

        if(!mouseControlEnabled){
            // Stop plane movement when keys are released
            if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
                plane.setVelocityY(0);
            }
            if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT) {
                plane.setVelocityX(0);
            }
        }

    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (mouseControlEnabled && !gameOver) {
            plane.setY(e.getY() - plane.getHeight() / 2); // center the plane on the mouse y
            plane.setX(e.getX() - plane.getWidth() / 2);   // center the plane on the mouse x

            // Keep plane within screen bounds - same as updatePlane()
            if (plane.getY() < 0) {
                plane.setY(0);
            } else if (plane.getY() > HEIGHT - plane.getHeight()) {
                plane.setY(HEIGHT - plane.getHeight());
            }
            if (plane.getX() < 0) {
                plane.setX(0);
            } else if (plane.getX() > WIDTH - plane.getWidth()) {
                plane.setX(WIDTH - plane.getWidth());
            }
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // You could potentially add mouse dragging functionality here,
        // but it's not typically needed for this kind of game.
        // For now, we'll just treat it the same as mouseMoved.
        mouseMoved(e);
    }

    public static void main(String[] args) {
        System.out.println("Game class loaded successfully");
        System.out.println("Working directory: " + System.getProperty("user.dir"));

        // Set up the game window
        JFrame frame = new JFrame("Plane Dodge Game");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(new Game());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
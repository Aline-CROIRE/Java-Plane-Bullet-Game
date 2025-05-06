import java.awt.*;
import java.awt.image.BufferedImage;

public class Bullet {
    private int x, y;
    private int velocityX;
    private BufferedImage image;

    public Bullet(int x, int y, BufferedImage image) {
        this.x = x;
        this.y = y;
        this.image = image;
        this.velocityX = -8; // Default bullet speed, can be modified
    }

    public void update() {
        // Update position based on velocity
        x += velocityX;
    }

    public void draw(Graphics2D g) {
        // Draw the bullet image
        g.drawImage(image, x, y, null);
    }

    public Rectangle getBounds() {
        // Return the bounding rectangle for collision detection
        return new Rectangle(x, y, image.getWidth(), image.getHeight());
    }

    // Getters and setters
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public int getVelocityX() {
        return velocityX;
    }

    public void setVelocityX(int velocityX) {
        this.velocityX = velocityX;
    }
}
import java.awt.*;
import java.awt.image.BufferedImage;

public class Plane {
    private int x, y;
    private int velocityX; // Added horizontal velocity
    private int velocityY;
    private BufferedImage image;
    private boolean exploding;

    public Plane(int x, int y, BufferedImage image) {
        this.x = x;
        this.y = y;
        this.image = image;
        this.velocityY = 0;
        this.velocityX = 0;  // Initialize horizontal velocity
        this.exploding = false;
    }

    public void update() {
        // Update position based on velocity
        y += velocityY;
        x += velocityX; // Update x position based on horizontal velocity
    }

    public void draw(Graphics2D g) {
        // Draw the plane image
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

    public void setX(int x) {
        this.x = x;
    }


    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getVelocityX() {
        return velocityX;
    }

    public void setVelocityX(int velocityX) {
        this.velocityX = velocityX;
    }


    public int getWidth() {
        return image.getWidth();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public void setVelocityY(int velocityY) {
        this.velocityY = velocityY;
    }

    public boolean isExploding() {
        return exploding;
    }

    public void setExploding(boolean exploding) {
        this.exploding = exploding;
    }
}
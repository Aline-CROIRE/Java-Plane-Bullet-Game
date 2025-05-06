import java.awt.*;
import java.awt.image.BufferedImage;

public class Plane {
    private int x, y;
    private double velocityX, velocityY;
    private BufferedImage image;
    private boolean exploding;
    
    public Plane(int x, int y, BufferedImage image) {
        this.x = x;
        this.y = y;
        this.image = image;
        this.velocityX = 0;
        this.velocityY = 0;
        this.exploding = false;
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
    
    public double getVelocityX() {
        return velocityX;
    }
    
    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }
    
    public double getVelocityY() {
        return velocityY;
    }
    
    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }
    
    public int getWidth() {
        return image.getWidth();
    }
    
    public int getHeight() {
        return image.getHeight();
    }
    
    public boolean isExploding() {
        return exploding;
    }
    
    public void setExploding(boolean exploding) {
        this.exploding = exploding;
    }
}
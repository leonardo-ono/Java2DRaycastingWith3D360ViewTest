import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * 2D Raycasting with 360 View Test
 * 
 * @author Leonardo Ono (ono.leo80@gmail.com)
 */
public class View extends JPanel {

    private BufferedImage map;
    private int wallColor;
    private final Rectangle renderArea = new Rectangle(-128, -128, 256, 256);
    private final AffineTransform at = new AffineTransform();
    
    private final boolean keys[] = new boolean[256];
    private boolean mousePressed;
    private int mouseX;
    private int mouseY;
    private int mousePressedX;
    private int mousePressedY;
    
    private double playerX = 200;
    private double playerY = 150;
    private double playerAngleY = 0;
    private double playerAngleX = 0;
    private double playerPressedAngleY = 0;
    private double playerPressedAngleX = 0;
    
    public void start() {
        try {
            map = ImageIO.read(getClass().getResourceAsStream("map.png"));
            wallColor = map.getRGB(0, 0);
        } catch (IOException ex) {
            Logger.getLogger(View.class.getName()).log(Level.SEVERE, null, ex);
            System.exit(-1);
        }
        addKeyListener(new KeyHandler());
        addMouseListener(new MouseHandler());
        addMouseMotionListener((MouseMotionListener) getMouseListeners()[0]);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 

        update();

        Graphics2D g2d = (Graphics2D) g;
        
        g.drawString("W,A,S and D keys - move camera", 20, 350);
        g.drawString("Mouse drag - rotate camera", 20, 370);
        
        g.drawImage(map, 0, 0, null);
        
        drawPlayer(g2d);
        
        g2d.translate(500, 280);
        g2d.scale(2, 2);
        g2d.setColor(Color.BLUE);
        g2d.draw(renderArea);
        g2d.setClip(renderArea);
        
        drawWalls3D(g2d, playerAngleY, 0);
        drawWalls3D(g2d, playerAngleY-Math.toRadians(90), Math.toRadians(180));
        drawWalls3D(g2d, playerAngleY+Math.toRadians(90), Math.toRadians(180));
        drawWalls3D(g2d, playerAngleY+Math.toRadians(180), Math.toRadians(0));
        
        // please implement your own more decent game loop later :)
        try {
            Thread.sleep(1000 / 60);
        } catch (InterruptedException ex) { }
        repaint();
    }

    private void update() {
        if (mousePressed) {
            double targetAngleY = playerPressedAngleY 
                        + 0.1 * (mouseX - mousePressedX);
            
            double targetAngleX = playerPressedAngleX 
                        + 0.1 * (mouseY - mousePressedY);
            
            playerAngleY += 0.25 * (targetAngleY - playerAngleY);
            playerAngleX += 0.25 * (targetAngleX - playerAngleX);
            
            playerAngleX = playerAngleX < -Math.PI / 2 
                    ? -Math.PI / 2 : playerAngleX > Math.PI / 2 
                    ? Math.PI / 2 : playerAngleX;
        }
        
        if (keys[KeyEvent.VK_W]) {
            movePlayer(1, 0);
        }
        else if (keys[KeyEvent.VK_S]) {
            movePlayer(-1, 0);
        }        

        if (keys[KeyEvent.VK_A]) {
            movePlayer(-1, Math.toRadians(90));
        }
        else if (keys[KeyEvent.VK_D]) {
            movePlayer(1, Math.toRadians(90));
        }        
    }
    
    private void movePlayer(double speed, double angle) {
        playerX += speed * Math.cos(playerAngleY + angle);
        playerY += speed * Math.sin(playerAngleY + angle);
    }
    
    public void drawPlayer(Graphics2D g) {
        double dx = Math.cos(playerAngleY);
        double dy = Math.sin(playerAngleY);
        g.setColor(Color.RED);
        g.drawOval((int) (playerX - 3), (int) (playerY - 3), 6, 6);
        g.drawLine((int) (playerX), (int) (playerY)
                , (int) (playerX + 30 * dx), (int) (playerY + 30 * dy));
    }

    private void drawWalls3D(
            Graphics2D g, double direction, double offsetAngleY) {
        
        for (int x = -128; x < 128; x += 2) {
            double angle = Math.atan2(x, 128.0);
            double z = castRay(playerX, playerY
                    , angle + direction) * Math.cos(angle);
            if (z > 0) {
                drawWall3D(g, z, x, direction, offsetAngleY);
            }
        }
    }

    private void drawWall3D(Graphics2D g
            , double z, int x, double direction, double offsetAngleY) {
        
        double p = z / 255;
        p = p > 1 ? 1 : p < 0 ? 0 : p;
        int wallColor = (int) (p * 255);
        g.setColor(new Color(wallColor, wallColor, wallColor));
        
        int wallHeight = (int) (8 * 255 / z);
        double[] p1 = { x, 256 / 2 - wallHeight - 128, 128 };
        double[] p2 = { x, 256 / 2 + wallHeight - 128, 128 };

        double[] srcPtsY = { x, 128, x, 128 };
        at.setToIdentity();
        at.rotate(-playerAngleY + direction + offsetAngleY);
        at.transform(srcPtsY, 0, srcPtsY, 0, 2);
        
        double[] srcPtsX = { srcPtsY[1], p1[1], srcPtsY[3], p2[1] };
        at.setToIdentity();
        at.rotate(-playerAngleX);
        at.transform(srcPtsX, 0, srcPtsX, 0, 2);

        p1[0] = srcPtsY[0]; p1[1] = srcPtsX[1]; p1[2] = srcPtsX[0];
        p2[0] = srcPtsY[2]; p2[1] = srcPtsX[3]; p2[2] = srcPtsX[2];
        
        if (p1[2] <= 0 && p2[2] <= 0) {
            return;
        }
        else if (p1[2] <= 0) {
            clip(p2, p1);
        }
        else if (p2[2] <= 0) {
            clip(p1, p2);
        }
        
        int sx1 = (int) (128 * (p1[0] / p1[2]));
        int sy1 = (int) (128 * (p1[1] / p1[2]));
        int sx2 = (int) (128 * (p2[0] / p2[2]));
        int sy2 = (int) (128 * (p2[1] / p2[2]));
        g.drawLine(sx1, sy1, sx2, sy2);
    }
    
    private void clip(double[] p1, double[] p2) {
        double s = p1[2] / (p1[2] - p2[2]);
        p2[0] = p1[0] + s * (p2[0] - p1[0]);
        p2[1] = p1[1] + s * (p2[1] - p1[1]);
        p2[2] = 0.01;
    }
    
    private double castRay(double px, double py, double pa) {
        double s = Math.sin(pa);
        double c = Math.cos(pa);
        double d = 0;
        int currentColor = 0;
        do {
            d += 0.25;
            int tx = (int) (px + d * c);
            int ty = (int) (py + d * s);
            if (tx < 0 || ty < 0 
                    || tx > map.getWidth() - 1 || ty > map.getHeight() - 1) {
                
                return 0;
            }
            else {
                currentColor = map.getRGB(tx, ty);
            }
        } while (currentColor != wallColor);
        return d;
    }
        
    private class KeyHandler extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() < 256) keys[e.getKeyCode()] = true;
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() < 256) keys[e.getKeyCode()] = false;
        }
        
    }

    private class MouseHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
            mousePressedX = e.getX();
            mousePressedY = e.getY();
            playerPressedAngleY = playerAngleY;
            playerPressedAngleX = playerAngleX;
            mousePressed = true;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mousePressed = false;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();
        }
        
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                View view = new View();
                JFrame frame = new JFrame();
                frame.setTitle("Java 2D Raycasting with 360 View Test");
                frame.setSize(800, 600);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setLocationRelativeTo(null);
                frame.getContentPane().add(view);
                frame.setResizable(false);
                frame.setVisible(true);
                view.requestFocus();
                view.start();
            }
        });        
    }
    
}

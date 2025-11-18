import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int TILE_SIZE = 20;
    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;

    private Arraylist<Point> snake;
    private Point food;
    private int dirX = 1, dirY = 0;
    private int nextDirX = 1, nextDirY = 0;
    private Timer timer;
    private boolean gameOver = false;
    private Random random;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        snake = new ArrayList<>();
        snake.add(new Point(WIDTH / 2 / TITLE_SIZE, HEIGHT / 2 / TILE_SIZE));
        random = new Random();
        spawnFood();

        timer = new Timer(100, this);
        timer.start();
    }

    private void spawnFood() {
        food = new Point(random.nextInt(WIDTH / TILE_SIZE), random.nextInt(HEIGHT / TILE_SIZE));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            dirX = nextDirX;
            dirY = nextDirY;

            Point head = snake.get(0);
            Point newHead = new Point(head.x + dirX, head.y + dirY);

            if (newHead.x < 0 || newHead.x >= WIDTH / TILE_SIZE ||
            newHead.y < 0 || newHead.y >= HEIGHT / TILE_SIZE) {
                gameOver = true;
            }

            for (Point p : snake) {
                if (newHead.equals(p)) {
                    gameOver = true;
                }
            }

            snake.add(0, newHead);

            if (newHead.equals(food)) {
                spawnFood();
            } else {
                snake.remove(snake.size() - 1);
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.GREEN);
        for (Point p : snake) {
        g.fillRect(p.x * TITLE_SIZE, p.y * TITLE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        g.setColor(Color.RED);
        g.fillRect(food.x * TILE_SIZE, food.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        if (gameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.drawString("Game Over" + (snake.size() - 1), 100, 200);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {

    }
}

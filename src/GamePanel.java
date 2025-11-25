import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class GamePanel extends JPanel implements ActionListener, KeyListener {
    private static final int TILE_SIZE = 20;
    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;

    private ArrayList<Point> snake;
    private Point food;
    private int dirX = 1, dirY = 0;
    private int nextDirX = 1, nextDirY = 0;
    private Timer timer;
    private enum State = { MENU, RUNNING, PAUSED, GAME_OVER }
    private State state = State.MENU;
    private Random random;
    private int score;
    private int highScore;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);

        loadHighScore();
        initializeGame();

        timer = new Timer(200, this);
        timer.start();
    }

    private void initializeGame() {
        snake = new ArrayList<>();
        snake.add(new Point(WIDTH / 2 / TILE_SIZE, HEIGHT / 2 / TILE_SIZE));
        random = new Random();
        spawnFood();
        score = 0;
        dirX = 1;
        dirY = 0;
        nextDirX = 1;
        nextDirY = 0;
        state = State.MENU;
    }

    private void loadHighScore() {
        try {
            File file = new File("highscore.txt");
            if (file.exists()) {
                Scanner scanner = new Scanner(file);
                if (scanner.hasNextInt()) {
                    highScore = scanner.nextInt();
                }
                scanner.close();
            } else {
                highScore = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
            highScore = 0;
        }
    }

    private void saveHighScore() {
        try {
            PrintWriter writer = new PrintWriter("highscore.txt");
            writer.println(highScore);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkGameOver() {
        Point head = snake.get(0);
        if (head.x < 0 || head.x >= WIDTH / TILE_SIZE ||
        head.y < 0 || head.y >= HEIGHT / TILE_SIZE) {
            state = State.GAME_OVER;
        }

        for (int i = 1; i < snake.size(); i++) {
            if (head.equals(snake.get(i))) {
                state = State.GAME_OVER;
                break;
            }
        }

        if (state == State.GAME_OVER) {
            if (score > highScore) {
                highScore = score;
                saveHighScore();
            }
        }
    }

    private void spawnFood() {
        int cellsX = WIDTH / TITLE_SIZE;
        int cellsY = HEIGHT / TITLE_SIZE;
        Point p;
        while (true) {
            p = new Point(random.nextInt(cellsX), random.nextInt(cellsY));
            if (!snake.contains(p)) {
                break;
            }
        }
        food = p;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == State.RUNNING) {
            dirX = nextDirX;
            dirY = nextDirY;

            Point head = snake.get(0);
            Point newHead = new Point(head.x + dirX, head.y + dirY);

            snake.add(0, newHead);

            if (newHead.equals(food)) {
                score++;
                spawnFood();
            } else {
                snake.remove(snake.size() - 1);
            }
            checkGameOver();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        switch (state) {
            case MENU -> drawMenu(g);
            case RUNNING, PAUSED -> drawGame(g);
            case GAME_OVER -> drawGameOver(g);
        }
    }

    private void drawMenu(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 26));
        g.drawString("SNAKE", 150, 130);

        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Press SPACE to start", 80, 180);
        g.drawString("Controls: WASD", 70, 210);
        g.drawString("R - restart", 60, 240);

        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Лучший счёт: " + highScore, 10, HEIGHT - 20);
    }

    private void drawGame(Graphics g) {

    }

    private void restartGame() {
        initializeGame();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver) {
            if (e.getKeyCode() == KeyEvent.VK_R) {
                restartGame();
            }
            return;
        }

        switch (e.getKeyCode()) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                if (dirY == 0) { nextDirX = 0; nextDirY = -1; }
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                if (dirY == 0) { nextDirX = 0; nextDirY = 1;}
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                if (dirX == 0) { nextDirX = -1; nextDirY = 0;}
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                if (dirX == 0) { nextDirX = 1; nextDirY = 0;}
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
}

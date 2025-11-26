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
    // размер клетки и поля
    private static final int TILE_SIZE = 20;
    private static final int WIDTH = 400;
    private static final int HEIGHT = 400;

    // базовые скорости для режимов (мс на тик)
    private static final int SPEED_SLOW = 260;
    private static final int SPEED_NORMAL = 200;
    private static final int SPEED_FAST = 140;

    private static final int MIN_GAME_SPEED = 80; // минимальная скорость при ускорении

    // выбранный режим скорости: 0 - медленно, 1 - нормально, 2 - быстро
    private int speedMode = 1;
    private int baseSpeed = SPEED_NORMAL;
    private int currentDelay = baseSpeed;

    private ArrayList<Point> snake;
    private Point food;
    private int dirX = 1, dirY = 0;
    private int nextDirX = 1, nextDirY = 0;
    private Timer timer;

    private enum State { MENU, RUNNING, PAUSED, GAME_OVER }
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

        // создаём таймер, реальная задержка будет установлена при старте игры
        timer = new Timer(currentDelay, this);
        timer.start();
    }

    private void applySpeedMode() {
        switch (speedMode) {
            case 0:
                baseSpeed = SPEED_SLOW;
                break;
            case 2:
                baseSpeed = SPEED_FAST;
                break;
            case 1:
            default:
                baseSpeed = SPEED_NORMAL;
        }
        currentDelay = baseSpeed;
        if (timer != null) {
            timer.setDelay(currentDelay);
        }
    }

    private void initializeGame() {
        snake = new ArrayList<>();
        // голова в центре поля в координатах клеток
        snake.add(new Point(WIDTH / 2 / TILE_SIZE, HEIGHT / 2 / TILE_SIZE));
        if (random == null) {
            random = new Random();
        }
        spawnFood();
        score = 0;
        dirX = 1;
        dirY = 0;
        nextDirX = 1;
        nextDirY = 0;
        // при старте игры будем показывать меню
        state = State.MENU;
        // сбрасываем базовую скорость в соответствии с выбранным режимом
        applySpeedMode();
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
        int cellsX = WIDTH / TILE_SIZE;
        int cellsY = HEIGHT / TILE_SIZE;
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
            // меняем направление только один раз за тик
            dirX = nextDirX;
            dirY = nextDirY;

            Point head = snake.get(0);
            Point newHead = new Point(head.x + dirX, head.y + dirY);

            snake.add(0, newHead);

            if (newHead.equals(food)) {
                score++;
                spawnFood();

                // лёгкое ускорение: каждые 5 очков уменьшаем задержку на 10 мс от базовой скорости
                int newDelay = baseSpeed - (score / 5) * 10;
                if (newDelay < MIN_GAME_SPEED) {
                    newDelay = MIN_GAME_SPEED;
                }
                if (newDelay != currentDelay) {
                    currentDelay = newDelay;
                    timer.setDelay(currentDelay);
                }
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
            case MENU:
                drawMenu(g);
                break;
            case RUNNING:
            case PAUSED:
                drawGame(g);
                break;
            case GAME_OVER:
                drawGameOver(g);
                break;
        }
    }

    private void drawMenu(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 26));
        g.drawString("SNAKE", 150, 80);

        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Нажмите ПРОБЕЛ, чтобы начать", 40, 130);
        g.drawString("Управление: WASD или стрелки", 40, 160);
        g.drawString("R - перезапуск после смерти", 40, 190);

        // меню выбора скорости
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("Скорость:", 40, 240);

        g.setFont(new Font("Arial", Font.PLAIN, 14));
        // варианты: 0 - медленно, 1 - нормально, 2 - быстро
        String slowLabel = "1) Медленно";
        String normalLabel = "2) Нормально";
        String fastLabel = "3) Быстро";

        // подсветка выбранного режима
        g.setColor(speedMode == 0 ? Color.YELLOW : Color.WHITE);
        g.drawString(slowLabel, 60, 270);
        g.setColor(speedMode == 1 ? Color.YELLOW : Color.WHITE);
        g.drawString(normalLabel, 60, 295);
        g.setColor(speedMode == 2 ? Color.YELLOW : Color.WHITE);
        g.drawString(fastLabel, 60, 320);

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Меняйте скорость стрелками ВВЕРХ/ВНИЗ или клавишами 1/2/3", 20, HEIGHT - 20);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Лучший счёт: " + highScore, 10, 20);
    }

    private void drawGame(Graphics g) {
        // фон
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // лёгкая сетка
        g.setColor(new Color(40, 40, 40));
        for (int x = 0; x < WIDTH; x += TILE_SIZE) {
            g.drawLine(x, 0, x, HEIGHT);
        }
        for (int y = 0; y < HEIGHT; y += TILE_SIZE) {
            g.drawLine(0, y, WIDTH, y);
        }

        // еда
        g.setColor(Color.RED);
        g.fillOval(food.x * TILE_SIZE, food.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        // змея
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);
            if (i == 0) {
                // голова
                g.setColor(new Color(0, 200, 0));
            } else {
                // тело
                g.setColor(new Color(0, 120, 0));
            }
            g.fillRect(p.x * TILE_SIZE, p.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }

        // текущий счёт и рекорд сверху
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Счёт: " + score, 10, 20);
        g.drawString("Рекорд: " + highScore, WIDTH - 120, 20);

        // подсказки управления снизу
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.drawString("Управление: WASD / стрелки   |   Пауза: P или ESC   |   Рестарт: R", 10, HEIGHT - 10);

        if (state == State.PAUSED) {
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("ПАУЗА", WIDTH / 2 - 50, HEIGHT / 2);
        }
    }

    private void drawGameOver(Graphics g) {
        // показываем поле как во время игры
        drawGame(g);

        // затем поверх затемнение и текст
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("ИГРА ОКОНЧЕНА", 80, 150);

        g.setFont(new Font("Arial", Font.PLAIN, 16));
        g.drawString("Ваш счёт: " + score, 130, 190);
        g.drawString("Рекорд: " + highScore, 130, 220);
        g.drawString("Нажмите R, чтобы начать заново", 60, 260);
        g.drawString("Нажмите ПРОБЕЛ для меню", 80, 290);
    }

    private void restartGame() {
        initializeGame();
        // сразу запускаем игру после рестарта
        state = State.RUNNING;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();

        // в меню пробел запускает игру, стрелки и 1/2/3 меняют скорость
        if (state == State.MENU) {
            if (key == KeyEvent.VK_SPACE) {
                // перед стартом применяем выбранный режим скорости
                applySpeedMode();
                state = State.RUNNING;
                return;
            }

            // выбор скорости: стрелки ВВЕРХ/ВНИЗ
            if (key == KeyEvent.VK_UP) {
                speedMode--;
                if (speedMode < 0) speedMode = 2;
                applySpeedMode();
            } else if (key == KeyEvent.VK_DOWN) {
                speedMode++;
                if (speedMode > 2) speedMode = 0;
                applySpeedMode();
            }

            // выбор скорости: цифры 1, 2, 3
            if (key == KeyEvent.VK_1) {
                speedMode = 0;
                applySpeedMode();
            } else if (key == KeyEvent.VK_2) {
                speedMode = 1;
                applySpeedMode();
            } else if (key == KeyEvent.VK_3) {
                speedMode = 2;
                applySpeedMode();
            }
            return;
        }

        // на экране game over: R — рестарт, SPACE — назад в меню
        if (state == State.GAME_OVER) {
            if (key == KeyEvent.VK_R) {
                restartGame();
            } else if (key == KeyEvent.VK_SPACE) {
                initializeGame();
                state = State.MENU;
            }
            return;
        }

        // во время игры
        if (state == State.RUNNING || state == State.PAUSED) {
            // пауза по ESC или P
            if (key == KeyEvent.VK_ESCAPE || key == KeyEvent.VK_P) {
                state = (state == State.RUNNING) ? State.PAUSED : State.RUNNING;
                return;
            }

            // управление: стрелки и WASD
            switch (key) {
                case KeyEvent.VK_UP:
                case KeyEvent.VK_W:
                    if (dirY == 0) { nextDirX = 0; nextDirY = -1; }
                    break;
                case KeyEvent.VK_DOWN:
                case KeyEvent.VK_S:
                    if (dirY == 0) { nextDirX = 0; nextDirY = 1; }
                    break;
                case KeyEvent.VK_LEFT:
                case KeyEvent.VK_A:
                    if (dirX == 0) { nextDirX = -1; nextDirY = 0; }
                    break;
                case KeyEvent.VK_RIGHT:
                case KeyEvent.VK_D:
                    if (dirX == 0) { nextDirX = 1; nextDirY = 0; }
                    break;
                case KeyEvent.VK_R:
                    // перезапуск прямо из игры
                    restartGame();
                    break;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) { }

    @Override
    public void keyTyped(KeyEvent e) { }
}

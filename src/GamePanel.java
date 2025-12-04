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

    // === НОВЫЕ ФУНКЦИИ ===

    // Бонусная еда
    private Point bonusFood = null;
    private int bonusFoodTimer = 0;
    private static final int BONUS_FOOD_DURATION = 50; // тиков
    private static final int BONUS_FOOD_CHANCE = 10; // шанс появления (1 из 10)
    private static final int BONUS_FOOD_POINTS = 5;

    // Режим без стен
    private boolean noWallsMode = false;

    // Анимация поедания
    private int eatAnimationTimer = 0;
    private Point lastEatPosition = null;

    // Эффекты
    private boolean soundEnabled = true;

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
        bonusFood = null;
        bonusFoodTimer = 0;
        score = 0;
        dirX = 1;
        dirY = 0;
        nextDirX = 1;
        nextDirY = 0;
        eatAnimationTimer = 0;
        lastEatPosition = null;
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

    private void playSound() {
        if (soundEnabled) {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    private void checkGameOver() {
        Point head = snake.get(0);

        // Проверка столкновения со стенами (только если режим без стен выключен)
        if (!noWallsMode) {
            if (head.x < 0 || head.x >= WIDTH / TILE_SIZE ||
                    head.y < 0 || head.y >= HEIGHT / TILE_SIZE) {
                state = State.GAME_OVER;
                playSound();
            }
        }

        // Проверка столкновения с собой
        for (int i = 1; i < snake.size(); i++) {
            if (head.equals(snake.get(i))) {
                state = State.GAME_OVER;
                playSound();
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
            if (!snake.contains(p) && (bonusFood == null || !p.equals(bonusFood))) {
                break;
            }
        }
        food = p;
    }

    private void spawnBonusFood() {
        if (bonusFood != null) return;

        int cellsX = WIDTH / TILE_SIZE;
        int cellsY = HEIGHT / TILE_SIZE;
        Point p;
        int attempts = 0;
        while (attempts < 100) {
            p = new Point(random.nextInt(cellsX), random.nextInt(cellsY));
            if (!snake.contains(p) && !p.equals(food)) {
                bonusFood = p;
                bonusFoodTimer = BONUS_FOOD_DURATION;
                return;
            }
            attempts++;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == State.RUNNING) {
            // меняем направление только один раз за тик
            dirX = nextDirX;
            dirY = nextDirY;

            Point head = snake.get(0);
            Point newHead = new Point(head.x + dirX, head.y + dirY);

            // Режим без стен - телепортация на другую сторону
            if (noWallsMode) {
                int cellsX = WIDTH / TILE_SIZE;
                int cellsY = HEIGHT / TILE_SIZE;
                if (newHead.x < 0) newHead.x = cellsX - 1;
                if (newHead.x >= cellsX) newHead.x = 0;
                if (newHead.y < 0) newHead.y = cellsY - 1;
                if (newHead.y >= cellsY) newHead.y = 0;
            }

            snake.add(0, newHead);

            boolean ate = false;

            // Проверка обычной еды
            if (newHead.equals(food)) {
                score++;
                ate = true;
                lastEatPosition = new Point(food);
                eatAnimationTimer = 5;
                playSound();
                spawnFood();

                // Шанс появления бонусной еды
                if (random.nextInt(BONUS_FOOD_CHANCE) == 0) {
                    spawnBonusFood();
                }

                // лёгкое ускорение: каждые 5 очков уменьшаем задержку на 10 мс от базовой скорости
                int newDelay = baseSpeed - (score / 5) * 10;
                if (newDelay < MIN_GAME_SPEED) {
                    newDelay = MIN_GAME_SPEED;
                }
                if (newDelay != currentDelay) {
                    currentDelay = newDelay;
                    timer.setDelay(currentDelay);
                }
            }

            // Проверка бонусной еды
            if (bonusFood != null && newHead.equals(bonusFood)) {
                score += BONUS_FOOD_POINTS;
                ate = true;
                lastEatPosition = new Point(bonusFood);
                eatAnimationTimer = 8;
                playSound();
                bonusFood = null;
                bonusFoodTimer = 0;
            }

            if (!ate) {
                snake.remove(snake.size() - 1);
            }

            // Таймер бонусной еды
            if (bonusFoodTimer > 0) {
                bonusFoodTimer--;
                if (bonusFoodTimer == 0) {
                    bonusFood = null;
                }
            }

            // Анимация
            if (eatAnimationTimer > 0) {
                eatAnimationTimer--;
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
        // Градиентный фон
        Graphics2D g2d = (Graphics2D) g;
        GradientPaint gradient = new GradientPaint(0, 0, new Color(20, 20, 40),
                                                    WIDTH, HEIGHT, new Color(40, 40, 60));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.GREEN);
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.drawString("🐍 SNAKE", 120, 70);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Нажмите ПРОБЕЛ, чтобы начать", 80, 110);
        g.drawString("Управление: WASD или стрелки", 80, 130);

        // меню выбора скорости
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Скорость:", 40, 170);

        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(speedMode == 0 ? Color.YELLOW : Color.WHITE);
        g.drawString("1) Медленно", 60, 190);
        g.setColor(speedMode == 1 ? Color.YELLOW : Color.WHITE);
        g.drawString("2) Нормально", 60, 210);
        g.setColor(speedMode == 2 ? Color.YELLOW : Color.WHITE);
        g.drawString("3) Быстро", 60, 230);

        // Режим без стен
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.setColor(Color.WHITE);
        g.drawString("Режимы:", 40, 265);

        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(noWallsMode ? Color.CYAN : Color.GRAY);
        g.drawString("N) Без стен: " + (noWallsMode ? "ВКЛ" : "ВЫКЛ"), 60, 285);

        g.setColor(soundEnabled ? Color.CYAN : Color.GRAY);
        g.drawString("M) Звук: " + (soundEnabled ? "ВКЛ" : "ВЫКЛ"), 60, 305);

        // Рекорд
        g.setColor(Color.YELLOW);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString("🏆 Лучший счёт: " + highScore, 120, 350);

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.drawString("Стрелки ↑↓ или 1/2/3 - скорость | N - стены | M - звук", 20, HEIGHT - 10);
    }

    private void drawGame(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // фон
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // лёгкая сетка
        g.setColor(new Color(30, 30, 30));
        for (int x = 0; x < WIDTH; x += TILE_SIZE) {
            g.drawLine(x, 0, x, HEIGHT);
        }
        for (int y = 0; y < HEIGHT; y += TILE_SIZE) {
            g.drawLine(0, y, WIDTH, y);
        }

        // Обычная еда
        g.setColor(Color.RED);
        g.fillOval(food.x * TILE_SIZE + 2, food.y * TILE_SIZE + 2, TILE_SIZE - 4, TILE_SIZE - 4);

        // Бонусная еда (мигает)
        if (bonusFood != null) {
            if (bonusFoodTimer % 4 < 2) { // мигание
                g.setColor(Color.YELLOW);
            } else {
                g.setColor(Color.ORANGE);
            }
            g.fillOval(bonusFood.x * TILE_SIZE + 1, bonusFood.y * TILE_SIZE + 1, TILE_SIZE - 2, TILE_SIZE - 2);

            // Показать оставшееся время
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.drawString("+" + BONUS_FOOD_POINTS, bonusFood.x * TILE_SIZE, bonusFood.y * TILE_SIZE - 2);
        }

        // Анимация поедания
        if (eatAnimationTimer > 0 && lastEatPosition != null) {
            int size = eatAnimationTimer * 4;
            g.setColor(new Color(255, 255, 0, 100));
            g.fillOval(lastEatPosition.x * TILE_SIZE + TILE_SIZE/2 - size/2,
                       lastEatPosition.y * TILE_SIZE + TILE_SIZE/2 - size/2, size, size);
        }

        // Градиентная змея
        for (int i = 0; i < snake.size(); i++) {
            Point p = snake.get(i);

            // Градиент от головы к хвосту
            float ratio = (float) i / Math.max(snake.size(), 1);
            int green = (int) (200 - ratio * 100);
            int blue = (int) (ratio * 50);

            if (i == 0) {
                // Голова - ярче
                g2d.setColor(new Color(50, 255, 50));
                g2d.fillRoundRect(p.x * TILE_SIZE + 1, p.y * TILE_SIZE + 1,
                                  TILE_SIZE - 2, TILE_SIZE - 2, 6, 6);

                // Глаза
                g.setColor(Color.BLACK);
                int eyeSize = 4;
                if (dirX == 1) { // вправо
                    g.fillOval(p.x * TILE_SIZE + 12, p.y * TILE_SIZE + 4, eyeSize, eyeSize);
                    g.fillOval(p.x * TILE_SIZE + 12, p.y * TILE_SIZE + 12, eyeSize, eyeSize);
                } else if (dirX == -1) { // влево
                    g.fillOval(p.x * TILE_SIZE + 4, p.y * TILE_SIZE + 4, eyeSize, eyeSize);
                    g.fillOval(p.x * TILE_SIZE + 4, p.y * TILE_SIZE + 12, eyeSize, eyeSize);
                } else if (dirY == -1) { // вверх
                    g.fillOval(p.x * TILE_SIZE + 4, p.y * TILE_SIZE + 4, eyeSize, eyeSize);
                    g.fillOval(p.x * TILE_SIZE + 12, p.y * TILE_SIZE + 4, eyeSize, eyeSize);
                } else { // вниз
                    g.fillOval(p.x * TILE_SIZE + 4, p.y * TILE_SIZE + 12, eyeSize, eyeSize);
                    g.fillOval(p.x * TILE_SIZE + 12, p.y * TILE_SIZE + 12, eyeSize, eyeSize);
                }
            } else {
                // Тело с градиентом
                g2d.setColor(new Color(0, green, blue));
                g2d.fillRoundRect(p.x * TILE_SIZE + 2, p.y * TILE_SIZE + 2,
                                  TILE_SIZE - 4, TILE_SIZE - 4, 4, 4);
            }
        }

        // Индикатор режима без стен
        if (noWallsMode) {
            g.setColor(new Color(0, 255, 255, 100));
            g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);
            g.drawRect(1, 1, WIDTH - 3, HEIGHT - 3);
        }

        // текущий счёт и рекорд сверху
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 14));
        g.drawString("Счёт: " + score, 10, 20);
        g.drawString("Рекорд: " + highScore, WIDTH - 100, 20);

        if (state == State.PAUSED) {
            g.setColor(new Color(0, 0, 0, 150));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.drawString("⏸ ПАУЗА", WIDTH / 2 - 70, HEIGHT / 2 - 20);

            g.setFont(new Font("Arial", Font.PLAIN, 14));
            g.drawString("P или ESC - продолжить", WIDTH / 2 - 80, HEIGHT / 2 + 20);
        }
    }

    private void drawGameOver(Graphics g) {
        // показываем поле как во время игры
        drawGame(g);

        // затем поверх затемнение и текст
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        g.setColor(Color.RED);
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.drawString("💀 ИГРА ОКОНЧЕНА", 60, 140);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 18));
        g.drawString("Ваш счёт: " + score, 140, 190);

        if (score >= highScore && score > 0) {
            g.setColor(Color.YELLOW);
            g.drawString("🎉 НОВЫЙ РЕКОРД!", 120, 220);
        } else {
            g.setColor(Color.GRAY);
            g.drawString("Рекорд: " + highScore, 140, 220);
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("R - играть снова", 140, 270);
        g.drawString("ПРОБЕЛ - в меню", 140, 295);
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

            // Переключение режима без стен
            if (key == KeyEvent.VK_N) {
                noWallsMode = !noWallsMode;
            }

            // Переключение звука
            if (key == KeyEvent.VK_M) {
                soundEnabled = !soundEnabled;
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
    public void keyReleased(KeyEvent e) {}

    @Override
    public void keyTyped(KeyEvent e) {}
}

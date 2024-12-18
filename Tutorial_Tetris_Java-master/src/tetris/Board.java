package tetris;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Board extends JPanel implements KeyListener, MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 1L;
    private BufferedImage pause, refresh;

    //board dimensions (the playing area)
    private final int boardHeight = 20, boardWidth = 10;

    // block size
    public static final int blockSize = 30;

    // field
    private Color[][] board = new Color[boardHeight][boardWidth];

    // array with all the possible shapes
    private Shape[] shapes = new Shape[7];

    // currentShape
    private static Shape currentShape, nextShape;

    // game loop
    private Timer looper;
    private int FPS = 60;
    private int delay = 1000 / FPS;

    // mouse events variables
    private int mouseX, mouseY;
    private boolean leftClick = false;
    private Rectangle stopBounds, refreshBounds;
    private boolean gamePaused = false;
    private boolean gameOver = false;

    // Random Color
    private Color[] colors = {
        Color.decode("#ed1c24"), Color.decode("#ff7f27"), Color.decode("#fff200"),
        Color.decode("#22b14c"), Color.decode("#00a2e8"), Color.decode("#a349a4"),
        Color.decode("#3f48cc")
    };

    private Random random = new Random();

    // buttons press lapse
    private Timer buttonLapse = new Timer(300, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            buttonLapse.stop();
        }
    });

    // score
    private int score = 0;

    // player
    private String playerName = "";

    // Score Board
    private List<PlayerScore> scoreboard = new ArrayList<>();

    public Board() {

        pause = ImageLoader.loadImage("/pause.png");
        refresh = ImageLoader.loadImage("/refresh.png");

        mouseX = 0;
        mouseY = 0;

        stopBounds = new Rectangle(350, 500, pause.getWidth(), pause.getHeight() + pause.getHeight() / 2);
        refreshBounds = new Rectangle(350, 500 - refresh.getHeight() - 20, refresh.getWidth(),
                refresh.getHeight() + refresh.getHeight() / 2);

        // create game looper
        looper = new Timer(delay, new GameLooper());

        // create random shapes
        shapes[0] = new Shape(new int[][]{
            {1, 1, 1, 1} // I shape;
        }, this, colors[0]);

        shapes[1] = new Shape(new int[][]{
            {1, 1, 1},
            {0, 1, 0}, // T shape;
        }, this, colors[1]);

        shapes[2] = new Shape(new int[][]{
            {1, 1, 1},
            {1, 0, 0}, // L shape;
        }, this, colors[2]);

        shapes[3] = new Shape(new int[][]{
            {1, 1, 1},
            {0, 0, 1}, // J shape;
        }, this, colors[3]);

        shapes[4] = new Shape(new int[][]{
            {0, 1, 1},
            {1, 1, 0}, // S shape;
        }, this, colors[4]);

        shapes[5] = new Shape(new int[][]{
            {1, 1, 0},
            {0, 1, 1}, // Z shape;
        }, this, colors[5]);

        shapes[6] = new Shape(new int[][]{
            {1, 1},
            {1, 1}, // O shape;
        }, this, colors[6]);

    }

    private void update() {
        if (stopBounds.contains(mouseX, mouseY) && leftClick && !buttonLapse.isRunning() && !gameOver) {
            buttonLapse.start();
            gamePaused = !gamePaused;
        }

        if (refreshBounds.contains(mouseX, mouseY) && leftClick) {
            startGame();
        }

        if (gamePaused || gameOver) {
            if (gameOver && !looper.isRunning()) {
                // Jika game over, simpan skor dan hentikan permainan
                saveScoreToDatabase(playerName, score);
                stopGame(); // Reset permainan
            }
            return;
        }

        // Hitung baris yang dibersihkan
        int linesCleared = checkAndClearLines();

        // Perbarui bentuk saat ini dengan jumlah baris yang dibersihkan
        currentShape.update(linesCleared);
    }

    private int checkAndClearLines() {
        int linesCleared = 0;

        for (int row = 0; row < boardHeight; row++) {
            boolean isLineFull = true;

            for (int col = 0; col < boardWidth; col++) {
                if (board[row][col] == null) {
                    isLineFull = false;
                    break;
                }
            }

            if (isLineFull) {
                linesCleared++;
                clearLine(row);
            }
        }

        return linesCleared;
    }

    private void clearLine(int row) {
        for (int i = row; i > 0; i--) {
            for (int j = 0; j < boardWidth; j++) {
                board[i][j] = board[i - 1][j];
            }
        }

        for (int j = 0; j < boardWidth; j++) {
            board[0][j] = null;
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(new Color(19, 27, 42));
        g.fillRect(0, 0, getWidth(), getHeight());

        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {

                if (board[row][col] != null) {
                    g.setColor(board[row][col]);
                    g.fillRect(col * blockSize, row * blockSize, blockSize, blockSize);
                }

            }
        }

        g.setColor(nextShape.getColor());
        for (int row = 0; row < nextShape.getCoords().length; row++) {
            for (int col = 0; col < nextShape.getCoords()[0].length; col++) {
                if (nextShape.getCoords()[row][col] != 0) {
                    g.fillRect(col * 30 + 320, row * 30 + 50, Board.blockSize, Board.blockSize);
                }
            }
        }
        currentShape.render(g);

        if (stopBounds.contains(mouseX, mouseY)) {
            g.drawImage(pause.getScaledInstance(pause.getWidth() + 3, pause.getHeight() + 3, BufferedImage.SCALE_DEFAULT), stopBounds.x + 3, stopBounds.y + 3, null);
        } else {
            g.drawImage(pause, stopBounds.x, stopBounds.y, null);
        }

        if (refreshBounds.contains(mouseX, mouseY)) {
            g.drawImage(refresh.getScaledInstance(refresh.getWidth() + 3, refresh.getHeight() + 3,
                    BufferedImage.SCALE_DEFAULT), refreshBounds.x + 3, refreshBounds.y + 3, null);
        } else {
            g.drawImage(refresh, refreshBounds.x, refreshBounds.y, null);
        }

        // Pause/Start Game
        if (gamePaused) {
            String gamePausedString = "GAME PAUSED";
            g.setColor(Color.WHITE);
            g.setFont(new Font("Georgia", Font.BOLD, 30));
            g.drawString(gamePausedString, 35, WindowGame.HEIGHT / 2);
        }

        //Game Over
        if (gameOver) {
            loadTopScoresFromDatabase(); // Load top scores from database before displaying
            displayScoreboard(g); // Display the scoreboard
            g.drawString(playerName + " " + " " + " " + score, 50, WindowGame.HEIGHT / 2 + 30);
            
//            g.setFont(new Font("Georgia", Font.BOLD, 20));

            String gameOverString = "GAME OVER";
            g.setColor(Color.WHITE);
            g.setFont(new Font("Georgia", Font.BOLD, 30));
            g.drawString(gameOverString, 50, WindowGame.HEIGHT / 2);

            // Display player's name and score
        }

        g.setColor(Color.WHITE);
        g.setFont(new Font("Georgia", Font.BOLD, 20));
        g.drawString("SCORE", WindowGame.WIDTH - 125, WindowGame.HEIGHT / 2);
        g.drawString(score + "", WindowGame.WIDTH - 125, WindowGame.HEIGHT / 2 + 30);

        g.setColor(new Color(148, 166, 199));
        for (int i = 0; i <= boardHeight; i++) {
            g.drawLine(0, i * blockSize, boardWidth * blockSize, i * blockSize);
        }
        for (int j = 0; j <= boardWidth; j++) {
            g.drawLine(j * blockSize, 0, j * blockSize, boardHeight * 30);
        }
    }

    // Loping shape baru
    public void setNextShape() {
        int index = random.nextInt(shapes.length);
        int colorIndex = random.nextInt(colors.length);
        nextShape = new Shape(shapes[index].getCoords(), this, colors[colorIndex]);
    }

    // Jika Shape sudah berada paling atas
    public void setCurrentShape() {
        currentShape = nextShape;
        setNextShape();

        // Tambahkan 1 poin setiap shape baru muncul
        score += 1;

        for (int row = 0; row < currentShape.getCoords().length; row++) {
            for (int col = 0; col < currentShape.getCoords()[0].length; col++) {
                if (currentShape.getCoords()[row][col] != 0) {
                    if (board[currentShape.getY() + row][currentShape.getX() + col] != null) {
                        gameOver = true;
                    }
                }
            }
        }
    }

    public Color[][] getBoard() {
        return board;
    }

    //memunculkan scoreboard
    private void addScoreToBoard(String playerName, int score) {
        PlayerScore newScore = new PlayerScore(playerName, score);
        scoreboard.add(newScore);
        Collections.sort(scoreboard);
        if (scoreboard.size() > 5) {
            scoreboard.remove(scoreboard.size() - 1); // Keep only top 5 scores
        }
    }

//    private void displayScoreboard(Graphics g) {
//        g.setColor(Color.WHITE);
//        g.setFont(new Font("Georgia", Font.BOLD, 20));
//        g.drawString("LEADERBOARD", 50, 50);
//
//        // Display top 5 scores
//        int displayCount = Math.min(5, scoreboard.size());
//        for (int i = 0; i < displayCount; i++) {
//            PlayerScore ps = scoreboard.get(i);
//            g.drawString((i + 1) + ". " + ps.getPlayerName() + " " + ps.getScore(), 50, 80 + (i * 30));
//        }
//
//        // Check if the player's score qualifies for the leaderboard
//        int playerRank = -1;
//        for (int i = 0; i < scoreboard.size(); i++) {
//            if (scoreboard.get(i).getPlayerName().equals(playerName)) {
//                playerRank = i + 1; // Rank is index + 1
//                break;
//            }
//        }
//
//        // If the player's score is in the top 5, display it twice
//        if (playerRank != -1 && playerRank <= 5) {
//            g.drawString(playerRank + ". " + playerName + " " + score, 50, 100 + (displayCount * 30));
//        } else if (playerRank > 5) {
//            // If the player's score is not in the top 5, display it once
//            g.drawString(playerRank + ". " + playerName + " " + score, 50, 100);
//        }
//    }
    
    private void displayScoreboard(Graphics g) {
        // Sort the scoreboard in descending order by score
        scoreboard.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));

        // Set leaderboard title
        g.setColor(Color.WHITE);
        g.setFont(new Font("Georgia", Font.BOLD, 20));
        g.drawString("LEADERBOARD", 50, 50);

        // Display top 5 scores
        int displayCount = Math.min(5, scoreboard.size());
        for (int i = 0; i < displayCount; i++) {
            PlayerScore ps = scoreboard.get(i);
            g.drawString((i + 1) + ". " + ps.getPlayerName() + " " + ps.getScore(), 50, 80 + (i * 30));
        }

        // Check if the player's score qualifies for the leaderboard
        int playerRank = -1;
        for (int i = 0; i < scoreboard.size(); i++) {
            if (scoreboard.get(i).getPlayerName().equals(playerName)) {
                playerRank = i + 1; // Rank is index + 1
                break;
            }
        }
//
//        // If the player's score is in the top 5, display it twice
//        if (playerRank != -1 && playerRank <= 5) {
//            g.drawString(playerRank + ". " + playerName + " " + score, 50, 100 + (displayCount * 30));
//        } else if (playerRank > 5) {
//            // If the player's score is not in the top 5, display it once
//            g.drawString(playerRank + ". " + playerName + " " + score, 50, 100 + (displayCount * 30));
//        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_UP) {
            currentShape.rotateShape();
        }
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
            currentShape.setDeltaX(1);
        }
        if (e.getKeyCode() == KeyEvent.VK_LEFT) {
            currentShape.setDeltaX(-1);
        }
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            currentShape.speedUp();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN) {
            currentShape.speedDown();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    // Memulai game yang di pause
    public void startGame() {
        // Prompt for player name before starting the game
        stopGame(); // Reset the game state

        loadTopScoresFromDatabase();

        playerName = JOptionPane.showInputDialog("Enter your name:");
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Unknown"; // Default jika tidak diisi
        }

        setNextShape();
        setCurrentShape();
        gameOver = false;
        score = 0; // Reset score for new game
        looper.start();

    }

    // Menghentikan permainan
    public void stopGame() {
        if (gameOver) {
            // Save score to the database immediately when the game is over
            saveScoreToDatabase(playerName, score);
        }

        score = 0;
        for (int row = 0; row < board.length; row++) {
            for (int col = 0; col < board[row].length; col++) {
                board[row][col] = null;
            }
        }
        looper.stop();
    }

    class GameLooper implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            update();
            repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClick = true;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
            leftClick = false;
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    public void addScore(int linesCleared) {
        score += 5 * linesCleared; // Tambahkan skor 5 poin per baris
    }

    public int getScore() {
        return score;
    }

    private void saveScoreToDatabase(String playerName, int score) {
        String url = "jdbc:mysql://localhost:3306/tetris"; // URL JDBC yang benar
        String username = "root"; // Nama pengguna MySQL
        String password = "";     // Kata sandi MySQL (biarkan kosong jika default XAMPP)

        String insertQuery = "INSERT INTO score_board (player_name, score) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(url, username, password); PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {

            pstmt.setString(1, playerName);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();

            System.out.println("Score saved to database successfully!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save score to database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        System.out.println("Saving score for player: " + playerName + " with score: " + score);

    }

    private void loadTopScoresFromDatabase() {
        String url = "jdbc:mysql://localhost:3306/tetris"; // Adjust database name
        String username = "root"; // MySQL username
        String password = "";     // MySQL password

        String query = "SELECT player_name, score FROM score_board ORDER BY score DESC LIMIT 5"; // Get top 5 scores

        scoreboard.clear(); // Clear previous scores

        try (Connection conn = DriverManager.getConnection(url, username, password); PreparedStatement pstmt = conn.prepareStatement(query)) {

            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                String playerName = resultSet.getString("player_name");
                int score = resultSet.getInt("score");

                scoreboard.add(new PlayerScore(playerName, score)); // Add score to list
            }
            
            addScoreToBoard(playerName, score);
            Collections.sort(scoreboard, Collections.reverseOrder());
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load top scores from database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}

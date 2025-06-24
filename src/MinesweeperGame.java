
import java.util.ArrayList;
import java.util.Collections;
// import java.util.HashSet;
// import java.util.LinkedList;
import java.util.List;
// import java.util.Queue;
// import java.util.Set;

    public class MinesweeperGame {

        public enum GameState {
            PLAYING,
            WON,
            LOST
        }

        public enum MoveType {
            DIG,
            FLAG
        }

        public record Move(MoveType type, int x, int y) {}

        private final int width;
        private final int height;
        private final int numBombs;
        private Cell[][] board;
        private GameState gameState;
        private int revealedCells;

        public MinesweeperGame(int width, int height, int numBombs, int startX, int startY, boolean verbose) {
        this.width = width;
        this.height = height;
        if (numBombs >= (width * height) - 9) {
            throw new IllegalArgumentException("Jumlah bom terlalu banyak untuk ukuran papan dan jaminan awal.");
        }
        this.numBombs = numBombs;
        
        // Inisialisasi papan
        this.board = new Cell[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                board[y][x] = new Cell();
            }
        }

        // Tempatkan bom dengan zona aman di sekitar klik pertama
        placeBombsWithSafeZone(startX, startY);
        
        // Hitung semua angka di sel yang tidak ada bom
        calculateAllAdjacentBombs();

        // Set status awal permainan
        this.gameState = GameState.PLAYING;
        this.revealedCells = 0;

        // Lakukan langkah pertama untuk membuka area awal
        handleMove(new Move(MoveType.DIG, startX, startY));
        
        if (verbose) {
            System.out.println("Konfigurasi papan telah dibuat.");
        }
    }
  
    private void placeBombsWithSafeZone(int startX, int startY) {
        List<Integer> possiblePositions = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                possiblePositions.add(y * width + x);
            }
        }

        for (int y = startY - 1; y <= startY + 1; y++) {
            for (int x = startX - 1; x <= startX + 1; x++) {
                if (isValid(x, y)) {
                    possiblePositions.remove(Integer.valueOf(y * width + x));
                }
            }
        }

        Collections.shuffle(possiblePositions);

        for (int i = 0; i < numBombs; i++) {
            int pos = possiblePositions.get(i);
            int x = pos % width;
            int y = pos / width;
            board[y][x].isBomb = true;
        }
    }

    private void calculateAllAdjacentBombs() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (board[y][x].isBomb) {
                    continue;
                }
                int count = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dx;
                        int ny = y + dy;
                        if (isValid(nx, ny) && board[ny][nx].isBomb) {
                            count++;
                        }
                    }
                }
                board[y][x].adjacentBombs = count;
            }
        }
    }

    public GameState handleMove(Move move) {
        if (gameState != GameState.PLAYING || !isValid(move.x, move.y)) {
            return gameState;
        }
        Cell cell = board[move.y][move.x];

        // Logika untuk FLAG tidak berubah
        if (move.type == MoveType.FLAG) {
            if (!cell.isRevealed) {
                cell.isFlagged = !cell.isFlagged;
            }
            return gameState; // Aksi flag tidak memicu akhir permainan
        }

        // Logika untuk DIG diperluas
        if (move.type == MoveType.DIG) {
            // Kasus 1: Aksi DIG pada petak yang sudah terbuka (Chording)
            if (cell.isRevealed && cell.adjacentBombs > 0) {
                System.out.println("Info: Aksi DIG pada petak terbuka [" + move.y + "," + move.x + "]. Memeriksa kondisi 'Buka Sekitar'...");

                // Hitung bendera di sekitar
                int adjacentFlagCount = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = move.x + dx;
                        int ny = move.y + dy;
                        if (isValid(nx, ny) && board[ny][nx].isFlagged) {
                            adjacentFlagCount++;
                        }
                    }
                }

                // Jika jumlah bendera sesuai, buka petak sekitar
                if (adjacentFlagCount == cell.adjacentBombs) {
                    System.out.println("Aksi Buka Sekitar: Kondisi terpenuhi. Membuka petak sekitar...");
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            if (dx == 0 && dy == 0) continue;
                            // Jika game sudah kalah (karena membuka bom), hentikan proses
                            if (gameState == GameState.LOST) break;
                            dig(move.x + dx, move.y + dy);
                        }
                        if (gameState == GameState.LOST) break;
                    }
                } else {
                    // Jika jumlah bendera tidak sesuai
                    System.out.println("Aksi Buka Sekitar Gagal: Jumlah bendera (" + adjacentFlagCount + ") tidak sesuai dengan angka petak (" + cell.adjacentBombs + ").");
                }
            
            // Kasus 2: Aksi DIG normal pada petak yang masih tertutup
            } else if (!cell.isRevealed && !cell.isFlagged) {
                dig(move.x, move.y);
            }
            // Jika DIG pada petak kosong yang sudah terbuka atau petak berbendera, tidak melakukan apa-apa
        }

        checkWinCondition();
        return gameState;
    }

    private void dig(int x, int y) {
        
        if (!isValid(x, y)) {
            return;
        }

        Cell cell = board[y][x];

        
        if (cell.isRevealed || cell.isFlagged) {
            return;
        }

        cell.isRevealed = true;
        revealedCells++;

        if (cell.isBomb) {
            gameState = GameState.LOST;
            return;
        }
        
        if (cell.adjacentBombs == 0) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    
                    dig(x + dx, y + dy);
                }
            }
        }
    }
    
    private void checkWinCondition() {
        if (revealedCells == (width * height) - numBombs) {
            gameState = GameState.WON;
        }
    }

    public boolean isValid(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public GameState getGameState() {
        return gameState;
    }

    public char[][] getDisplayBoard() {
        char[][] display = new char[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Cell cell = board[y][x];
                if (cell.isFlagged) {
                    display[y][x] = 'F';
                } else if (!cell.isRevealed) {
                    display[y][x] = 'C';
                } else if (cell.isBomb) {
                    display[y][x] = 'B'; 
                } else if (cell.adjacentBombs == 0) {
                    display[y][x] = '_';
                } else {
                    display[y][x] = (char) (cell.adjacentBombs + '0');
                }
            }
        }
        return display;
    }

    public void printBoard(int step) {
        System.out.println("\n--- Langkah ke-" + step + " ---");
        char[][] display = getDisplayBoard();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                System.out.print(display[y][x] + " ");
            }
            System.out.println();
        }
    }
}
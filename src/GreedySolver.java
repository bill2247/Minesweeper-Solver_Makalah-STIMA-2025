// File: GreedySolver.java
// NOTE: Ini adalah file baru yang disesuaikan dari kode Anda.
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

// PERUBAHAN: Menambahkan 'implements ISolver'
public class GreedySolver implements ISolver {

    // PERUBAHAN: Bagian placeholder untuk MinesweeperGame dihapus karena kelas aslinya sudah ada.

    private record Coordinate(int row, int col) {}

    private int rows;
    private int cols;
    private char[][] board;

    // PERUBAHAN: Menambahkan anotasi @Override untuk kejelasan
    @Override
    public MinesweeperGame.Move findNextMove(char[][] currentBoard, int totalBombs) {
        this.board = currentBoard;
        this.rows = board.length;
        this.cols = board[0].length;

        // 1. Mencari langkah deterministik
        List<MinesweeperGame.Move> certainMoves = findDeterministicMoves();
        if (!certainMoves.isEmpty()) {
            MinesweeperGame.Move move = certainMoves.get(0);
            System.out.println("GreedySolver: Langkah Pasti Ditemukan -> " + move.type() + " di [" + move.y() + ", " + move.x() + "]");
            return move;
        }

        // 2. Jika tidak ada langkah pasti, lakukan tebakan acak
        System.out.println("GreedySolver: Tidak ada langkah pasti. Melakukan tebakan acak...");
        return findRandomMove();
    }


    private List<MinesweeperGame.Move> findDeterministicMoves() {
        List<MinesweeperGame.Move> chordingMoves = new ArrayList<>();
        List<MinesweeperGame.Move> flaggingMoves = new ArrayList<>();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (Character.isDigit(board[r][c])) {
                    int numValue = Character.getNumericValue(board[r][c]);
                    List<Coordinate> neighbors = getNeighbors(r, c);
                    
                    long flagCount = neighbors.stream()
                        .filter(n -> board[n.row()][n.col()] == 'F').count();
                    List<Coordinate> coveredNeighbors = neighbors.stream()
                        .filter(n -> board[n.row()][n.col()] == 'C')
                        .collect(Collectors.toList());

                    // PELUANG CHORDING (Aturan Aman Dasar yang Ditingkatkan)
                    if (numValue == flagCount && !coveredNeighbors.isEmpty()) {
                        Coordinate chordCoord = new Coordinate(r, c);
                        // Tambahkan aksi DIG pada sel angka itu sendiri
                        chordingMoves.add(new MinesweeperGame.Move(MinesweeperGame.MoveType.DIG, chordCoord.col, chordCoord.row));
                    }

                    // ATURAN BOM DASAR (tidak berubah)
                    if (numValue == flagCount + coveredNeighbors.size() && !coveredNeighbors.isEmpty()) {
                        for (Coordinate n : coveredNeighbors) {
                            flaggingMoves.add(new MinesweeperGame.Move(MinesweeperGame.MoveType.FLAG, n.col, n.row));
                        }
                    }
                }
            }
        }

        // Prioritaskan aksi chording
        if (!chordingMoves.isEmpty()) {
            System.out.println("GreedySolver: Peluang 'Buka Sekitar' (Chording) terdeteksi.");
            return chordingMoves;
        }

        // Jika tidak ada peluang chording, lakukan aksi flagging
        if (!flaggingMoves.isEmpty()) {
            System.out.println("GreedySolver: Langkah penandaan (FLAG) terdeteksi.");
            return flaggingMoves;
        }
        
        // Jika tidak ada aksi deterministik sama sekali
        return Collections.emptyList();
    }

    private MinesweeperGame.Move findRandomMove() {
        List<Coordinate> coveredCells = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (board[r][c] == 'C') {
                    coveredCells.add(new Coordinate(r, c));
                }
            }
        }

        if (!coveredCells.isEmpty()) {
            Coordinate randomCell = coveredCells.get(new Random().nextInt(coveredCells.size()));
            System.out.println("GreedySolver: Memilih sel acak di [" + randomCell.row + ", " + randomCell.col + "]");
            return new MinesweeperGame.Move(MinesweeperGame.MoveType.DIG, randomCell.col, randomCell.row);
        }

        return null;
    }

    private List<Coordinate> getNeighbors(int r, int c) {
        List<Coordinate> neighbors = new ArrayList<>();
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr;
                int nc = c + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    neighbors.add(new Coordinate(nr, nc));
                }
            }
        }
        return neighbors;
    }
}
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class GreedySolver implements ISolver {

    private record Coordinate(int row, int col) {}

    private int rows;
    private int cols;
    private char[][] board;

    @Override
    public MinesweeperGame.Move findNextMove(char[][] currentBoard, int totalBombs) {
        this.board = currentBoard;
        this.rows = board.length;
        this.cols = board[0].length;

        List<MinesweeperGame.Move> certainMoves = findDeterministicMoves();
        if (!certainMoves.isEmpty()) {
            MinesweeperGame.Move move = certainMoves.get(0);
            System.out.println("GreedySolver: Langkah Pasti Ditemukan -> " + move.type() + " di [" + move.y() + ", " + move.x() + "]");
            return move;
        }

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

                    if (numValue == flagCount && !coveredNeighbors.isEmpty()) {
                        Coordinate chordCoord = new Coordinate(r, c);
                        
                        chordingMoves.add(new MinesweeperGame.Move(MinesweeperGame.MoveType.DIG, chordCoord.col, chordCoord.row));
                    }

                    if (numValue == flagCount + coveredNeighbors.size() && !coveredNeighbors.isEmpty()) {
                        for (Coordinate n : coveredNeighbors) {
                            flaggingMoves.add(new MinesweeperGame.Move(MinesweeperGame.MoveType.FLAG, n.col, n.row));
                        }
                    }
                }
            }
        }

        if (!chordingMoves.isEmpty()) {
            System.out.println("GreedySolver: Peluang 'Buka Sekitar' (Chording) terdeteksi.");
            return chordingMoves;
        }

        if (!flaggingMoves.isEmpty()) {
            System.out.println("GreedySolver: Langkah penandaan (FLAG) terdeteksi.");
            return flaggingMoves;
        }
        
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